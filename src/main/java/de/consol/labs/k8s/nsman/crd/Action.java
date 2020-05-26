package de.consol.labs.k8s.nsman.crd;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Action {
  private ActionType type;
  private ActionParams params;
}
