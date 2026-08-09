package com.flo.cart.client;

public record ProductDto(
    Long id, String name, String brand, String category, double price, int stock) {}
