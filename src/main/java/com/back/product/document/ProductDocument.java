package com.back.product.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "products")
@Data
public class ProductDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Dense_Vector, dims = 384)
    private float[] embedding;

}
