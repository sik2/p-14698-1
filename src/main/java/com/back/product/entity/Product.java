package com.back.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "embedding")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    @Getter
    @Setter
    private float[] embedding;

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
