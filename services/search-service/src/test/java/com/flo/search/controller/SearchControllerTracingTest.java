package com.flo.search.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.flo.search.repository.ProductRepository;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchControllerTracingTest {

  @Test
  void filterCatalogSpan_marksErrorStatus_whenRepositoryThrows() {
    InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();
    OpenTelemetrySdk openTelemetry =
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

    ProductRepository productRepository = mock(ProductRepository.class);
    when(productRepository.findAll()).thenThrow(new RuntimeException("kasitli test hatasi"));

    SearchController controller = new SearchController(productRepository, openTelemetry);

    assertThrows(RuntimeException.class, () -> controller.search("nike"));

    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    SpanData filterCatalogSpan =
        spans.stream()
            .filter(s -> s.getName().equals("filter-catalog"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("filter-catalog span not exported"));

    assertEquals(StatusCode.ERROR, filterCatalogSpan.getStatus().getStatusCode());
    assertTrue(
        filterCatalogSpan.getEvents().stream()
            .anyMatch(event -> event.getName().equals("exception")));
  }
}
