package me.textmate.theme;

public class ParsedThemeRule {
  // _parsedThemeRuleBrand: void;

  public final String scope;
  public final String[] parentScopes;
  public final int index;

  /**
   * -1 if not set. An or mask of `FontStyle` otherwise.
   */
  public final int fontStyle;
  public final String foreground;
  public final String background;

  public ParsedThemeRule(String scope, String[] parentScopes, int index, int fontStyle, String foreground,
      String background) {
    this.scope = scope;
    this.parentScopes = parentScopes;
    this.index = index;
    this.fontStyle = fontStyle;
    this.foreground = foreground;
    this.background = background;
  }
}