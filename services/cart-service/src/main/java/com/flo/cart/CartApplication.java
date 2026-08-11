package com.flo.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.flo")
public class CartApplication {

  public static void main(String[] args) {
    SpringApplication.run(CartApplication.class, args);
  }
}
