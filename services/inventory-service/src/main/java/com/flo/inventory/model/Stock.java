package com.flo.inventory.model;

public record Stock(Long productId, int stockCount, int reservedCount) {}
