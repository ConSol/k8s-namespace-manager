package de.consol.labs.k8s.nsman.crd;

import java.util.Objects;

import de.consol.labs.k8s.nsman.K8sMetadataUtil;
import io.fabric8.kubernetes.client.CustomResource;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@ToString
@Slf4j
@SuppressWarnings("serial")
public class NamespaceManager extends CustomResource {
  private NamespaceManagerSpec spec;

  public static boolean isDeactivated(final NamespaceManager manager) {
    final boolean result =
        Objects.nonNull(manager.getSpec()) && manager.getSpec().isDeactivated();
    if (result) {
      log.info("{} is deactivated", K8sMetadataUtil.format(manager));
    }
    return result;
  }
}
