package com.flo.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.flo")
public class SearchApplication {

  public static void main(String[] args) {
    SpringApplication.run(SearchApplication.class, args);
  }
}
