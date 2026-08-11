package com.flo.common.correlation;

import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class RequestIdPropagationInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    String requestId = MDC.get(RequestIdFilter.MDC_KEY);
    if (requestId != null) {
      request.getHeaders().add(RequestIdFilter.HEADER_NAME, requestId);
    }
    return execution.execute(request, body);
  }
}
