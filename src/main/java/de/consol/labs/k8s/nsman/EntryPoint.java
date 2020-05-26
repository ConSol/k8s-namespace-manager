package de.consol.labs.k8s.nsman;

import org.springframework.boot.SpringApplication;

public class EntryPoint {
  public static void main(final String[] args) {
    SpringApplication.run(K8sNamespaceManager.class, args);
  }
}
