package me.textmate.rule;

import me.textmate.types.ILocation;

public class CaptureRule extends Rule {

  public Integer retokenizeCapturedWithRuleId;

  public CaptureRule(ILocation location, int id, String name, String contentName,
      Integer retokenizeCapturedWithRuleId) {
    super(location, id, name, contentName);
    this.retokenizeCapturedWithRuleId = retokenizeCapturedWithRuleId;
  }

  public void collectPatternsRecursive(IRuleRegistry grammar, RegExpSourceList out, boolean isFirst) {
  }

  public ICompiledRule compile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG) {
    return null;
  }
}
