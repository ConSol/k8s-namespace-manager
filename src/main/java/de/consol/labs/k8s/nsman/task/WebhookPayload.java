package de.consol.labs.k8s.nsman.task;

import de.consol.labs.k8s.nsman.crd.Policy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class WebhookPayload {
  @lombok.NonNull
  private final NamespaceManagerIdentifier namespaceManager;
  @lombok.NonNull
  private final Policy policy;
  @lombok.NonNull
  private final String namespace;
}
