package de.consol.labs.k8s.nsman.task;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import de.consol.labs.k8s.nsman.K8sMetadataUtil;
import de.consol.labs.k8s.nsman.crd.K8sObjectSelector;
import de.consol.labs.k8s.nsman.crd.NamespaceManager;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NamespaceUtil {
  private final KubernetesClient k8sClient;

  public Namespace getManagedNamespace(final NamespaceManager manager) {
    final String name = Objects.nonNull(manager.getSpec().getNamespace())
        ? manager.getSpec().getNamespace()
        : K8sMetadataUtil.getName(manager);
    log.debug("name = {}", name);
    final Namespace ns = k8sClient.namespaces().withName(name).get();
    if (Objects.isNull(ns)) {
      log.warn("could not find namespace \"{}\" for {}", name, manager);
    }
    return ns;
  }

  public void scaleDown(final Namespace ns,
      final List<K8sObjectSelector> selectors) {
    final String nsName = K8sMetadataUtil.getName(ns);
    if (Objects.isNull(selectors) || selectors.isEmpty()) {
      log.info("no selectors. scaling everything down");
      scaleDownDeployments(nsName, null);
      scaleDownStatefulSets(nsName, null);
      return;
    }
    for (final K8sObjectSelector s : selectors) {
      if (Objects.isNull(s.getKind()) || s.getKind().isBlank()) {
        log.error("no kind in {}", s);
        continue;
      }
      switch (s.getKind().toLowerCase()) {
        case "deployment":
          scaleDownDeployments(nsName, s.getLabels());
          break;
        case "statefulset":
          scaleDownStatefulSets(nsName, s.getLabels());
          break;
        default:
          log.error("unknown kind in {}", s);
      }
    }
  }

  private void scaleDownDeployments(final String namespace,
      final Map<String, String> labels) {
    scaleDownK8sResources(() -> k8sClient.apps().deployments(), namespace,
        labels);
  }

  private void scaleDownStatefulSets(final String namespace,
      final Map<String, String> labels) {
    scaleDownK8sResources(() -> k8sClient.apps().statefulSets(), namespace,
        labels);
  }

  private static <T extends HasMetadata> void scaleDownK8sResources(
      final Supplier<MixedOperation<T, ? extends KubernetesResourceList<T>, ? extends Doneable<T>, ? extends RollableScalableResource<T, ? extends Doneable<T>>>> specializedK8sClientFactory,
      final String namespace, final Map<String, String> labels) {
    final KubernetesResourceList<T> list;
    if (Objects.nonNull(labels) && !labels.isEmpty()) {
      list = specializedK8sClientFactory.get().inNamespace(namespace)
          .withLabels(labels).list();
    } else {
      list = specializedK8sClientFactory.get().inNamespace(namespace).list();
    }
    if (Objects.nonNull(list) && Objects.nonNull(list.getItems())) {
      list.getItems().stream().forEach(obj -> {
        specializedK8sClientFactory.get().inNamespace(namespace)
            .withName(K8sMetadataUtil.getName(obj)).scale(0);
        log.info("scaled down {}", K8sMetadataUtil.format(obj));
      });
    }
  }
}
