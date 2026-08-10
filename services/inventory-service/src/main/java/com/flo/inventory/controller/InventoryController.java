package com.flo.inventory.controller;

import com.flo.inventory.model.Stock;
import com.flo.inventory.repository.StockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    return stockRepository
        .findById(productId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Stock not found: " + productId));
  }
}
