package de.consol.labs.k8s.nsman;

import java.time.Instant;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.HasMetadata;

public final class K8sMetadataUtil {
  private K8sMetadataUtil() {}

  public static String format(final HasMetadata obj) {
    final String ns = obj.getMetadata().getNamespace();
    final String formattedNs =
        Objects.nonNull(ns) && !ns.isBlank() ? ns : "<NA>";
    return String.format("%s[%s/%s]", obj.getKind(), formattedNs, getName(obj));
  }

  public static String getName(final HasMetadata obj) {
    return obj.getMetadata().getName();
  }

  public static Instant getCreationTimestamp(final HasMetadata obj) {
    return Instant.parse(obj.getMetadata().getCreationTimestamp());
  }

  public static Instant getFirstCheckTimestamp(final HasMetadata obj) {
    return parseInstant(obj, K8sAnnotation.FIRST_CHECK_TIMESTAMP);
  }

  public static Instant getLastCheckTimestamp(final HasMetadata obj) {
    return parseInstant(obj, K8sAnnotation.LAST_CHECK_TIMESTAMP);
  }

  private static Instant parseInstant(final HasMetadata obj,
      final String annotation) {
    final String s = getAnnotation(obj, annotation);
    if (Objects.isNull(s) || s.isBlank()) {
      return null;
    }
    return Instant.parse(s);
  }

  private static String getAnnotation(final HasMetadata obj,
      final String annotation) {
    if (Objects.isNull(obj) || Objects.isNull(obj.getMetadata())
        || Objects.isNull(obj.getMetadata().getAnnotations())) {
      return null;
    }
    return obj.getMetadata().getAnnotations().get(annotation);
  }
}
