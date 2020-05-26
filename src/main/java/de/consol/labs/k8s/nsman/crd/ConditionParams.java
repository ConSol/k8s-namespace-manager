package de.consol.labs.k8s.nsman.crd;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ConditionParams {
  // #region TTL
  private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
  // #endregion
  // #region PODS_SUCCEED
  private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(60);
  private static final Duration DEFAULT_PERIOD = Duration.ofSeconds(10);
  // #endregion

  // #region TTL
  private Long ttlSeconds;
  // #endregion
  // #region PODS_SUCCEED
  private Map<String, String> podLabels;
  private Long initialDelaySeconds;
  private Long periodSeconds;
  private Long minChecks;
  // #endregion

  public static Duration getTtl(final ConditionParams params) {
    if (Objects.nonNull(params) && Objects.nonNull(params.getTtlSeconds())) {
      return Duration.ofSeconds(params.getTtlSeconds());
    }
    return DEFAULT_TTL;
  }

  public static Duration getInitialDelay(final ConditionParams params) {
    if (Objects.nonNull(params)
        && Objects.nonNull(params.getInitialDelaySeconds())) {
      return Duration.ofSeconds(params.getInitialDelaySeconds());
    }
    return DEFAULT_INITIAL_DELAY;
  }

  public static Duration getPeriod(final ConditionParams params) {
    if (Objects.nonNull(params) && Objects.nonNull(params.getPeriodSeconds())) {
      return Duration.ofSeconds(params.getPeriodSeconds());
    }
    return DEFAULT_PERIOD;
  }
}
