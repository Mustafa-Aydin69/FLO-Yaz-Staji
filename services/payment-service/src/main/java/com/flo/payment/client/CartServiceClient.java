package com.flo.payment.client;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CartServiceClient {

  private final RestClient restClient;

  public CartServiceClient(@Value("${cart-service.base-url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  public Optional<CartDto> findCart(java.util.UUID cartId) {
    try {
      return Optional.ofNullable(
          restClient.get().uri("/cart/{cartId}", cartId).retrieve().body(CartDto.class));
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Cart service unavailable: " + ex.getMessage());
    }
  }
}
