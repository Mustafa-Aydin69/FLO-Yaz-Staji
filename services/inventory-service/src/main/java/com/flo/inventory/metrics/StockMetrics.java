package com.flo.inventory.metrics;

import com.flo.inventory.model.Stock;
import com.flo.inventory.repository.StockRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class StockMetrics {

  private final StockRepository stockRepository;
  private final MeterRegistry meterRegistry;

  public StockMetrics(StockRepository stockRepository, MeterRegistry meterRegistry) {
    this.stockRepository = stockRepository;
    this.meterRegistry = meterRegistry;
  }

  @PostConstruct
  void registerGauges() {
    for (Stock stock : stockRepository.findAll()) {
      Long productId = stock.productId();
      Gauge.builder(
              "stock_level",
              stockRepository,
              repository -> availableStock(repository.findById(productId).orElse(null)))
          .tag("product_id", String.valueOf(productId))
          .register(meterRegistry);
    }
  }

  private double availableStock(Stock stock) {
    return stock == null ? 0 : stock.stockCount() - stock.reservedCount();
  }
}
