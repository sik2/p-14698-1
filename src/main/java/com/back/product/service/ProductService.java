package com.back.product.service;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import com.back.product.document.ProductDocument;
import com.back.product.entity.Product;
import com.back.product.repository.ProductDocumentRepository;
import com.back.product.repository.ProductRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductDocumentRepository productDocumentRepository;
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Transactional
    protected Product _create(String name, List<String> keywords){
        Product product = new Product();
        product.setName(name);
        keywords.stream().forEach(k->product.addKeyword(k));
        return productRepository.save(product);
    }

    public Product create(String name, List<String> keywords){
        Product product = this._create(name, keywords);
        List<float[]> embeddings = embeddingModel.embed(keywords);
        ProductDocument doc = new ProductDocument();
        doc.setEmbedding(calculateAverage(embeddings));
        doc.setId(product.getId());
        productDocumentRepository.save(doc);
        return product;
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findAll() {
        return StreamSupport.stream(productRepository.findAll().spliterator(), false).toList();
    }

    public Optional<ProductDocument> findDocumentById(Long id) {
        return productDocumentRepository.findById(id);
    }

    @Transactional
    public Product update(Long id, String name, List<String> keywords) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        product.setName(name);
        product.getKeywords().clear();
        keywords.forEach(product::addKeyword);

        Product savedProduct = productRepository.save(product);

        ProductDocument doc = productDocumentRepository.findById(id)
                .orElse(new ProductDocument());
        doc.setId(savedProduct.getId());

        if (!keywords.isEmpty()) {
            List<float[]> embeddings = embeddingModel.embed(keywords);
            doc.setEmbedding(calculateAverage(embeddings));
        }

        productDocumentRepository.save(doc);
        return savedProduct;
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
        productDocumentRepository.deleteById(id);
    }

    public List<Product> knnSearch(String query, int k) {
        float[] queryEmbedding = embeddingModel.embed(query);
        List<ProductDocument> docs = knnSearchByVector(queryEmbedding, "embedding", k, k * 2);
        List<Long> ids = docs.stream().map(ProductDocument::getId).toList();

        // KNN 결과 순서대로 정렬
        Map<Long, Product> productMap = StreamSupport
                .stream(productRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(Product::getId, p -> p));

        return ids.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ProductDocument> knnSearchByVector(float[] queryVector, String field, int k, int numCandidates) {
        List<Float> vectorList = toFloatList(queryVector);

        KnnSearch knnSearch = KnnSearch.of(knn -> knn
                .queryVector(vectorList)
                .field(field)
                .k(k)
                .numCandidates(numCandidates)
        );

        NativeQuery query = NativeQuery.builder()
                .withKnnSearches(knnSearch)
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchTemplate.search(query, ProductDocument.class);
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new java.util.ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    private float[] calculateAverage(List<float[]> vectors) {
        if (vectors.isEmpty()) return new float[0];

        int dimension = vectors.get(0).length;
        float[] sum = new float[dimension];

        for (float[] vector : vectors) {
            for (int i = 0; i < dimension; i++) {
                sum[i] += vector[i];
            }
        }

        for (int i = 0; i < dimension; i++) {
            sum[i] /= vectors.size();
        }

        return sum;
    }
}
