package com.flo.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flo.inventory.model.Stock;
import com.flo.inventory.repository.StockRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private StockRepository stockRepository;

  private static final Long PRODUCT_ID = 1L;

  @Test
  void getStock_returnsStock_whenExists() throws Exception {
    when(stockRepository.findById(PRODUCT_ID))
        .thenReturn(Optional.of(new Stock(PRODUCT_ID, 42, 0)));

    mockMvc
        .perform(get("/inventory/" + PRODUCT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stockCount").value(42))
        .andExpect(jsonPath("$.reservedCount").value(0));
  }

  @Test
  void getStock_returnsNotFound_whenDoesNotExist() throws Exception {
    when(stockRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

    mockMvc.perform(get("/inventory/" + PRODUCT_ID)).andExpect(status().isNotFound());
  }

  @Test
  void reserve_returnsUpdatedStock_whenAvailable() throws Exception {
    when(stockRepository.findById(PRODUCT_ID))
        .thenReturn(Optional.of(new Stock(PRODUCT_ID, 42, 0)));
    when(stockRepository.save(any(Stock.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/inventory/" + PRODUCT_ID + "/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservedCount").value(2));
  }

  @Test
  void reserve_returnsConflict_whenInsufficientStock() throws Exception {
    when(stockRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(new Stock(PRODUCT_ID, 0, 0)));

    mockMvc
        .perform(
            post("/inventory/" + PRODUCT_ID + "/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isConflict());
  }

  @Test
  void reserve_returnsBadRequest_whenQuantityIsNotPositive() throws Exception {
    mockMvc
        .perform(
            post("/inventory/" + PRODUCT_ID + "/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void release_returnsUpdatedStock_withReducedReservedCount() throws Exception {
    when(stockRepository.findById(PRODUCT_ID))
        .thenReturn(Optional.of(new Stock(PRODUCT_ID, 42, 2)));
    when(stockRepository.save(any(Stock.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/inventory/" + PRODUCT_ID + "/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservedCount").value(0));
  }
}
