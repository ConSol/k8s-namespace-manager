package de.consol.labs.k8s.nsman.config;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import de.consol.labs.k8s.nsman.config.K8sNamespaceManagerConfig.K8sClient;
import de.consol.labs.k8s.nsman.config.K8sNamespaceManagerConfig.K8sNamespaceManagerProperties;
import de.consol.labs.k8s.nsman.crd.DoneableNamespaceManager;
import de.consol.labs.k8s.nsman.crd.NamespaceManager;
import de.consol.labs.k8s.nsman.crd.NamespaceManagerList;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class K8sConfig {
  private final K8sNamespaceManagerProperties namespaceManagerProperties;

  @Bean
  MixedOperation<NamespaceManager, NamespaceManagerList, DoneableNamespaceManager, Resource<NamespaceManager, DoneableNamespaceManager>> k8sNamespaceManagerClient(
      final KubernetesClient k8sClient) {
    final CustomResourceDefinition crd = new CustomResourceDefinitionBuilder()
        .withNewMetadata().withName("namespacemanagers.k8s.consol.de")
        .endMetadata().withNewSpec().withGroup("k8s.consol.de")
        .withVersion("v1beta1").withNewNames().withKind("NamespaceManager")
        .withPlural("namespacemanagers").endNames().withScope("Namespaced")
        .endSpec().build();
    return k8sClient.customResources(crd, NamespaceManager.class,
        NamespaceManagerList.class, DoneableNamespaceManager.class);
  }

  @Bean
  SharedInformerFactory k8sSharedInformerFactory(
      final KubernetesClient k8sClient,
      @Qualifier("k8sExecutorService") final ExecutorService executorService) {
    return k8sClient.informers(executorService);
  }

  @Bean
  ExecutorService k8sExecutorService() {
    final ThreadPoolTaskScheduler threadPoolTaskScheduler =
        new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(5);
    threadPoolTaskScheduler.setThreadNamePrefix("k8s-client-");
    threadPoolTaskScheduler.initialize();
    return new ExecutorServiceAdapter(threadPoolTaskScheduler);
  }

  @Bean
  KubernetesClient k8sClient(final Config config) {
    return new DefaultKubernetesClient(config);
  }

  @Bean
  Config k8sClientConfig() {
    final K8sClient clientSettings = namespaceManagerProperties.getK8sClient();
    if (clientSettings.isUseAutoconfig()) {
      final Config config = Config.autoConfigure(null);
      // do not scope operations to a namespace by default.
      // namespace scoping can be done explicitly for each operation
      // e.g. client.pods().inNamespace("my-ns").list();
      config.setNamespace(null);
      return config;
    }
    return new ConfigBuilder().withMasterUrl(clientSettings.getMasterUrl())
        .withCaCertFile(clientSettings.getCaCertFile())
        .withClientCertFile(clientSettings.getClientCertFile())
        .withClientKeyFile(clientSettings.getClientKeyFile())
        .withClientKeyPassphrase(clientSettings.getClientKeyPassphrase())
        .withClientKeyAlgo(clientSettings.getClientKeyAlgo()).build();
  }
}
