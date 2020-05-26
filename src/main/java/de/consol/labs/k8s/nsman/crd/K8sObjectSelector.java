package de.consol.labs.k8s.nsman.crd;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class K8sObjectSelector {
  private String kind;
  private Map<String, String> labels;
}
