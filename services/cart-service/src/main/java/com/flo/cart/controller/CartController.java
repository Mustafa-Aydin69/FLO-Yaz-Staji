package com.flo.cart.controller;

import com.flo.cart.client.ProductDto;
import com.flo.cart.client.SearchServiceClient;
import com.flo.cart.model.AddCartItemRequest;
import com.flo.cart.model.Cart;
import com.flo.cart.model.CartItem;
import com.flo.cart.model.CreateCartRequest;
import com.flo.cart.repository.CartRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CartController {

  private final CartRepository cartRepository;
  private final SearchServiceClient searchServiceClient;

  public CartController(CartRepository cartRepository, SearchServiceClient searchServiceClient) {
    this.cartRepository = cartRepository;
    this.searchServiceClient = searchServiceClient;
  }

  @PostMapping("/cart")
  @ResponseStatus(HttpStatus.CREATED)
  public Cart createCart(@RequestBody(required = false) CreateCartRequest request) {
    String userId = request != null ? request.userId() : null;
    Cart cart = new Cart(UUID.randomUUID(), userId, List.of(), 0.0, Instant.now());
    return cartRepository.save(cart);
  }

  @GetMapping("/cart/{cartId}")
  public Cart getCart(@PathVariable UUID cartId) {
    return cartRepository
        .findById(cartId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found: " + cartId));
  }

  @PostMapping("/cart/{cartId}/items")
  public Cart addItem(@PathVariable UUID cartId, @RequestBody AddCartItemRequest request) {
    if (request.quantity() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be positive");
    }
    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found: " + cartId));
    ProductDto product =
        searchServiceClient
            .findProduct(request.productId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found: " + request.productId()));

    List<CartItem> updatedItems = new ArrayList<>(cart.items());
    updatedItems.add(
        new CartItem(product.id(), product.name(), product.price(), request.quantity()));
    Cart updatedCart =
        new Cart(
            cart.cartId(),
            cart.userId(),
            List.copyOf(updatedItems),
            totalAmount(updatedItems),
            cart.createdAt());
    return cartRepository.save(updatedCart);
  }

  @DeleteMapping("/cart/{cartId}/items/{productId}")
  public Cart removeItem(@PathVariable UUID cartId, @PathVariable Long productId) {
    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found: " + cartId));
    boolean present = cart.items().stream().anyMatch(item -> item.productId().equals(productId));
    if (!present) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Item not found in cart: " + productId);
    }
    List<CartItem> updatedItems =
        cart.items().stream().filter(item -> !item.productId().equals(productId)).toList();
    Cart updatedCart =
        new Cart(
            cart.cartId(),
            cart.userId(),
            updatedItems,
            totalAmount(updatedItems),
            cart.createdAt());
    return cartRepository.save(updatedCart);
  }

  private static double totalAmount(List<CartItem> items) {
    return items.stream().mapToDouble(item -> item.price() * item.quantity()).sum();
  }
}
