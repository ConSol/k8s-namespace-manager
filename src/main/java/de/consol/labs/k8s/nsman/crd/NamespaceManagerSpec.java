package de.consol.labs.k8s.nsman.crd;

import java.util.List;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonDeserialize(using = JsonDeserializer.None.class)
@Setter
@Getter
@ToString
@EqualsAndHashCode
public class NamespaceManagerSpec {
  private boolean isDeactivated;
  private String namespace;
  private List<Policy> policies;
}
