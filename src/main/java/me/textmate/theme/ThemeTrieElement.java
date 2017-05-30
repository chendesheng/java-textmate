package me.textmate.theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ThemeTrieElement {
  // _themeTrieElementBrand: void;

  private final ThemeTrieElementRule _mainRule;
  private final List<ThemeTrieElementRule> _rulesWithParentScopes;
  private final ITrieChildrenMap _children;

  public ThemeTrieElement(ThemeTrieElementRule mainRule, ThemeTrieElementRule[] rulesWithParentScopes) {
    this._mainRule = mainRule;
    this._rulesWithParentScopes = Arrays.asList(rulesWithParentScopes);
    this._children = new ITrieChildrenMap();
  }

  public ThemeTrieElement(ThemeTrieElementRule mainRule, ThemeTrieElementRule[] rulesWithParentScopes,
      ITrieChildrenMap children) {
    this._mainRule = mainRule;
    this._rulesWithParentScopes = Arrays.asList(rulesWithParentScopes);
    this._children = children;
  }

  private static ThemeTrieElementRule[] _sortBySpecificity(ThemeTrieElementRule[] arr) {
    if (arr.length == 1) {
      return arr;
    }

    Arrays.sort(arr, new Comparator<ThemeTrieElementRule>() {
      public int compare(ThemeTrieElementRule a, ThemeTrieElementRule b) {
        if (a.scopeDepth == b.scopeDepth) {
          int aValue = a.parentScopes == null ? 0 : a.parentScopes.length;
          int bValue = b.parentScopes == null ? 0 : b.parentScopes.length;
          return bValue - aValue;
        }
        return b.scopeDepth - a.scopeDepth;
      }
    });

    return arr;
  }

  public ThemeTrieElementRule[] match(String scope) {
    if (scope.isEmpty()) {
      List<ThemeTrieElementRule> rules = Arrays
          .asList(this._rulesWithParentScopes.toArray(new ThemeTrieElementRule[0]));
      rules.add(0, this._mainRule);
      return ThemeTrieElement._sortBySpecificity(rules.toArray(new ThemeTrieElementRule[0]));
    }

    int dotIndex = scope.indexOf('.');
    String head;
    String tail;
    if (dotIndex == -1) {
      head = scope;
      tail = "";
    } else {
      head = scope.substring(0, dotIndex);
      tail = scope.substring(dotIndex + 1);
    }

    if (this._children.containsKey(head)) {
      return this._children.get(head).match(tail);
    }

    List<ThemeTrieElementRule> rules = new ArrayList<ThemeTrieElementRule>();
    rules.add(this._mainRule);
    rules.addAll(this._rulesWithParentScopes);
    return ThemeTrieElement._sortBySpecificity(rules.toArray(new ThemeTrieElementRule[0]));
    // return ThemeTrieElement._sortBySpecificity([].concat(this._mainRule).concat(this._rulesWithParentScopes));
  }

  public void insert(int scopeDepth, String scope, String[] parentScopes, int fontStyle, int foreground,
      int background) {
    if (scope.isEmpty()) {
      this._doInsertHere(scopeDepth, parentScopes, fontStyle, foreground, background);
      return;
    }

    int dotIndex = scope.indexOf('.');
    String head;
    String tail;
    if (dotIndex == -1) {
      head = scope;
      tail = "";
    } else {
      head = scope.substring(0, dotIndex);
      tail = scope.substring(dotIndex + 1);
    }

    ThemeTrieElement child;
    if (this._children.containsKey(head)) {
      child = this._children.get(head);
    } else {
      child = new ThemeTrieElement(this._mainRule.clone(),
          ThemeTrieElementRule.cloneArr(this._rulesWithParentScopes.toArray(new ThemeTrieElementRule[0])));
      this._children.put(head, child);
    }

    child.insert(scopeDepth + 1, tail, parentScopes, fontStyle, foreground, background);
  }

  static int strArrCmp(String[] a, String[] b) {
    if (a == null && b == null)
      return 0;
    if (a == null)
      return -1;
    if (b == null)
      return 1;
    int len1 = a.length;
    int len2 = b.length;
    if (len1 == len2) {
      for (int i = 0; i < len1; i++) {
        int res = a[i].compareTo(b[i]);
        if (res != 0)
          return res;
      }
      return 0;
    }
    return len1 - len2;
  }

  private void _doInsertHere(int scopeDepth, String[] parentScopes, int fontStyle, int foreground, int background) {

    if (parentScopes == null) {
      // Merge into the main rule
      this._mainRule.acceptOverwrite(scopeDepth, fontStyle, foreground, background);
      return;
    }

    // Try to merge into existing rule
    for (int i = 0, len = this._rulesWithParentScopes.size(); i < len; i++) {
      ThemeTrieElementRule rule = this._rulesWithParentScopes.get(i);

      if (strArrCmp(rule.parentScopes, parentScopes) == 0) {
        // bingo! => we get to merge this into an existing one
        rule.acceptOverwrite(scopeDepth, fontStyle, foreground, background);
        return;
      }
    }

    // Must add a new rule

    // Inherit from main rule
    if (fontStyle == FontStyle.NotSet) {
      fontStyle = this._mainRule.fontStyle;
    }
    if (foreground == 0) {
      foreground = this._mainRule.foreground;
    }
    if (background == 0) {
      background = this._mainRule.background;
    }

    this._rulesWithParentScopes
        .add(new ThemeTrieElementRule(scopeDepth, parentScopes, fontStyle, foreground, background));
  }
}