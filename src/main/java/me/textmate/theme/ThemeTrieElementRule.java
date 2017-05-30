package me.textmate.theme;

public class ThemeTrieElementRule {
  // _themeTrieElementRuleBrand:void;

  public int scopeDepth;
  public String[] parentScopes;
  public int fontStyle;
  public int foreground;
  public int background;

  public ThemeTrieElementRule(int scopeDepth, String[] parentScopes, int fontStyle, int foreground, int background) {
    this.scopeDepth = scopeDepth;
    this.parentScopes = parentScopes;
    this.fontStyle = fontStyle;
    this.foreground = foreground;
    this.background = background;
  }

  public ThemeTrieElementRule clone() {
    return new ThemeTrieElementRule(this.scopeDepth, this.parentScopes, this.fontStyle, this.foreground,
        this.background);
  }

  public static ThemeTrieElementRule[] cloneArr(ThemeTrieElementRule[] arr) {
    ThemeTrieElementRule[] r = new ThemeTrieElementRule[arr.length];
    for (int i = 0, len = arr.length; i < len; i++) {
      r[i] = arr[i].clone();
    }
    return r;
  }

  public void acceptOverwrite(int scopeDepth, int fontStyle, int foreground, int background) {
    if (this.scopeDepth > scopeDepth) {
      // console.log('how did this happen?');
    } else {
      this.scopeDepth = scopeDepth;
    }

    // console.log('-> my depth: ' + this.scopeDepth + ', overwriting depth: ' + scopeDepth);

    if (fontStyle != FontStyle.NotSet) {
      this.fontStyle = fontStyle;
    }
    if (foreground != 0) {
      this.foreground = foreground;
    }
    if (background != 0) {
      this.background = background;
    }
  }
}