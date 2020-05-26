package de.consol.labs.k8s.nsman;

import org.springframework.stereotype.Component;

import de.consol.labs.k8s.nsman.crd.NamespaceManager;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NamespaceManagerEventHandler
    implements ResourceEventHandler<NamespaceManager> {
  private final NamespaceManagersQueueUtil namespaceManagersQueueUtil;

  @Override
  public void onAdd(final NamespaceManager obj) {
    log.info("new:\n{}", obj);
    namespaceManagersQueueUtil.enqueue(obj);
  }

  @Override
  public void onUpdate(final NamespaceManager oldObj,
      final NamespaceManager newObj) {
    log.info("update.\nold:\n{}\nnew:\n{}", oldObj, newObj);
    namespaceManagersQueueUtil.enqueue(newObj);
  }

  @Override
  public void onDelete(final NamespaceManager obj,
      final boolean deletedFinalStateUnknown) {
    log.info("deletion:\n{}\ndeletedFinalStateUnknown = {}", obj,
        deletedFinalStateUnknown);
  }
}
