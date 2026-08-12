package com.flo.search.controller;

import com.flo.search.model.Product;
import com.flo.search.repository.ProductRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SearchController {

  private final ProductRepository productRepository;
  private final Tracer tracer;

  public SearchController(ProductRepository productRepository, OpenTelemetry openTelemetry) {
    this.productRepository = productRepository;
    this.tracer = openTelemetry.getTracer(SearchController.class.getName());
  }

  @GetMapping("/search")
  public List<Product> search(@RequestParam String q) {
    if (q.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Query parameter 'q' must not be blank");
    }
    Span span = tracer.spanBuilder("filter-catalog").startSpan();
    span.setAttribute("search.query", q);
    try (Scope scope = span.makeCurrent()) {
      String needle = q.toLowerCase();
      List<Product> results =
          productRepository.findAll().stream()
              .filter(
                  p ->
                      p.name().toLowerCase().contains(needle)
                          || p.category().toLowerCase().contains(needle)
                          || p.brand().toLowerCase().contains(needle))
              .toList();
      span.setAttribute("search.result_count", results.size());
      return results;
    } catch (RuntimeException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }

  @GetMapping("/products/{id}")
  public Product getById(@PathVariable Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
  }
}
