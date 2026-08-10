package com.flo.payment.model;

import java.time.Instant;
import java.util.UUID;

public record Payment(
    UUID paymentId,
    UUID cartId,
    double amount,
    PaymentStatus status,
    String transactionId,
    Instant createdAt) {}
