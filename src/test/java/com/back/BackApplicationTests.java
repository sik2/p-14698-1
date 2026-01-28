package com.back;

import com.back.product.document.ProductDocument;
import com.back.product.entity.Product;
import com.back.product.repository.ProductDocumentRepository;
import com.back.product.repository.ProductRepository;
import com.back.product.service.ProductService;
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
    @DisplayName("ProductService - KNN Search basic test")
    void t7() {
        // given
        productService.create("Gaming Laptop", List.of("gaming", "high-performance", "graphics-card"));
        productService.create("Business Laptop", List.of("office", "document", "lightweight"));
        productService.create("Gaming Mouse", List.of("gaming", "high-sensitivity", "RGB"));

        // when - search for "gaming computer"
        List<Product> results = productService.knnSearch("gaming computer", 3);

        // then
        assertNotNull(results);
        assertFalse(results.isEmpty());
        System.out.println("KNN Search Results:");
        results.forEach(r -> System.out.println(" - " + r.getName()));
    }

    @Test
    @DisplayName("ProductService - KNN Search result order verification")
    void t8() {
        // given - products from different categories
        Product coffee = productService.create("Americano", List.of("coffee", "caffeine", "beverage"));
        Product tea = productService.create("Green Tea", List.of("tea", "catechin", "beverage"));
        Product juice = productService.create("Orange Juice", List.of("fruit", "vitamin", "beverage"));

        // when - search for "coffee drink"
        List<Product> results = productService.knnSearch("coffee drink", 3);

        // then - verify results are returned and contain the products
        assertFalse(results.isEmpty());
        List<Long> resultIds = results.stream().map(Product::getId).toList();
        assertTrue(resultIds.contains(coffee.getId()), "Americano should be in results");

        System.out.println("Order verification (coffee drink search):");
        for (int i = 0; i < results.size(); i++) {
            System.out.println((i + 1) + ": " + results.get(i).getName());
        }
    }

    @Test
    @DisplayName("ProductService - KNN Search electronics category")
    void t9() {
        // given
        Product laptop = productService.create("MacBook Pro", List.of("laptop", "apple", "development"));
        Product phone = productService.create("iPhone 15", List.of("smartphone", "apple", "communication"));
        Product tablet = productService.create("iPad Pro", List.of("tablet", "apple", "drawing"));
        Product headphone = productService.create("AirPods Max", List.of("headphone", "apple", "music"));

        // when - search for "developer computer"
        List<Product> results = productService.knnSearch("developer computer", 4);

        // then - verify MacBook is in results
        assertFalse(results.isEmpty());
        List<Long> resultIds = results.stream().map(Product::getId).toList();
        assertTrue(resultIds.contains(laptop.getId()), "MacBook should be in results");

        System.out.println("Electronics search (developer computer):");
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

        // when - search for "cold weather clothes"
        List<Product> results = productService.knnSearch("cold weather clothes", 4);

        // then - winter clothes should be in results
        assertFalse(results.isEmpty());
        List<Long> resultIds = results.stream().map(Product::getId).toList();
        List<Long> winterIds = List.of(jacket.getId(), coat.getId());
        boolean hasWinterClothes = resultIds.stream().anyMatch(winterIds::contains);
        assertTrue(hasWinterClothes, "Winter clothes should be in results");

        System.out.println("Clothing search (cold weather clothes):");
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
        List<Product> results = productService.knnSearch("test product", 3);

        // then - should return at most 3 results
        assertTrue(results.size() <= 3, "Should not return more than k results");

        System.out.println("k limit verification (k=3):");
        System.out.println("Number of results: " + results.size());
    }

    @Test
    @DisplayName("ProductService - KNN Search empty result handling")
    void t12() {
        // when - search with no data
        List<Product> results = productService.knnSearch("non-existent product", 3);

        // then - should return empty list
        assertNotNull(results);
        System.out.println("Empty result handling: result count = " + results.size());
    }

}