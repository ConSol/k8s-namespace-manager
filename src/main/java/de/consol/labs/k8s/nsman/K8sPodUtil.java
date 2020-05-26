package de.consol.labs.k8s.nsman;

import java.util.Objects;

import io.fabric8.kubernetes.api.model.Pod;

public final class K8sPodUtil {
  private K8sPodUtil() {}

  public static boolean isSucceeded(final Pod pod) {
    if (Objects.isNull(pod) || Objects.isNull(pod.getStatus())
        || Objects.isNull(pod.getStatus().getPhase())) {
      return false;
    }
    return "succeeded".equals(pod.getStatus().getPhase().toLowerCase());
  }
}
