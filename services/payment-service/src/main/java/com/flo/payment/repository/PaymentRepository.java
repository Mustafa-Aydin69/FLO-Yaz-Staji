package com.flo.payment.repository;

import com.flo.payment.model.Payment;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {

  private final Map<UUID, Payment> payments = new ConcurrentHashMap<>();

  public Payment save(Payment payment) {
    payments.put(payment.paymentId(), payment);
    return payment;
  }

  public Optional<Payment> findById(UUID paymentId) {
    return Optional.ofNullable(payments.get(paymentId));
  }
}
