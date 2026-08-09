package com.flo.cart.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Cart(UUID cartId, String userId, List<CartItem> items, Instant createdAt) {}
