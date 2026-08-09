package com.flo.cart.client;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SearchServiceClient {

  private final RestClient restClient;

  public SearchServiceClient(@Value("${search-service.base-url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  public Optional<ProductDto> findProduct(Long productId) {
    try {
      return Optional.ofNullable(
          restClient.get().uri("/products/{id}", productId).retrieve().body(ProductDto.class));
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Search service unavailable: " + ex.getMessage());
    }
  }
}
