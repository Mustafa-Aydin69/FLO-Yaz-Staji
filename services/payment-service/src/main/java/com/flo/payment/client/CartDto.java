package com.flo.payment.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartDto(
    UUID cartId, String userId, List<CartItemDto> items, double totalAmount, Instant createdAt) {}
