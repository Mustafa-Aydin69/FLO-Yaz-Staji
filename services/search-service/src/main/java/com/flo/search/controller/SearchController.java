package com.flo.search.controller;

import com.flo.search.model.Product;
import com.flo.search.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final ProductRepository productRepository;

    public SearchController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam String q) {
        String needle = q.toLowerCase();
        return productRepository.findAll().stream()
                .filter(p -> p.name().toLowerCase().contains(needle)
                        || p.category().toLowerCase().contains(needle)
                        || p.brand().toLowerCase().contains(needle))
                .toList();
    }
}
