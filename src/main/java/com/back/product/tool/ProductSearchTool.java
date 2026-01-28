package com.back.product.tool;

import com.back.product.entity.Product;
import com.back.product.entity.ProductKeyword;
import com.back.product.service.ProductService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductSearchTool {

    private final ProductService productService;

    public ProductSearchTool(ProductService productService) {
        this.productService = productService;
    }

    @Tool(description = "주어진 키워드와 유사한 상품을 KNN 벡터 검색으로 찾습니다. 검색 키워드와 의미적으로 유사한 상품의 이름과 키워드 목록을 반환합니다.")
    public List<ProductInfo> searchProducts(
            @ToolParam(description = "유사한 상품을 검색할 키워드들 (쉼표로 구분). 예: '노트북, 게이밍, 고성능'") String keywords,
            @ToolParam(description = "반환할 최대 상품 수. 기본값은 5입니다.") int k) {

        if (k <= 0) k = 5;

        List<String> keywordList = Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<Product> products = productService.knnSearch(keywordList, k);

        return products.stream()
                .map(p -> new ProductInfo(
                        p.getId(),
                        p.getName(),
                        p.getKeywords().stream()
                                .map(ProductKeyword::getKeyword)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @Tool(description = "특정 상품 ID를 기준으로 유사한 상품을 찾습니다. 키워드 임베딩을 기반으로 비슷한 특성을 가진 상품들을 반환합니다.")
    public List<ProductInfo> findSimilarProducts(
            @ToolParam(description = "유사한 상품을 찾을 기준 상품의 ID") Long productId,
            @ToolParam(description = "반환할 최대 유사 상품 수. 기본값은 5입니다.") int k) {

        if (k <= 0) k = 5;

        List<Product> products = productService.findSimilarProducts(productId, k);

        return products.stream()
                .map(p -> new ProductInfo(
                        p.getId(),
                        p.getName(),
                        p.getKeywords().stream()
                                .map(ProductKeyword::getKeyword)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @Tool(description = "상품 ID로 특정 상품의 상세 정보를 조회합니다.")
    public ProductInfo getProductById(
            @ToolParam(description = "조회할 상품의 ID") Long productId) {

        return productService.findById(productId)
                .map(p -> new ProductInfo(
                        p.getId(),
                        p.getName(),
                        p.getKeywords().stream()
                                .map(ProductKeyword::getKeyword)
                                .collect(Collectors.toList())
                ))
                .orElse(null);
    }

    @Tool(description = "시스템에 등록된 모든 상품 목록을 조회합니다.")
    public List<ProductInfo> getAllProducts() {
        return productService.findAll().stream()
                .map(p -> new ProductInfo(
                        p.getId(),
                        p.getName(),
                        p.getKeywords().stream()
                                .map(ProductKeyword::getKeyword)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    public record ProductInfo(Long id, String name, List<String> keywords) {}
}
