package com.flo.common.http;

import java.util.function.Supplier;
import org.springframework.web.client.ResourceAccessException;

public final class RemoteCallRetry {

  private static final int MAX_ATTEMPTS = 2;

  private RemoteCallRetry() {}

  public static <T> T withRetry(Supplier<T> action) {
    ResourceAccessException lastError = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        return action.get();
      } catch (ResourceAccessException ex) {
        lastError = ex;
      }
    }
    throw lastError;
  }
}
