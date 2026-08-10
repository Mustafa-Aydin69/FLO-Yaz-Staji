package com.flo.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flo.payment.client.BankApiClient;
import com.flo.payment.client.CartDto;
import com.flo.payment.client.CartItemDto;
import com.flo.payment.client.CartServiceClient;
import com.flo.payment.client.InventoryServiceClient;
import com.flo.payment.model.Payment;
import com.flo.payment.model.PaymentStatus;
import com.flo.payment.repository.PaymentRepository;
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

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PaymentRepository paymentRepository;

  @MockBean private CartServiceClient cartServiceClient;

  @MockBean private InventoryServiceClient inventoryServiceClient;

  @MockBean private BankApiClient bankApiClient;

  private static final UUID CART_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private CartDto cartWithTotal(double totalAmount) {
    return new CartDto(
        CART_ID, "test-user", List.of(), totalAmount, Instant.parse("2026-01-01T00:00:00Z"));
  }

  private CartDto cartWithItem(long productId, double price, int quantity) {
    CartItemDto item = new CartItemDto(productId, "Air Runner X1", price, quantity);
    return new CartDto(
        CART_ID,
        "test-user",
        List.of(item),
        price * quantity,
        Instant.parse("2026-01-01T00:00:00Z"));
  }

  private CartDto cartWithTwoItems() {
    CartItemDto available = new CartItemDto(1L, "Air Runner X1", 2899.90, 1);
    CartItemDto unavailable = new CartItemDto(4L, "Old Skool Wave", 1799.00, 1);
    return new CartDto(
        CART_ID,
        "test-user",
        List.of(available, unavailable),
        2899.90 + 1799.00,
        Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void createPayment_returnsCreatedPayment_whenCartIsValid() throws Exception {
    when(cartServiceClient.findCart(CART_ID)).thenReturn(Optional.of(cartWithItem(1L, 2899.90, 1)));
    when(inventoryServiceClient.reserve(anyLong(), anyInt())).thenReturn(true);
    when(bankApiClient.charge(anyDouble())).thenReturn("txn-123");
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartId\":\"" + CART_ID + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.cartId").value(CART_ID.toString()))
        .andExpect(jsonPath("$.amount").value(2899.90))
        .andExpect(jsonPath("$.status").value(PaymentStatus.SUCCESS.name()))
        .andExpect(jsonPath("$.transactionId").value("txn-123"));
  }

  @Test
  void createPayment_returnsNotFound_whenCartDoesNotExist() throws Exception {
    when(cartServiceClient.findCart(CART_ID)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartId\":\"" + CART_ID + "\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void createPayment_returnsBadRequest_whenCartTotalIsZero() throws Exception {
    when(cartServiceClient.findCart(CART_ID)).thenReturn(Optional.of(cartWithTotal(0.0)));

    mockMvc
        .perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartId\":\"" + CART_ID + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createPayment_returnsConflict_andReleasesReserved_whenAnyItemUnavailable() throws Exception {
    when(cartServiceClient.findCart(CART_ID)).thenReturn(Optional.of(cartWithTwoItems()));
    when(inventoryServiceClient.reserve(eq(1L), anyInt())).thenReturn(true);
    when(inventoryServiceClient.reserve(eq(4L), anyInt())).thenReturn(false);

    mockMvc
        .perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartId\":\"" + CART_ID + "\"}"))
        .andExpect(status().isConflict());

    verify(inventoryServiceClient).release(eq(1L), anyInt());
    verify(bankApiClient, never()).charge(anyDouble());
  }

  @Test
  void createPayment_chargesOnlyAvailableItems_whenContinueWithAvailableIsTrue() throws Exception {
    when(cartServiceClient.findCart(CART_ID)).thenReturn(Optional.of(cartWithTwoItems()));
    when(inventoryServiceClient.reserve(eq(1L), anyInt())).thenReturn(true);
    when(inventoryServiceClient.reserve(eq(4L), anyInt())).thenReturn(false);
    when(bankApiClient.charge(anyDouble())).thenReturn("txn-partial");
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartId\":\"" + CART_ID + "\",\"continueWithAvailable\":true}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.amount").value(2899.90));

    verify(inventoryServiceClient, never()).release(eq(1L), anyInt());
  }

  @Test
  void getPayment_returnsPayment_whenExists() throws Exception {
    UUID paymentId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    Payment payment =
        new Payment(paymentId, CART_ID, 2899.90, PaymentStatus.SUCCESS, "txn-123", Instant.now());
    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    mockMvc
        .perform(get("/payment/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("txn-123"));
  }

  @Test
  void getPayment_returnsNotFound_whenPaymentDoesNotExist() throws Exception {
    UUID paymentId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/payment/" + paymentId)).andExpect(status().isNotFound());
  }
}
