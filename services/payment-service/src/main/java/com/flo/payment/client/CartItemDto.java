package com.flo.payment.client;

public record CartItemDto(Long productId, String productName, double price, int quantity) {}
