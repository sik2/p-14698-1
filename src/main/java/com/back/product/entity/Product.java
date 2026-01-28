package com.back.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column
    @Getter
    @Setter
    private String name;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter
    private List<ProductKeyword> keywords = new ArrayList<>();

    public void addKeyword(ProductKeyword keyword) {
        keywords.add(keyword);
        keyword.setProduct(this);
    }

    public void removeKeyword(ProductKeyword keyword) {
        keywords.remove(keyword);
        keyword.setProduct(null);
    }

    public void addKeyword(String keywordText) {
        ProductKeyword keyword = new ProductKeyword(keywordText);
        addKeyword(keyword);
    }
}
