package com.flo.payment.client;

import com.flo.common.http.RemoteLookup;
import com.flo.common.http.RestClientFactory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CartServiceClient {

  private final RestClient restClient;

  public CartServiceClient(@Value("${cart-service.base-url}") String baseUrl) {
    this.restClient = RestClientFactory.create(baseUrl);
  }

  public Optional<CartDto> findCart(UUID cartId) {
    return RemoteLookup.findOrEmpty(
        restClient, "Cart service", "/cart/{cartId}", CartDto.class, cartId);
  }
}
