package me.textmate.rule;

import me.textmate.types.ILocation;
import me.oniguruma.IOnigCaptureIndex;

public class BeginEndRule extends Rule {
  private final RegExpSource _begin;
  public final CaptureRule[] beginCaptures;
  private final RegExpSource _end;
  public final boolean endHasBackReferences;
  public final CaptureRule[] endCaptures;
  public final boolean applyEndPatternLast;
  public final boolean hasMissingPatterns;
  public final int[] patterns;

  private RegExpSourceList _cachedCompiledPatterns;

  public BeginEndRule(ILocation location, int id, String name, String contentName, String begin,
      CaptureRule[] beginCaptures, String end, CaptureRule[] endCaptures, boolean applyEndPatternLast,
      ICompilePatternsResult patterns) {
    super(location, id, name, contentName);
    this._begin = new RegExpSource(begin, this.id);
    this.beginCaptures = beginCaptures;
    this._end = new RegExpSource(end, -1);
    this.endHasBackReferences = this._end.hasBackReferences;
    this.endCaptures = endCaptures;
    this.applyEndPatternLast = applyEndPatternLast || false;
    this.patterns = patterns.patterns;
    this.hasMissingPatterns = patterns.hasMissingPatterns;
    this._cachedCompiledPatterns = null;
  }

  public String debugBeginRegExp() {
    return this._begin.source;
  }

  public String debugEndRegExp() {
    return this._end.source;
  }

  public String getEndWithResolvedBackReferences(String lineText, IOnigCaptureIndex[] captureIndices) {
    return this._end.resolveBackReferences(lineText, captureIndices);
  }

  public void collectPatternsRecursive(IRuleRegistry grammar, RegExpSourceList out, boolean isFirst) {
    if (isFirst) {
      int i;
      int len;
      Rule rule;

      for (i = 0, len = this.patterns.length; i < len; i++) {
        rule = grammar.getRule(this.patterns[i]);
        rule.collectPatternsRecursive(grammar, out, false);
      }
    } else {
      out.push(this._begin);
    }
  }

  public ICompiledRule compile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG) {
    RegExpSourceList precompiled = this._precompile(grammar);

    if (this._end.hasBackReferences) {
      if (this.applyEndPatternLast) {
        precompiled.setSource(precompiled.length() - 1, endRegexSource);
      } else {
        precompiled.setSource(0, endRegexSource);
      }
    }
    return this._cachedCompiledPatterns.compile(grammar, allowA, allowG);
  }

  private RegExpSourceList _precompile(IRuleRegistry grammar) {
    if (this._cachedCompiledPatterns == null) {
      this._cachedCompiledPatterns = new RegExpSourceList();

      this.collectPatternsRecursive(grammar, this._cachedCompiledPatterns, true);

      if (this.applyEndPatternLast) {
        this._cachedCompiledPatterns.push(this._end.hasBackReferences ? this._end.clone() : this._end);
      } else {
        this._cachedCompiledPatterns.unshift(this._end.hasBackReferences ? this._end.clone() : this._end);
      }
    }
    return this._cachedCompiledPatterns;
  }
}