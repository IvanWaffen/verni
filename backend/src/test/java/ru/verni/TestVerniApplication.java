package ru.verni;

import org.springframework.boot.SpringApplication;

/** Runs the app locally against a Testcontainers database. */
public class TestVerniApplication {

  public static void main(String[] args) {
    SpringApplication.from(VerniApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
