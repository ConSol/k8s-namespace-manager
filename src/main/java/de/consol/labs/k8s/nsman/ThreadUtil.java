package de.consol.labs.k8s.nsman;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ThreadUtil {
  private ThreadUtil() {}

  public static void logAndInterrupt(final InterruptedException e) {
    log.info("thread got interrupted", e);
    Thread.currentThread().interrupt();
  }
}
