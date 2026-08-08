package com.flo.search.model;

public record Product(
    Long id, String name, String brand, String category, double price, int stock) {}
