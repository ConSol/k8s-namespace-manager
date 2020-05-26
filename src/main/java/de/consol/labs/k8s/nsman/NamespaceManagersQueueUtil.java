package de.consol.labs.k8s.nsman;

import java.util.concurrent.BlockingQueue;

import org.springframework.stereotype.Component;

import de.consol.labs.k8s.nsman.crd.NamespaceManager;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NamespaceManagersQueueUtil {
  private final BlockingQueue<String> namespaceManagersQueue;

  public void enqueue(final NamespaceManager namespaceManager) {
    final String key = Cache.metaNamespaceKeyFunc(namespaceManager);
    try {
      namespaceManagersQueue.put(key);
      log.info("enqueued {}", key);
    } catch (final InterruptedException e) {
      ThreadUtil.logAndInterrupt(e);
      return;
    }
  }
}
