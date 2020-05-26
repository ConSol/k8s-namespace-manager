package de.consol.labs.k8s.nsman;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import de.consol.labs.k8s.nsman.config.K8sNamespaceManagerConfig.K8sNamespaceManagerProperties;
import de.consol.labs.k8s.nsman.crd.NamespaceManager;
import de.consol.labs.k8s.nsman.crd.NamespaceManagerList;
import de.consol.labs.k8s.nsman.task.TaskManager;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class K8sNamespaceManager implements CommandLineRunner {
  private final SharedInformerFactory sharedInformerFactory;
  private final K8sNamespaceManagerProperties namespaceManagerProperties;
  private final NamespaceManagerEventHandler namespaceManagerEventHandler;
  private final ThreadPoolTaskScheduler taskScheduler;
  private final TaskManager taskManager;

  @Override
  public void run(final String... args) {
    final CustomResourceDefinitionContext crdContext =
        new CustomResourceDefinitionContext.Builder().withVersion("v1beta1")
            .withGroup("k8s.consol.de").withScope("Namespaced")
            .withPlural("namespacemanagers").build();
    final SharedIndexInformer<NamespaceManager> informer =
        sharedInformerFactory.sharedIndexInformerForCustomResource(crdContext,
            NamespaceManager.class, NamespaceManagerList.class,
            namespaceManagerProperties.getK8sInformer().getRecyncPeriodSec()
                .toMillis());
    informer.addEventHandler(namespaceManagerEventHandler);
    log.info("starting informers");
    taskScheduler.execute(sharedInformerFactory::startAllRegisteredInformers);
    log.info("informers have been started");
    while (!informer.hasSynced()) {
      log.info("waiting for informer to sync");
      try {
        Thread.sleep(1000);
      } catch (final InterruptedException e) {
        ThreadUtil.logAndInterrupt(e);
        return;
      }
    }
    log.info("informer has synced");
    taskScheduler.execute(taskManager);
  }
}
