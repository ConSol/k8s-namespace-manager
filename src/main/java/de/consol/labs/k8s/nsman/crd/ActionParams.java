package de.consol.labs.k8s.nsman.crd;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ActionParams {
  // #region SCALE_DOWN
  private List<K8sObjectSelector> selectors;
  // #endregion
  // #region WEB_HOOK
  private String url;
  // #endregion
}
