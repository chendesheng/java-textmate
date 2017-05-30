package me.textmate.rule;

import me.oniguruma.IOnigCaptureIndex;
import me.textmate.types.ILocation;

public class BeginWhileRule extends Rule {
  private final RegExpSource _begin;
  public final CaptureRule[] beginCaptures;
  public final CaptureRule[] whileCaptures;
  private final RegExpSource _while;
  public final boolean whileHasBackReferences;
  public final boolean hasMissingPatterns;
  public final int[] patterns;
  private RegExpSourceList _cachedCompiledPatterns;
  private RegExpSourceList _cachedCompiledWhilePatterns;

  public BeginWhileRule(ILocation location, int id, String name, String contentName, String begin,
      CaptureRule[] beginCaptures, String _while, CaptureRule[] whileCaptures, ICompilePatternsResult patterns) {
    super(location, id, name, contentName);
    this._begin = new RegExpSource(begin, this.id);
    this.beginCaptures = beginCaptures;
    this.whileCaptures = whileCaptures;
    this._while = new RegExpSource(_while, -2);
    this.whileHasBackReferences = this._while.hasBackReferences;
    this.patterns = patterns.patterns;
    this.hasMissingPatterns = patterns.hasMissingPatterns;
    this._cachedCompiledPatterns = null;
    this._cachedCompiledWhilePatterns = null;
  }

  public String getWhileWithResolvedBackReferences(String lineText, IOnigCaptureIndex[] captureIndices) {
    return this._while.resolveBackReferences(lineText, captureIndices);
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
    this._precompile(grammar);
    return this._cachedCompiledPatterns.compile(grammar, allowA, allowG);
  }

  private void _precompile(IRuleRegistry grammar) {
    if (this._cachedCompiledPatterns == null) {
      this._cachedCompiledPatterns = new RegExpSourceList();
      this.collectPatternsRecursive(grammar, this._cachedCompiledPatterns, true);
    }
  }

  public ICompiledRule compileWhile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG) {
    this._precompileWhile(grammar);
    if (this._while.hasBackReferences) {
      this._cachedCompiledWhilePatterns.setSource(0, endRegexSource);
    }
    return this._cachedCompiledWhilePatterns.compile(grammar, allowA, allowG);
  }

  private void _precompileWhile(IRuleRegistry grammar) {
    if (this._cachedCompiledWhilePatterns == null) {
      this._cachedCompiledWhilePatterns = new RegExpSourceList();
      this._cachedCompiledWhilePatterns.push(this._while.hasBackReferences ? this._while.clone() : this._while);
    }
  }
}