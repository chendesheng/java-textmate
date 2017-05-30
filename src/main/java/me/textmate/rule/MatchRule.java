package me.textmate.rule;

import me.textmate.types.ILocation;

public class MatchRule extends Rule {
  private final RegExpSource _match;
  public final CaptureRule[] captures;
  private RegExpSourceList _cachedCompiledPatterns;

  public MatchRule(ILocation location, int id, String name, String match, CaptureRule[] captures) {
    super(location, id, name, null);
    this._match = new RegExpSource(match, this.id);
    this.captures = captures;
    this._cachedCompiledPatterns = null;
  }

  public String debugMatchRegExp() {
    return this._match.source;
  }

  public void collectPatternsRecursive(IRuleRegistry grammar, RegExpSourceList out, boolean isFirst) {
    out.push(this._match);
  }

  public ICompiledRule compile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG) {
    if (this._cachedCompiledPatterns == null) {
      this._cachedCompiledPatterns = new RegExpSourceList();
      this.collectPatternsRecursive(grammar, this._cachedCompiledPatterns, true);
    }
    return this._cachedCompiledPatterns.compile(grammar, allowA, allowG);
  }
}
