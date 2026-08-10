package com.flo.inventory.controller;

import com.flo.inventory.model.Stock;
import com.flo.inventory.model.StockAdjustmentRequest;
import com.flo.inventory.repository.StockRepository;
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

  public InventoryController(StockRepository stockRepository) {
    this.stockRepository = stockRepository;
  }

  @GetMapping("/inventory/{productId}")
  public Stock getStock(@PathVariable Long productId) {
    return findStockOrThrow(productId);
  }

  @PostMapping("/inventory/{productId}/reserve")
  public Stock reserve(
      @PathVariable Long productId, @RequestBody StockAdjustmentRequest request) {
    if (request.quantity() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be positive");
    }
    Stock stock = findStockOrThrow(productId);
    int available = stock.stockCount() - stock.reservedCount();
    if (available < request.quantity()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Insufficient stock for product: " + productId);
    }
    Stock updated =
        new Stock(
            stock.productId(), stock.stockCount(), stock.reservedCount() + request.quantity());
    return stockRepository.save(updated);
  }

  private Stock findStockOrThrow(Long productId) {
    return stockRepository
        .findById(productId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Stock not found: " + productId));
  }
}
