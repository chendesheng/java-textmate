package me.textmate.rule;

import java.util.ArrayList;
import java.util.List;

import me.oniguruma.OnigScanner;

public class RegExpSourceList {
  private final List<RegExpSource> _items;
  private boolean _hasAnchors;
  private ICompiledRule _cached;
  private IRegExpSourceListAnchorCache _anchorCache = new IRegExpSourceListAnchorCache();
  // private final String[] _cachedSources;

  public RegExpSourceList() {
    this._items = new ArrayList<RegExpSource>();
    this._hasAnchors = false;
    this._cached = null;
    // this._cachedSources = null;

    this._anchorCache.A0_G0 = null;
    this._anchorCache.A0_G1 = null;
    this._anchorCache.A1_G0 = null;
    this._anchorCache.A1_G1 = null;
  }

  public void push(RegExpSource item) {
    this._items.add(item);
    this._hasAnchors = this._hasAnchors || item.hasAnchor;
  }

  public void unshift(RegExpSource item) {
    this._items.add(0, item);
    this._hasAnchors = this._hasAnchors || item.hasAnchor;
  }

  public int length() {
    return this._items.size();
  }

  public void setSource(int index, String newSource) {
    if (this._items.get(index).source != newSource) {
      // bust the cache
      this._cached = null;
      this._anchorCache.A0_G0 = null;
      this._anchorCache.A0_G1 = null;
      this._anchorCache.A1_G0 = null;
      this._anchorCache.A1_G1 = null;
      this._items.get(index).setSource(newSource);
    }
  }

  public ICompiledRule compile(IRuleRegistry grammar, boolean allowA, boolean allowG) {
    if (!this._hasAnchors) {
      if (this._cached == null) {
        String[] regExps = new String[this.length()];
        for (int i = 0; i < this.length(); i++) {
          regExps[i] = this._items.get(i).source;
        }
        this._cached = new ICompiledRule();
        this._cached.scanner = new OnigScanner(regExps);
        this._cached.rules = new int[this.length()];
        for (int i = 0; i < this.length(); i++) {
          this._cached.rules[i] = this._items.get(i).ruleId;
        }
        this._cached.debugRegExps = regExps;
      }
      return this._cached;
    } else {
      if (this._anchorCache.A0_G0 == null)
        this._anchorCache.A0_G0 = (allowA == false && allowG == false ? this._resolveAnchors(allowA, allowG) : null);
      if (this._anchorCache.A0_G1 == null)
        this._anchorCache.A0_G1 = (allowA == false && allowG == true ? this._resolveAnchors(allowA, allowG) : null);
      if (this._anchorCache.A1_G0 == null)
        this._anchorCache.A1_G0 = (allowA == true && allowG == false ? this._resolveAnchors(allowA, allowG) : null);
      if (this._anchorCache.A1_G1 == null)
        this._anchorCache.A1_G1 = (allowA == true && allowG == true ? this._resolveAnchors(allowA, allowG) : null);
      if (allowA) {
        if (allowG) {
          return this._anchorCache.A1_G1;
        } else {
          return this._anchorCache.A1_G0;
        }
      } else {
        if (allowG) {
          return this._anchorCache.A0_G1;
        } else {
          return this._anchorCache.A0_G0;
        }
      }
    }

  }

  private ICompiledRule _resolveAnchors(boolean allowA, boolean allowG) {
    String[] regExps = new String[this.length()];
    for (int i = 0; i < this.length(); i++) {
      regExps[i] = this._items.get(i).resolveAnchors(allowA, allowG);
    }

    ICompiledRule rule = new ICompiledRule();
    rule.scanner = new OnigScanner(regExps);
    rule.rules = new int[this.length()];
    for (int i = 0; i < this.length(); i++) {
      rule.rules[i] = this._items.get(i).ruleId;
    }
    rule.debugRegExps = regExps;
    return rule;
  }
}

class IRegExpSourceListAnchorCache {
  public ICompiledRule A0_G0;
  public ICompiledRule A0_G1;
  public ICompiledRule A1_G0;
  public ICompiledRule A1_G1;
}