package me.textmate.types;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IRawRule implements Serializable {
  static final long serialVersionUID = 1486812467936L;

  public ILocation vscodeTextmateLocation;
  public Integer id;
  public String include;
  public String name;
  public String contentName;
  public String match;
  public IRawCaptures captures;
  public String begin;
  public IRawCaptures beginCaptures;
  public String end;
  public IRawCaptures endCaptures;
  public String whileq;
  public IRawCaptures whileCaptures;
  public IRawRule[] patterns;
  public IRawRepository repository;
  public boolean applyEndPatternLast;
  @JsonIgnore
  public String comment;
  @Override
  public String toString() {
    try {
      return (new ObjectMapper()).writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return super.toString();
    }
  }
}
