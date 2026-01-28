package com.back;

import com.back.product.document.ProductDocument;
import com.back.product.entity.Product;
import com.back.product.repository.ProductDocumentRepository;
import com.back.product.repository.ProductRepository;
import com.back.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
//@ActiveProfiles("test")
class BackApplicationTests {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductDocumentRepository productDocumentRepository;

    @Autowired
    private ProductService productService;

    @BeforeEach
    void setUp() {
        // 이전 테스트 데이터 정리
        productDocumentRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("JPA 테스트")
    void t1(){
        assertDoesNotThrow(()->{
            Product product = new Product();
            product.setName("테스트 상품");

            product.addKeyword("키워드1");
            product.addKeyword("키워드2");

            productRepository.save(product);
        });
    }

    @Test
    @DisplayName("Elasticsearch 테스트")
    void t2(){
        assertDoesNotThrow(()->{
            ProductDocument doc = new ProductDocument();
            doc.setEmbedding(new float[384]);
            doc.setId(1L);
            productDocumentRepository.findAll();
        });
    }

    @Test
    @DisplayName("ProductService - Create 테스트")
    void t3() {
        // given
        String name = "노트북";
        List<String> keywords = List.of("전자기기", "컴퓨터", "휴대용");

        // when
        Product product = productService.create(name, keywords);

        // then
        assertNotNull(product.getId());
        assertEquals(name, product.getName());
        assertEquals(3, product.getKeywords().size());

        // Elasticsearch에도 저장되었는지 확인
        Optional<ProductDocument> doc = productService.findDocumentById(product.getId());
        assertTrue(doc.isPresent());
        assertEquals(product.getId(), doc.get().getId());
    }

    @Test
    @DisplayName("ProductService - Read 테스트")
    void t4() {
        // given
        Product created = productService.create("스마트폰", List.of("전자기기", "통신"));

        // when
        Optional<Product> found = productService.findById(created.getId());
        List<Product> all = productService.findAll();

        // then
        assertTrue(found.isPresent());
        assertEquals("스마트폰", found.get().getName());
        assertFalse(all.isEmpty());
    }

    @Test
    @DisplayName("ProductService - Update 테스트")
    void t5() {
        // given
        Product created = productService.create("태블릿", List.of("전자기기"));

        // when
        Product updated = productService.update(created.getId(), "아이패드", List.of("애플", "태블릿", "전자기기"));

        // then
        assertEquals("아이패드", updated.getName());
        assertEquals(3, updated.getKeywords().size());

        // Elasticsearch 문서도 업데이트 확인
        Optional<ProductDocument> doc = productService.findDocumentById(updated.getId());
        assertTrue(doc.isPresent());
    }

    @Test
    @DisplayName("ProductService - Delete 테스트")
    void t6() {
        // given
        Product created = productService.create("삭제용상품", List.of("테스트"));
        Long id = created.getId();

        // when
        productService.delete(id);

        // then
        Optional<Product> found = productService.findById(id);
        assertFalse(found.isPresent());

        Optional<ProductDocument> doc = productService.findDocumentById(id);
        assertFalse(doc.isPresent());
    }

    @Test
    @DisplayName("ProductService - KNN Search basic test with keywords")
    void t7() {
        // given
        productService.create("Gaming Laptop", List.of("gaming", "high-performance", "graphics-card"));
        productService.create("Business Laptop", List.of("office", "document", "lightweight"));
        productService.create("Gaming Mouse", List.of("gaming", "high-sensitivity", "RGB"));

        // when - search using keywords list (simulating user viewing a gaming product)
        List<Product> results = productService.knnSearch(List.of("gaming", "computer"), 3);

        // then
        assertNotNull(results);
        assertFalse(results.isEmpty());
        System.out.println("KNN Search Results (keywords: gaming, computer):");
        results.forEach(r -> System.out.println(" - " + r.getName()));
    }

    @Test
    @DisplayName("ProductService - KNN Search with product keywords")
    void t8() {
        // given - products from different categories
        Product coffee = productService.create("Americano", List.of("coffee", "caffeine", "beverage"));
        Product tea = productService.create("Green Tea", List.of("tea", "catechin", "beverage"));
        Product juice = productService.create("Orange Juice", List.of("fruit", "vitamin", "beverage"));

        // when - search using coffee product's keywords
        List<String> coffeeKeywords = List.of("coffee", "caffeine", "beverage");
        List<Product> results = productService.knnSearch(coffeeKeywords, 3);

        // then - verify results are returned and coffee is in results
        assertFalse(results.isEmpty());
        List<Long> resultIds = results.stream().map(Product::getId).toList();
        assertTrue(resultIds.contains(coffee.getId()), "Americano should be in results");

        System.out.println("Search with coffee keywords:");
        for (int i = 0; i < results.size(); i++) {
            System.out.println((i + 1) + ": " + results.get(i).getName());
        }
    }

    @Test
    @DisplayName("ProductService - findSimilarProducts test")
    void t9() {
        // given
        Product laptop = productService.create("MacBook Pro", List.of("laptop", "apple", "development"));
        Product phone = productService.create("iPhone 15", List.of("smartphone", "apple", "communication"));
        Product tablet = productService.create("iPad Pro", List.of("tablet", "apple", "drawing"));
        Product headphone = productService.create("AirPods Max", List.of("headphone", "apple", "music"));

        // when - find similar products to MacBook Pro
        List<Product> results = productService.findSimilarProducts(laptop.getId(), 3);

        // then - MacBook should not be in results (exclude itself), other apple products should be
        assertFalse(results.isEmpty());
        List<Long> resultIds = results.stream().map(Product::getId).toList();
        assertFalse(resultIds.contains(laptop.getId()), "MacBook should NOT be in results (self excluded)");

        System.out.println("Similar products to MacBook Pro:");
        for (int i = 0; i < results.size(); i++) {
            System.out.println((i + 1) + ": " + results.get(i).getName());
        }
    }

    @Test
    @DisplayName("ProductService - KNN Search clothing category")
    void t10() {
        // given
        Product jacket = productService.create("Winter Puffer Jacket", List.of("outerwear", "winter", "warm"));
        Product tshirt = productService.create("Cotton T-Shirt", List.of("top", "summer", "casual"));
        Product jeans = productService.create("Blue Jeans", List.of("bottom", "denim", "casual"));
        Product coat = productService.create("Wool Coat", List.of("outerwear", "winter", "formal"));

        // when - search using winter clothing keywords
        List<Product> results = productService.knnSearch(List.of("outerwear", "winter", "warm"), 4);

        // then - winter clothes should be in results
        assertFalse(results.isEmpty());
        List<Long> resultIds = results.stream().map(Product::getId).toList();
        List<Long> winterIds = List.of(jacket.getId(), coat.getId());
        boolean hasWinterClothes = resultIds.stream().anyMatch(winterIds::contains);
        assertTrue(hasWinterClothes, "Winter clothes should be in results");

        System.out.println("Clothing search (winter keywords):");
        for (int i = 0; i < results.size(); i++) {
            System.out.println((i + 1) + ": " + results.get(i).getName());
        }
    }

    @Test
    @DisplayName("ProductService - KNN Search k limit verification")
    void t11() {
        // given - create 5 products
        productService.create("Product 1", List.of("test", "first"));
        productService.create("Product 2", List.of("test", "second"));
        productService.create("Product 3", List.of("test", "third"));
        productService.create("Product 4", List.of("test", "fourth"));
        productService.create("Product 5", List.of("test", "fifth"));

        // when - search with k=3
        List<Product> results = productService.knnSearch(List.of("test", "product"), 3);

        // then - should return at most 3 results
        assertTrue(results.size() <= 3, "Should not return more than k results");

        System.out.println("k limit verification (k=3):");
        System.out.println("Number of results: " + results.size());
    }

    @Test
    @DisplayName("ProductService - KNN Search empty keywords handling")
    void t12() {
        // when - search with empty keywords
        List<Product> results = productService.knnSearch(List.of(), 3);

        // then - should return empty list
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Empty keywords should return empty list");
        System.out.println("Empty keywords handling: result count = " + results.size());
    }

}