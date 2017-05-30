package me.textmate.rule;

import me.oniguruma.IOnigCaptureIndex;
import me.textmate.types.ILocation;
import me.textmate.utils.RegexSource;

public abstract class Rule {
  public final ILocation location;
  public final int id;

  private final boolean _nameIsCapturing;
  private final String _name;

  private final boolean _contentNameIsCapturing;
  private final String _contentName;

  public Rule(ILocation location, int id, String name, String contentName) {
    this.location = location;
    this.id = id;
    this._name = name;
    this._nameIsCapturing = RegexSource.hasCaptures(this._name);
    this._contentName = contentName;
    this._contentNameIsCapturing = RegexSource.hasCaptures(this._contentName);
  }

  public String debugName() {
    // return `${(<any>this.constructor).name}#${this.id} @ ${path.basename(this.$location.filename)}:${this.$location.line}`;
    return this.getClass().toString()+"#" + this.id;
  }

  public String getName(String lineText, IOnigCaptureIndex[] captureIndices) {
    if (!this._nameIsCapturing) {
      return this._name;
    }
    return RegexSource.replaceCaptures(this._name, lineText, captureIndices);
  }

  public String getContentName(String lineText, IOnigCaptureIndex[] captureIndices) {
    if (!this._contentNameIsCapturing) {
      return this._contentName;
    }
    return RegexSource.replaceCaptures(this._contentName, lineText, captureIndices);
  }

  public abstract void collectPatternsRecursive(IRuleRegistry grammar, RegExpSourceList out, boolean isFirst);

  public abstract ICompiledRule compile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG);
}