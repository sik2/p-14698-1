package com.back.product.controller;

import com.back.product.entity.Product;
import com.back.product.service.ProductChatService;
import com.back.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private final ProductChatService productChatService;

    // ==================== CREATE ====================
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody CreateRequest request) {
        Product product = productService.create(request.name(), request.keywords());
        return ResponseEntity.ok(product);
    }

    // ==================== READ ====================
    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Product>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    // ==================== UPDATE ====================
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody UpdateRequest request) {
        try {
            Product product = productService.update(id, request.name(), request.keywords());
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== DELETE ====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== KNN SEARCH ====================
    @PostMapping("/search")
    public ResponseEntity<List<Product>> knnSearch(
            @RequestBody SearchRequest request) {
        List<Product> results = productService.knnSearch(request.keywords(), request.k());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<Product>> findSimilarProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int k) {
        try {
            List<Product> results = productService.findSimilarProducts(id, k);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== CHAT ====================
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        var response = productChatService.chat(request.message());
        return ResponseEntity.ok(new ChatResponse(response.message()));
    }

    // ==================== Request DTOs ====================
    public record CreateRequest(String name, List<String> keywords) {}
    public record UpdateRequest(String name, List<String> keywords) {}
    public record SearchRequest(List<String> keywords, int k) {
        public SearchRequest {
            if (k <= 0) k = 10;
        }
    }
    public record ChatRequest(String message) {}
    public record ChatResponse(String message) {}

    @GetMapping(value="/chat/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(0L);

        // 2. ChatClient를 통해 스트림(Flux) 요청
        Flux<String> response = productChatService.chatStream(message);

        // 3. 스트림 구독 및 전송
        response.subscribe(
                r -> {
                    try {
                        // 토큰 텍스트 추출
                        if (r != null) {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(r));
                        }
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> emitter.completeWithError(error), // 에러 발생 시
                () -> emitter.complete() // 스트림 종료 시
        );

        return emitter;
    }
}
