package com.flo.common.http;

import java.util.Optional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class RemoteLookup {

  private RemoteLookup() {}

  public static <T> Optional<T> findOrEmpty(
      RestClient restClient,
      String serviceName,
      String uri,
      Class<T> responseType,
      Object... uriVariables) {
    try {
      return Optional.ofNullable(
          RemoteCallRetry.withRetry(
              () -> restClient.get().uri(uri, uriVariables).retrieve().body(responseType)));
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      throw RemoteCallException.unavailable(serviceName, ex);
    }
  }
}
