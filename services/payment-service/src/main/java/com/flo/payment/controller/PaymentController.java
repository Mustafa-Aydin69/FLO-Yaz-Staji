package com.flo.payment.controller;

import com.flo.payment.client.BankApiClient;
import com.flo.payment.client.CartDto;
import com.flo.payment.client.CartItemDto;
import com.flo.payment.client.CartServiceClient;
import com.flo.payment.client.InventoryServiceClient;
import com.flo.payment.model.CreatePaymentRequest;
import com.flo.payment.model.Payment;
import com.flo.payment.model.PaymentStatus;
import com.flo.payment.repository.PaymentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PaymentController {

  private final PaymentRepository paymentRepository;
  private final CartServiceClient cartServiceClient;
  private final InventoryServiceClient inventoryServiceClient;
  private final BankApiClient bankApiClient;

  public PaymentController(
      PaymentRepository paymentRepository,
      CartServiceClient cartServiceClient,
      InventoryServiceClient inventoryServiceClient,
      BankApiClient bankApiClient) {
    this.paymentRepository = paymentRepository;
    this.cartServiceClient = cartServiceClient;
    this.inventoryServiceClient = inventoryServiceClient;
    this.bankApiClient = bankApiClient;
  }

  @PostMapping("/payment")
  @ResponseStatus(HttpStatus.CREATED)
  public Payment createPayment(@RequestBody CreatePaymentRequest request) {
    CartDto cart =
        cartServiceClient
            .findCart(request.cartId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cart not found: " + request.cartId()));

    if (cart.totalAmount() <= 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cart total amount must be positive");
    }

    boolean continueWithAvailable = Boolean.TRUE.equals(request.continueWithAvailable());
    List<CartItemDto> reserved = new ArrayList<>();
    List<Long> unavailable = new ArrayList<>();
    for (CartItemDto item : cart.items()) {
      if (inventoryServiceClient.reserve(item.productId(), item.quantity())) {
        reserved.add(item);
      } else {
        unavailable.add(item.productId());
      }
    }

    if (!unavailable.isEmpty() && !continueWithAvailable) {
      reserved.forEach(item -> inventoryServiceClient.release(item.productId(), item.quantity()));
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Insufficient stock for products: " + unavailable);
    }

    double amount =
        continueWithAvailable
            ? reserved.stream().mapToDouble(item -> item.price() * item.quantity()).sum()
            : cart.totalAmount();

    if (amount <= 0) {
      reserved.forEach(item -> inventoryServiceClient.release(item.productId(), item.quantity()));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No items available to charge");
    }

    String transactionId = bankApiClient.charge(amount);
    Payment payment =
        new Payment(
            UUID.randomUUID(),
            cart.cartId(),
            amount,
            PaymentStatus.SUCCESS,
            transactionId,
            Instant.now());
    return paymentRepository.save(payment);
  }

  @GetMapping("/payment/{paymentId}")
  public Payment getPayment(@PathVariable UUID paymentId) {
    return paymentRepository
        .findById(paymentId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Payment not found: " + paymentId));
  }
}
