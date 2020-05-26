package de.consol.labs.k8s.nsman.crd;

import java.util.List;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesList;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonDeserialize(using = JsonDeserializer.None.class)
@Setter
@Getter
@ToString
@SuppressWarnings("serial")
public class NamespaceManagerSpec extends KubernetesList {
  private String namespace;
  private List<Policy> policies;
}
