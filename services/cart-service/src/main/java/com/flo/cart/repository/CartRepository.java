package com.flo.cart.repository;

import com.flo.cart.model.Cart;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepository {

  private final Map<UUID, Cart> carts = new ConcurrentHashMap<>();

  public Cart save(Cart cart) {
    carts.put(cart.cartId(), cart);
    return cart;
  }

  public Optional<Cart> findById(UUID cartId) {
    return Optional.ofNullable(carts.get(cartId));
  }
}
