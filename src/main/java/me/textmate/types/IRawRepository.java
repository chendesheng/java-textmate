package me.textmate.types;

import java.util.HashMap;

public class IRawRepository extends HashMap<String, IRawRule> {
  static final long serialVersionUID = 1486812490417L;
  public IRawRule self() {
    return this.get("$self");
  }
  public IRawRule self(IRawRule rule) {
    return this.put("$self", rule);
  }
  public IRawRule base() {
    return this.get("$base");
  }
  public IRawRule base(IRawRule rule) {
    return this.put("$base", rule);
  }
}