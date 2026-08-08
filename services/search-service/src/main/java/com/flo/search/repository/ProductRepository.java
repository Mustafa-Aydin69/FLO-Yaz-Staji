package com.flo.search.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flo.search.model.Product;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Repository
public class ProductRepository {

    private final List<Product> products;

    public ProductRepository(ObjectMapper objectMapper) throws IOException {
        try (InputStream in = new ClassPathResource("products.json").getInputStream()) {
            this.products = objectMapper.readValue(in, new TypeReference<List<Product>>() {});
        }
    }

    public List<Product> findAll() {
        return products;
    }
}
