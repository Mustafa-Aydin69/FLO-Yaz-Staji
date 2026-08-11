package com.flo.payment.client;

import com.flo.common.http.RemoteCallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
      throw RemoteCallException.unavailable("Inventory service", ex);
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
