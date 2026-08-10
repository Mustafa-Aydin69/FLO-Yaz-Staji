package com.flo.payment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InventoryServiceClient {

  private static final Logger log = LoggerFactory.getLogger(InventoryServiceClient.class);

  private final RestClient restClient;

  public InventoryServiceClient(@Value("${inventory-service.base-url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  public boolean reserve(Long productId, int quantity) {
    try {
      restClient
          .post()
          .uri("/inventory/{productId}/reserve", productId)
          .body(new StockAdjustmentRequest(quantity))
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (HttpClientErrorException.Conflict | HttpClientErrorException.NotFound ex) {
      return false;
    } catch (RestClientException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Inventory service unavailable: " + ex.getMessage());
    }
  }

  public void release(Long productId, int quantity) {
    try {
      restClient
          .post()
          .uri("/inventory/{productId}/release", productId)
          .body(new StockAdjustmentRequest(quantity))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      log.warn("Failed to release stock for product {}: {}", productId, ex.getMessage());
    }
  }

  private record StockAdjustmentRequest(int quantity) {}
}
