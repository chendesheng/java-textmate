package me.textmate.types;

import java.io.Serializable;

public class ILocation implements Serializable {
  static final long serialVersionUID = 1486812557291L;

  public String filename;
  public int line;
  public int character;
}