package com.flo.payment.model;

import java.util.UUID;

public record CreatePaymentRequest(UUID cartId, Boolean continueWithAvailable) {}
