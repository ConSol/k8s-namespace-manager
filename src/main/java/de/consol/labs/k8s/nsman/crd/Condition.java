package de.consol.labs.k8s.nsman.crd;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@EqualsAndHashCode
public class Condition {
  private ConditionType type;
  private ConditionParams params;
}
