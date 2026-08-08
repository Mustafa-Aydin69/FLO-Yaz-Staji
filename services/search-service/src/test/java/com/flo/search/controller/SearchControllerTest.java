package com.flo.search.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flo.search.model.Product;
import com.flo.search.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ProductRepository productRepository;

  private static final Product NIKE =
      new Product(1L, "Air Runner X1", "Nike", "Spor Ayakkabı", 2899.90, 42);
  private static final Product ADIDAS =
      new Product(2L, "UltraBoost Flow", "Adidas", "Spor Ayakkabı", 3199.00, 15);

  @Test
  void search_returnsMatchingProducts_whenQueryMatchesName() throws Exception {
    when(productRepository.findAll()).thenReturn(List.of(NIKE, ADIDAS));

    mockMvc
        .perform(get("/search").param("q", "nike"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].brand").value("Nike"));
  }

  @Test
  void search_returnsEmptyList_whenNoProductMatches() throws Exception {
    when(productRepository.findAll()).thenReturn(List.of(NIKE, ADIDAS));

    mockMvc
        .perform(get("/search").param("q", "zzz-yok"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void search_returnsBadRequest_whenQueryIsBlank() throws Exception {
    mockMvc.perform(get("/search").param("q", "  ")).andExpect(status().isBadRequest());
  }

  @Test
  void getById_returnsProduct_whenIdExists() throws Exception {
    when(productRepository.findById(1L)).thenReturn(Optional.of(NIKE));

    mockMvc
        .perform(get("/products/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Air Runner X1"));
  }

  @Test
  void getById_returnsNotFound_whenIdDoesNotExist() throws Exception {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/products/999")).andExpect(status().isNotFound());
  }
}
