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
    final Namespace ns = namespaceUtil.getManagedNamespace(manager);
    if (Objects.isNull(ns) || NamespaceUtil.isTerminating(ns)) {
      return getDefaultScheduledAt();
    }
    final Map<Condition, Instant> conditions =
        manager.getSpec().getPolicies().stream().map(Policy::getCondition)
            .collect(Collectors.toMap(Function.identity(),
                c -> getScheduleAtForCondition(manager, c, ns)));
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
      final Condition condition, final Namespace ns) {
    if (Objects.isNull(condition)) {
      log.warn("condition is not provided");
      return null;
    }
    final Instant nsCreationTs = K8sMetadataUtil.getCreationTimestamp(ns);
    log.debug("nsCreationTs = {}", nsCreationTs);
    final Instant firstCheckTs =
        K8sMetadataUtil.getFirstCheckTimestamp(manager);
    log.debug("firstCheckTs = {}", firstCheckTs);
    final Instant lastCheckTs = K8sMetadataUtil.getLastCheckTimestamp(manager);
    log.debug("lastCheckTs = {}", lastCheckTs);
    final ConditionParams params = condition.getParams();
    log.debug("params = {}", params);
    switch (condition.getType()) {
      case TTL:
        final Duration ttl = ConditionParams.getTtl(params);
        log.debug("ttl = {} sec", ttl.toSeconds());
        return nsCreationTs.plus(ttl);
      case PODS_SUCCEED:
        final Duration initialDelay = ConditionParams.getInitialDelay(params);
        log.debug("initialDelay = {} sec", initialDelay.toSeconds());
        if (Objects.isNull(firstCheckTs)) {
          log.debug("1st check has not been done yet");
          return nsCreationTs.plus(initialDelay);
        }
        if (Duration.between(firstCheckTs, Instant.now())
            .compareTo(initialDelay) < 0) {
          log.debug("initial delay must be satisfied");
          return firstCheckTs.plus(initialDelay);
        }
        final Duration period = ConditionParams.getPeriod(params);
        log.debug("period = {} sec", period);
        if (Objects.nonNull(lastCheckTs)) {
          log.debug("using last check");
          return lastCheckTs.plus(period);
        }
        log.error("1st check is provided but the last check is not");
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
