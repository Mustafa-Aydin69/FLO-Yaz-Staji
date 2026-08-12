package com.flo.inventory.controller;

import com.flo.inventory.model.Stock;
import com.flo.inventory.model.StockAdjustmentRequest;
import com.flo.inventory.repository.StockRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class InventoryController {

  private final StockRepository stockRepository;
  private final Tracer tracer;

  public InventoryController(StockRepository stockRepository, OpenTelemetry openTelemetry) {
    this.stockRepository = stockRepository;
    this.tracer = openTelemetry.getTracer(InventoryController.class.getName());
  }

  @GetMapping("/inventory/{productId}")
  public Stock getStock(@PathVariable Long productId) {
    return findStockOrThrow(productId);
  }

  @PostMapping("/inventory/{productId}/reserve")
  public Stock reserve(@PathVariable Long productId, @RequestBody StockAdjustmentRequest request) {
    if (request.quantity() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be positive");
    }
    Span span = tracer.spanBuilder("reserve-stock").startSpan();
    span.setAttribute("inventory.product_id", productId);
    span.setAttribute("inventory.quantity", request.quantity());
    try (Scope scope = span.makeCurrent()) {
      Stock stock = findStockOrThrow(productId);
      int available = stock.stockCount() - stock.reservedCount();
      if (available < request.quantity()) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Insufficient stock for product: " + productId);
      }
      Stock updated =
          new Stock(
              stock.productId(), stock.stockCount(), stock.reservedCount() + request.quantity());
      span.setAttribute("inventory.reserved_count_after", updated.reservedCount());
      return stockRepository.save(updated);
    } catch (RuntimeException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }

  @PostMapping("/inventory/{productId}/release")
  public Stock release(@PathVariable Long productId, @RequestBody StockAdjustmentRequest request) {
    if (request.quantity() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be positive");
    }
    Span span = tracer.spanBuilder("release-stock").startSpan();
    span.setAttribute("inventory.product_id", productId);
    span.setAttribute("inventory.quantity", request.quantity());
    try (Scope scope = span.makeCurrent()) {
      Stock stock = findStockOrThrow(productId);
      int newReservedCount = Math.max(0, stock.reservedCount() - request.quantity());
      Stock updated = new Stock(stock.productId(), stock.stockCount(), newReservedCount);
      span.setAttribute("inventory.reserved_count_after", updated.reservedCount());
      return stockRepository.save(updated);
    } catch (RuntimeException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }

  private Stock findStockOrThrow(Long productId) {
    return stockRepository
        .findById(productId)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found: " + productId));
  }
}
