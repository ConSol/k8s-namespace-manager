package de.consol.labs.k8s.nsman.crd;

import io.fabric8.kubernetes.client.CustomResource;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@SuppressWarnings("serial")
public class NamespaceManager extends CustomResource {
  private NamespaceManagerSpec spec;
}
