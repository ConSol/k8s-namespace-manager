package de.consol.labs.k8s.nsman;

public final class K8sAnnotation {
  public static final String PREFIX = "namespacemanagers.k8s.consol.de/";
  public static final String FIRST_CHECK_TIMESTAMP =
      PREFIX + "firstCheckTimestamp";
  public static final String LAST_CHECK_TIMESTAMP =
      PREFIX + "lastCheckTimestamp";

  private K8sAnnotation() {}
}
