package de.consol.labs.k8s.nsman.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class NamespaceManagerIdentifier {
  @lombok.NonNull
  private final String namespace;
  @lombok.NonNull
  private final String name;
}
