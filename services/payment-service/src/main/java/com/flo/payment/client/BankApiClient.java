package com.flo.payment.client;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BankApiClient {

  private static final long SIMULATED_LATENCY_MILLIS = 300;

  public String charge(double amount) {
    try {
      Thread.sleep(SIMULATED_LATENCY_MILLIS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    return UUID.randomUUID().toString();
  }
}
