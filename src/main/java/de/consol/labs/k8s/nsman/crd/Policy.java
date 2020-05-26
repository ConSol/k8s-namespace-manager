package de.consol.labs.k8s.nsman.crd;

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
public class Policy {
  private String name;
  private Condition condition;
  private Action action;
}
