package com.flo.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.flo.payment.client.BankApiClient;
import com.flo.payment.client.CartDto;
import com.flo.payment.client.CartItemDto;
import com.flo.payment.client.CartServiceClient;
import com.flo.payment.client.InventoryServiceClient;
import com.flo.payment.model.CreatePaymentRequest;
import com.flo.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentControllerTracingTest {

  @Test
  void bankChargeSpan_marksErrorStatus_whenBankApiFails() {
    InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();
    OpenTelemetrySdk openTelemetry =
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

    UUID cartId = UUID.randomUUID();
    CartDto cart =
        new CartDto(
            cartId,
            "test-user",
            List.of(new CartItemDto(1L, "Air Runner X1", 2899.90, 1)),
            2899.90,
            Instant.now());

    CartServiceClient cartServiceClient = mock(CartServiceClient.class);
    when(cartServiceClient.findCart(cartId)).thenReturn(Optional.of(cart));

    InventoryServiceClient inventoryServiceClient = mock(InventoryServiceClient.class);
    when(inventoryServiceClient.reserve(1L, 1)).thenReturn(true);

    BankApiClient bankApiClient = mock(BankApiClient.class);
    when(bankApiClient.charge(2899.90)).thenThrow(new RuntimeException("POS timeout"));

    PaymentController controller =
        new PaymentController(
            mock(PaymentRepository.class),
            cartServiceClient,
            inventoryServiceClient,
            bankApiClient,
            openTelemetry,
            new SimpleMeterRegistry());

    assertThrows(
        RuntimeException.class,
        () -> controller.createPayment(new CreatePaymentRequest(cartId, null)));

    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    SpanData bankChargeSpan =
        spans.stream()
            .filter(s -> s.getName().equals("bank-charge"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("bank-charge span not exported"));

    assertEquals(StatusCode.ERROR, bankChargeSpan.getStatus().getStatusCode());
  }
}
