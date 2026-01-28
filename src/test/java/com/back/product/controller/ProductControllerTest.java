package com.back.product.controller;


import com.back.TestcontainersConfiguration;
import com.back.product.entity.Product;
import com.back.product.repository.ProductDocumentRepository;
import com.back.product.repository.ProductRepository;
import com.back.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
//@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;


    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDocumentRepository productDocumentRepository;

    @BeforeEach
    void setUp() {
        productDocumentRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/products - 상품 생성")
    void t1() throws Exception {
        var request = new ProductController.CreateRequest("Gaming Laptop", List.of("laptop", "gaming", "high-performance"));

        ResultActions result = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.keywords", hasSize(3)));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - ID로 상품 조회")
    void t2() throws Exception {
        Product product = productService.create("MacBook Pro", List.of("laptop", "apple", "development"));

        ResultActions result = mockMvc.perform(get("/api/v1/products/{id}", product.getId()));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("MacBook Pro"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - 존재하지 않는 상품 조회")
    void t3() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/products/{id}", 99999L));

        result.andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/products - 전체 상품 조회")
    void t4() throws Exception {
        productService.create("Product A", List.of("test", "a"));
        productService.create("Product B", List.of("test", "b"));

        ResultActions result = mockMvc.perform(get("/api/v1/products"));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - 상품 수정")
    void t5() throws Exception {
        Product product = productService.create("Old Name", List.of("old", "keyword"));
        var request = new ProductController.UpdateRequest("New Name", List.of("new", "updated", "keyword"));

        ResultActions result = mockMvc.perform(put("/api/v1/products/{id}", product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.keywords", hasSize(3)));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - 존재하지 않는 상품 수정")
    void t6() throws Exception {
        var request = new ProductController.UpdateRequest("New Name", List.of("new"));

        ResultActions result = mockMvc.perform(put("/api/v1/products/{id}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - 상품 삭제")
    void t7() throws Exception {
        Product product = productService.create("To Delete", List.of("delete", "test"));

        ResultActions result = mockMvc.perform(delete("/api/v1/products/{id}", product.getId()));

        result.andDo(print())
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/products/search - 키워드로 KNN 검색")
    void t8() throws Exception {
        productService.create("Gaming Laptop", List.of("gaming", "laptop", "high-performance"));
        productService.create("Business Laptop", List.of("business", "laptop", "office"));
        productService.create("Gaming Mouse", List.of("gaming", "mouse", "RGB"));

        var request = new ProductController.SearchRequest(List.of("gaming", "computer"), 3);

        ResultActions result = mockMvc.perform(post("/api/v1/products/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id}/similar - 유사 상품 조회")
    void t9() throws Exception {
        Product laptop = productService.create("MacBook Pro", List.of("laptop", "apple", "development"));
        productService.create("iPhone 15", List.of("smartphone", "apple", "communication"));
        productService.create("iPad Pro", List.of("tablet", "apple", "drawing"));

        ResultActions result = mockMvc.perform(get("/api/v1/products/{id}/similar", laptop.getId())
                .param("k", "2"));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", not(hasItem("MacBook Pro"))));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id}/similar - 존재하지 않는 상품의 유사 상품 조회")
    void t10() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/products/{id}/similar", 99999L));

        result.andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/products/search - 전자기기 키워드로 KNN 검색")
    void t11() throws Exception {
        productService.create("iPhone 15", List.of("smartphone", "apple", "camera"));
        productService.create("Samsung Galaxy", List.of("smartphone", "android", "camera"));
        productService.create("Sony Headphones", List.of("headphone", "music", "wireless"));

        var request = new ProductController.SearchRequest(List.of("smartphone", "camera"), 3);

        ResultActions result = mockMvc.perform(post("/api/v1/products/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/products/search - 빈 키워드로 KNN 검색")
    void t12() throws Exception {
        productService.create("Test Product", List.of("test"));

        var request = new ProductController.SearchRequest(List.of(), 3);

        ResultActions result = mockMvc.perform(post("/api/v1/products/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/v1/products/chat - 채팅으로 상품 추천 요청")
    void t13() throws Exception {
        productService.create("Gaming Laptop", List.of("gaming", "laptop", "high-performance"));
        productService.create("Business Laptop", List.of("business", "laptop", "office"));
        productService.create("Gaming Mouse", List.of("gaming", "mouse", "RGB"));

        var request = new ProductController.ChatRequest("I need a laptop for gaming. Can you recommend something?");

        ResultActions result = mockMvc.perform(post("/api/v1/products/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("POST /api/v1/products/chat - 특정 상품과 유사한 상품 추천 요청")
    void t14() throws Exception {
        Product laptop = productService.create("MacBook Pro", List.of("laptop", "apple", "development"));
        productService.create("iPhone 15", List.of("smartphone", "apple", "communication"));
        productService.create("iPad Pro", List.of("tablet", "apple", "drawing"));

        var request = new ProductController.ChatRequest("Find products similar to product ID " + laptop.getId());

        ResultActions result = mockMvc.perform(post("/api/v1/products/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/products/chat - 모든 상품 조회 요청")
    void t15() throws Exception {
        productService.create("Product A", List.of("test", "a"));
        productService.create("Product B", List.of("test", "b"));

        var request = new ProductController.ChatRequest("Show me all available products");

        ResultActions result = mockMvc.perform(post("/api/v1/products/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/products/chat - 카테고리별 상품 검색 요청")
    void t16() throws Exception {
        productService.create("Running Shoes", List.of("sports", "running", "fitness"));
        productService.create("Yoga Mat", List.of("sports", "yoga", "fitness"));
        productService.create("Dumbbells", List.of("sports", "weight", "fitness"));

        var request = new ProductController.ChatRequest("I'm looking for fitness equipment for home workout");

        ResultActions result = mockMvc.perform(post("/api/v1/products/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

    }
}
