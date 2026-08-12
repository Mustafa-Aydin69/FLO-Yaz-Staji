package com.flo.inventory.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.flo.inventory.model.Stock;
import com.flo.inventory.model.StockAdjustmentRequest;
import com.flo.inventory.repository.StockRepository;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class InventoryControllerTracingTest {

  @Test
  void reserveStockSpan_marksErrorStatus_whenStockInsufficient() {
    InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();
    OpenTelemetrySdk openTelemetry =
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

    StockRepository stockRepository = mock(StockRepository.class);
    when(stockRepository.findById(1L)).thenReturn(Optional.of(new Stock(1L, 5, 5)));

    InventoryController controller = new InventoryController(stockRepository, openTelemetry);

    assertThrows(
        ResponseStatusException.class, () -> controller.reserve(1L, new StockAdjustmentRequest(1)));

    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    SpanData reserveStockSpan =
        spans.stream()
            .filter(s -> s.getName().equals("reserve-stock"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("reserve-stock span not exported"));

    assertEquals(StatusCode.ERROR, reserveStockSpan.getStatus().getStatusCode());
  }
}
