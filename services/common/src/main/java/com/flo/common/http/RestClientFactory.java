package com.flo.common.http;

import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

public final class RestClientFactory {

  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  private RestClientFactory() {}

  public static RestClient create(String baseUrl) {
    ClientHttpRequestFactorySettings settings =
        ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(TIMEOUT)
            .withReadTimeout(TIMEOUT);
    return RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(ClientHttpRequestFactories.get(settings))
        .build();
  }
}
