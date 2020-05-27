package de.consol.labs.k8s.nsman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EntryPoint {
  public static void main(final String[] args) {
    SpringApplication.run(EntryPoint.class, args);
  }
}
