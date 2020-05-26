package de.consol.labs.k8s.nsman.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import de.consol.labs.k8s.nsman.K8sMetadataUtil;
import de.consol.labs.k8s.nsman.crd.Condition;
import de.consol.labs.k8s.nsman.crd.ConditionParams;
import de.consol.labs.k8s.nsman.crd.DoneableNamespaceManager;
import de.consol.labs.k8s.nsman.crd.NamespaceManager;
import de.consol.labs.k8s.nsman.crd.NamespaceManagerList;
import de.consol.labs.k8s.nsman.crd.Policy;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulingUtil {
  private static final Duration DEFAULT_CHECK_INTERVAL = Duration.ofMinutes(5);

  private final NamespaceUtil namespaceUtil;
  private final MixedOperation<NamespaceManager, NamespaceManagerList, DoneableNamespaceManager, Resource<NamespaceManager, DoneableNamespaceManager>> k8sNamespaceManagerClient;

  public Instant getScheduleAt(final NamespaceManagerIdentifier id) {
    final NamespaceManager manager = k8sNamespaceManagerClient
        .inNamespace(id.getNamespace()).withName(id.getName()).get();
    if (Objects.isNull(manager)) {
      log.warn("manager has probably been deleted. manager identifier = {}",
          id);
      return null;
    }
    if (Objects.isNull(manager.getSpec())
        || Objects.isNull(manager.getSpec().getPolicies())) {
      log.warn("no policies. manager = {}", manager);
      return null;
    }
    final Map<Condition, Instant> conditions =
        manager.getSpec().getPolicies().stream().map(Policy::getCondition)
            .collect(Collectors.toMap(Function.identity(),
                c -> getScheduleAtForCondition(manager, c)));
    if (conditions.isEmpty()) {
      log.warn("no conditons. manager = {}", manager);
      return null;
    }
    conditions
        .forEach((k, v) -> log.debug("condition: {}   scheduleAt: {}", k, v));
    return conditions.values().stream().filter(Objects::nonNull)
        .min(Comparator.<Instant>naturalOrder()).orElse(null);
  }

  private Instant getScheduleAtForCondition(final NamespaceManager manager,
      final Condition condition) {
    if (Objects.isNull(condition)) {
      log.warn("condition is not provided");
      return null;
    }
    final Namespace ns = namespaceUtil.getManagedNamespace(manager);
    if (Objects.isNull(ns)) {
      return getDefaultScheduledAt();
    }
    final Instant nsCreationTs = K8sMetadataUtil.getCreationTimestamp(ns);
    final Instant firstCheckTs =
        K8sMetadataUtil.getFirstCheckTimestamp(manager);
    final Instant lastCheckTs = K8sMetadataUtil.getLastCheckTimestamp(manager);
    final ConditionParams params = condition.getParams();
    switch (condition.getType()) {
      case TTL:
        final Duration ttl = ConditionParams.getTtl(params);
        log.debug("ttl = {} sec", ttl.toSeconds());
        return nsCreationTs.plus(ttl);
      case PODS_SUCCEED:
        final Duration initialDelay = ConditionParams.getInitialDelay(params);
        if (Objects.isNull(firstCheckTs)) {
          return nsCreationTs.plus(initialDelay);
        }
        if (Duration.between(firstCheckTs, Instant.now())
            .compareTo(initialDelay) < 0) {
          return firstCheckTs.plus(initialDelay);
        }
        final Duration period = ConditionParams.getPeriod(params);
        log.debug("period = {} sec", period);
        if (Objects.nonNull(lastCheckTs)) {
          return lastCheckTs.plus(period);
        }
        return firstCheckTs.plus(period);
      default:
        log.warn("unknown condition type in {}", condition);
        return null;
    }
  }

  private static Instant getDefaultScheduledAt() {
    return Instant.now().plus(DEFAULT_CHECK_INTERVAL);
  }
}
