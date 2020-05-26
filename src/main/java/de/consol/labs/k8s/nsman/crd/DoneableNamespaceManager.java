package de.consol.labs.k8s.nsman.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableNamespaceManager
    extends CustomResourceDoneable<NamespaceManager> {
  public DoneableNamespaceManager(final NamespaceManager resource,
      final Function<NamespaceManager, NamespaceManager> function) {
    super(resource, function);
  }
}
