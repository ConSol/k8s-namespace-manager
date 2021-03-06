package de.consol.labs.k8s.nsman.task;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import de.consol.labs.k8s.nsman.K8sAnnotation;
import de.consol.labs.k8s.nsman.K8sMetadataUtil;
import de.consol.labs.k8s.nsman.K8sPodUtil;
import de.consol.labs.k8s.nsman.NamespaceManagersQueueUtil;
import de.consol.labs.k8s.nsman.crd.ActionParams;
import de.consol.labs.k8s.nsman.crd.Condition;
import de.consol.labs.k8s.nsman.crd.ConditionParams;
import de.consol.labs.k8s.nsman.crd.DoneableNamespaceManager;
import de.consol.labs.k8s.nsman.crd.K8sObjectSelector;
import de.consol.labs.k8s.nsman.crd.NamespaceManager;
import de.consol.labs.k8s.nsman.crd.NamespaceManagerList;
import de.consol.labs.k8s.nsman.crd.NamespaceManagerSpec;
import de.consol.labs.k8s.nsman.crd.Policy;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class CheckNamespaceManagerTask implements Runnable {
  private final NamespaceManagersQueueUtil namespaceManagersQueueUtil;
  private final MixedOperation<NamespaceManager, NamespaceManagerList, DoneableNamespaceManager, Resource<NamespaceManager, DoneableNamespaceManager>> k8sNamespaceManagerClient;
  private final KubernetesClient k8sClient;
  private final NamespaceUtil namespaceUtil;
  private final WebClient webClient;

  @Setter
  @Getter
  private NamespaceManagerIdentifier namespaceManagerIdentifier;

  @Override
  public void run() {
    if (Objects.isNull(namespaceManagerIdentifier)) {
      log.error("namespace manager is not specified");
      return;
    }
    final NamespaceManager manager = k8sNamespaceManagerClient
        .inNamespace(namespaceManagerIdentifier.getNamespace())
        .withName(namespaceManagerIdentifier.getName()).get();
    if (Objects.isNull(manager)) {
      log.warn("manager has been deleted. manager id = {}",
          namespaceManagerIdentifier);
      return;
    }
    if (NamespaceManager.isDeactivated(manager)) {
      return;
    }
    log.info("checking {}", K8sMetadataUtil.format(manager));
    try {
      check(manager);
      updateAnnotations();
    } catch (final RuntimeException e) {
      log.error(
          "failure during task execution but show must go on: manager will be enqueued",
          e);
    }
    log.info("enqueue {} for scheduling a check task later",
        K8sMetadataUtil.format(manager));
    namespaceManagersQueueUtil.enqueue(manager);
  }

  private void check(final NamespaceManager manager) {
    if (Objects.isNull(manager.getSpec())
        || Objects.isNull(manager.getSpec().getPolicies())
        || manager.getSpec().getPolicies().isEmpty()) {
      log.warn("no policies: {}", manager);
      return;
    }
    final Namespace ns = namespaceUtil.getManagedNamespace(manager);
    if (Objects.isNull(ns) || NamespaceUtil.isTerminating(ns)) {
      return;
    }
    final Map<Policy, Boolean> policyEvaluationResults =
        manager.getSpec().getPolicies().stream().collect(Collectors
            .toMap(Function.identity(), p -> isApplyAction(manager, ns, p)));
    policyEvaluationResults.forEach((p, isApply) -> log
        .debug("policy = {}   isApplyAction = {}", p, isApply));
    policyEvaluationResults.entrySet().stream().filter(kv -> kv.getValue())
        .forEach(kv -> tryApplyAction(ns, kv.getKey()));
  }

  private boolean isApplyAction(final NamespaceManager manager,
      final Namespace ns, final Policy policy) {
    if (Objects.isNull(policy) || Objects.isNull(policy.getCondition())
        || Objects.isNull(policy.getCondition().getType())) {
      log.error("{} in {} does not have condition", policy, manager);
      return false;
    }
    final Condition condition = policy.getCondition();
    log.debug("condition = {}", condition);
    final String nsName = K8sMetadataUtil.getName(ns);
    log.debug("nsName = {}", nsName);
    final Instant nsCreationTs = K8sMetadataUtil.getCreationTimestamp(ns);
    log.debug("nsCreationTs = {}", nsCreationTs);
    final ConditionParams params = condition.getParams();
    switch (condition.getType()) {
      case TTL:
        final Duration ttl = ConditionParams.getTtl(params);
        log.debug("ttl = {} sec", ttl.toSeconds());
        return nsCreationTs.plus(ttl).compareTo(Instant.now()) <= 0;
      case PODS_SUCCEED:
        final Duration initialDelay = ConditionParams.getInitialDelay(params);
        if (nsCreationTs.plus(initialDelay).compareTo(Instant.now()) > 0) {
          log.debug("initial delay has not elapsed yet");
          return false;
        }
        final PodList pods;
        if (Objects.nonNull(condition.getParams())
            && Objects.nonNull(condition.getParams().getPodLabels())
            && !condition.getParams().getPodLabels().isEmpty()) {
          pods = k8sClient.pods().inNamespace(nsName)
              .withLabels(condition.getParams().getPodLabels()).list();
        } else {
          pods = k8sClient.pods().inNamespace(nsName).list();
        }
        if (Objects.isNull(pods) || Objects.isNull(pods.getItems())) {
          log.debug("no pods");
          return true;
        }
        log.debug("found {} pods", pods.getItems().size());
        log.debug("pods: {}", pods.getItems().stream()
            .map(K8sMetadataUtil::format).collect(Collectors.joining(", ")));
        return pods.getItems().stream().allMatch(K8sPodUtil::isSucceeded);
      default:
        log.warn("unknown condition type in {}", condition);
        return false;
    }
  }

  private boolean tryApplyAction(final Namespace ns, final Policy policy) {
    try {
      final boolean result = applyAction(ns, policy);
      if (result) {
        deactivateManager();
      }
      return result;
    } catch (final RuntimeException e) {
      log.error("failed to apply action", e);
      return false;
    }
  }

  private boolean applyAction(final Namespace ns, final Policy policy) {
    if (Objects.isNull(policy) || Objects.isNull(policy.getAction())
        || Objects.isNull(policy.getAction().getType())) {
      log.error("action is not properly defined");
      return false;
    }
    log.debug("action = {}", policy.getAction());
    final String nsName = K8sMetadataUtil.getName(ns);
    final ActionParams params = policy.getAction().getParams();
    switch (policy.getAction().getType()) {
      case DELETE:
        k8sClient.namespaces().withName(nsName).delete();
        log.info("deleted namespace \"{}\"", nsName);
        return true;
      case SCALE_DOWN:
        final List<K8sObjectSelector> selectors =
            Objects.nonNull(params) ? params.getSelectors() : null;
        namespaceUtil.scaleDown(ns, selectors);
        log.info("scaled down \"{}\" namespace", nsName);
        return true;
      case WEBHOOK:
        if (Objects.nonNull(params) && Objects.nonNull(params.getUrl())) {
          final String url = params.getUrl();
          log.info("HTTP POST {}", url);
          final WebhookPayload payload =
              new WebhookPayload(namespaceManagerIdentifier, policy, nsName);
          final ClientResponse response = webClient.post().uri(params.getUrl())
              .contentType(MediaType.APPLICATION_JSON).bodyValue(payload)
              .exchange().block();
          log.info("response status code = {}", response.statusCode());
          return response.statusCode().is2xxSuccessful();
        }
        log.error("URL for webhook is not defined");
        return false;
      default:
        log.error("unknown action in {}", policy);
        return false;
    }
  }

  private void deactivateManager() {
    final NamespaceManager manager = k8sNamespaceManagerClient
        .inNamespace(namespaceManagerIdentifier.getNamespace())
        .withName(namespaceManagerIdentifier.getName()).get();
    if (Objects.isNull(manager)) {
      log.warn("manager has been deleted. manager id = {}",
          namespaceManagerIdentifier);
      return;
    }
    if (Objects.isNull(manager.getSpec())) {
      manager.setSpec(new NamespaceManagerSpec());
    }
    manager.getSpec().setDeactivated(true);
    k8sNamespaceManagerClient
        .inNamespace(namespaceManagerIdentifier.getNamespace())
        .withName(namespaceManagerIdentifier.getName()).patch(manager);
    log.info("deactivated {}", K8sMetadataUtil.format(manager));
  }

  private void updateAnnotations() {
    final NamespaceManager manager = k8sNamespaceManagerClient
        .inNamespace(namespaceManagerIdentifier.getNamespace())
        .withName(namespaceManagerIdentifier.getName()).get();
    if (Objects.isNull(manager)) {
      log.warn("manager has been deleted. manager id = {}",
          namespaceManagerIdentifier);
      return;
    }
    if (Objects.isNull(manager.getMetadata().getAnnotations())) {
      manager.getMetadata().setAnnotations(new HashMap<>());
    }
    final Instant now = Instant.now();
    final Map<String, String> annotations =
        manager.getMetadata().getAnnotations();
    if (Objects.isNull(K8sMetadataUtil.getFirstCheckTimestamp(manager))) {
      annotations.put(K8sAnnotation.FIRST_CHECK_TIMESTAMP, now.toString());
    }
    annotations.put(K8sAnnotation.LAST_CHECK_TIMESTAMP, now.toString());
    k8sNamespaceManagerClient
        .inNamespace(namespaceManagerIdentifier.getNamespace())
        .withName(namespaceManagerIdentifier.getName()).patch(manager);
    log.debug("updated annotations");
  }
}
