package com.flo.inventory.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flo.inventory.model.Stock;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

@Repository
public class StockRepository {

  private final Map<Long, Stock> stocks;

  public StockRepository(ObjectMapper objectMapper) throws IOException {
    try (InputStream in = new ClassPathResource("stock.json").getInputStream()) {
      List<Stock> seed = objectMapper.readValue(in, new TypeReference<List<Stock>>() {});
      this.stocks =
          new ConcurrentHashMap<>(
              seed.stream().collect(Collectors.toMap(Stock::productId, s -> s)));
    }
  }

  public Stock save(Stock stock) {
    stocks.put(stock.productId(), stock);
    return stock;
  }

  public Optional<Stock> findById(Long productId) {
    return Optional.ofNullable(stocks.get(productId));
  }
}
