package com.flo.common.http;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

public final class RemoteCallException {

  private RemoteCallException() {}

  public static ResponseStatusException unavailable(String serviceName, RestClientException cause) {
    return new ResponseStatusException(
        HttpStatus.BAD_GATEWAY, serviceName + " unavailable: " + cause.getMessage());
  }
}
