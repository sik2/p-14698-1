package com.back;

import com.back.product.entity.Product;
import com.back.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BackApplicationTests {
    @Autowired
    ProductRepository productRepository;

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
}
