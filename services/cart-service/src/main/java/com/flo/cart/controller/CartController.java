package com.flo.cart.controller;

import com.flo.cart.model.Cart;
import com.flo.cart.model.CreateCartRequest;
import com.flo.cart.repository.CartRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CartController {

  private final CartRepository cartRepository;

  public CartController(CartRepository cartRepository) {
    this.cartRepository = cartRepository;
  }

  @PostMapping("/cart")
  @ResponseStatus(HttpStatus.CREATED)
  public Cart createCart(@RequestBody(required = false) CreateCartRequest request) {
    String userId = request != null ? request.userId() : null;
    Cart cart = new Cart(UUID.randomUUID(), userId, List.of(), Instant.now());
    return cartRepository.save(cart);
  }
}
