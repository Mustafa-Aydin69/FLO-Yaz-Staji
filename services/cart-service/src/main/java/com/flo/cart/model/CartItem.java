package com.flo.cart.model;

public record CartItem(Long productId, String productName, double price, int quantity) {}
