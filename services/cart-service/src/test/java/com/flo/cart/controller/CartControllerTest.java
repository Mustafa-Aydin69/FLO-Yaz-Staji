package com.flo.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flo.cart.client.ProductDto;
import com.flo.cart.client.SearchServiceClient;
import com.flo.cart.model.Cart;
import com.flo.cart.model.CartItem;
import com.flo.cart.repository.CartRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
class CartControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CartRepository cartRepository;

  @MockBean private SearchServiceClient searchServiceClient;

  private static final UUID CART_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final ProductDto NIKE =
      new ProductDto(1L, "Air Runner X1", "Nike", "Spor Ayakkabı", 2899.90, 42);

  private Cart emptyCart() {
    return new Cart(CART_ID, "test-user", List.of(), Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void createCart_returnsCreatedCartWithEmptyItems() throws Exception {
    when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"test-user\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value("test-user"))
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void getCart_returnsEmptyCart_whenCartHasNoItems() throws Exception {
    when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(emptyCart()));

    mockMvc
        .perform(get("/cart/" + CART_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void getCart_returnsNotFound_whenCartDoesNotExist() throws Exception {
    when(cartRepository.findById(CART_ID)).thenReturn(Optional.empty());

    mockMvc.perform(get("/cart/" + CART_ID)).andExpect(status().isNotFound());
  }

  @Test
  void addItem_returnsUpdatedCart_whenProductExists() throws Exception {
    when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(emptyCart()));
    when(searchServiceClient.findProduct(1L)).thenReturn(Optional.of(NIKE));
    when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/cart/" + CART_ID + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].productName").value("Air Runner X1"))
        .andExpect(jsonPath("$.items[0].quantity").value(2));
  }

  @Test
  void addItem_returnsNotFound_whenProductDoesNotExist() throws Exception {
    when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(emptyCart()));
    when(searchServiceClient.findProduct(999L)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/cart/" + CART_ID + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":999,\"quantity\":1}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void addItem_returnsBadRequest_whenQuantityIsNotPositive() throws Exception {
    mockMvc
        .perform(
            post("/cart/" + CART_ID + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removeItem_returnsUpdatedCart_whenItemExists() throws Exception {
    CartItem item = new CartItem(1L, "Air Runner X1", 2899.90, 2);
    Cart cartWithItem =
        new Cart(CART_ID, "test-user", List.of(item), Instant.parse("2026-01-01T00:00:00Z"));
    when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cartWithItem));
    when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(delete("/cart/" + CART_ID + "/items/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void removeItem_returnsNotFound_whenItemNotInCart() throws Exception {
    when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(emptyCart()));

    mockMvc.perform(delete("/cart/" + CART_ID + "/items/999")).andExpect(status().isNotFound());
  }
}
