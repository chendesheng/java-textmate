package me.textmate.types;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IRawGrammar implements Serializable {
  static final long serialVersionUID = 1486812514404L;

  @JsonIgnore
  public String uuid;
  public IRawRepository repository;
  public String scopeName;
  public IRawRule[] patterns;
  public Map<String, IRawRule> injections;
  public String injectionSelector;

  public String[] fileTypes;
  public String name;
  public String firstLineMatch;
  @JsonIgnore
  public String version;

  public String foldingStartMarker;
  public String foldingStopMarker;

  public String toString() {
    try {
      return (new ObjectMapper()).writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return super.toString();
    }
  }
}
