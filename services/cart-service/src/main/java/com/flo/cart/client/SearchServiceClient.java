package com.flo.cart.client;

import com.flo.common.http.RemoteLookup;
import com.flo.common.http.RestClientFactory;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SearchServiceClient {

  private final RestClient restClient;

  public SearchServiceClient(@Value("${search-service.base-url}") String baseUrl) {
    this.restClient = RestClientFactory.create(baseUrl);
  }

  public Optional<ProductDto> findProduct(Long productId) {
    return RemoteLookup.findOrEmpty(
        restClient, "Search service", "/products/{id}", ProductDto.class, productId);
  }
}
