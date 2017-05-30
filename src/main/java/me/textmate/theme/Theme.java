package me.textmate.theme;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import me.textmate.main.IRawTheme;
import me.textmate.main.IRawThemeSetting;

public class Theme {
  public static Theme createFromRawTheme(IRawTheme source) {
    return createFromParsedTheme(parseTheme(source));
  }

  public static Theme createFromParsedTheme(ParsedThemeRule[] source) {
    return resolveParsedThemeRules(source);
  }

  private final ColorMap _colorMap;
  private final ThemeTrieElement _root;
  private final ThemeTrieElementRule _defaults;
  private Map<String, ThemeTrieElementRule[]> _cache;

  public Theme(ColorMap colorMap, ThemeTrieElementRule defaults, ThemeTrieElement root) {
    this._colorMap = colorMap;
    this._root = root;
    this._defaults = defaults;
    this._cache = new HashMap<String, ThemeTrieElementRule[]>();
  }

  public String[] getColorMap() {
    return this._colorMap.getColorMap();
  }

  public ThemeTrieElementRule getDefaults() {
    return this._defaults;
  }

  public ThemeTrieElementRule[] match(String scopeName) {
    if (!this._cache.containsKey(scopeName)) {
      this._cache.put(scopeName, this._root.match(scopeName));
    }
    return this._cache.get(scopeName);
  }

  public static ParsedThemeRule[] parseTheme(IRawTheme source) {
    if (source == null) {
      return new ParsedThemeRule[0];
    }
    if (source.settings == null) {
      return new ParsedThemeRule[0];
    }
    IRawThemeSetting[] settings = source.settings;
    List<ParsedThemeRule> result = new ArrayList<ParsedThemeRule>();
    for (int i = 0, len = settings.length; i < len; i++) {
      IRawThemeSetting entry = settings[i];

      if (entry.settings == null) {
        continue;
      }

      String[] scopes = entry.scope;
      if (scopes == null) {
        scopes = new String[] { "" };
      }

      int fontStyle = FontStyle.NotSet;
      if (entry.settings.fontStyle == null) {
        fontStyle = FontStyle.None;

        String[] segments = entry.settings.fontStyle.split(" ");
        for (int j = 0, lenJ = segments.length; j < lenJ; j++) {
          String segment = segments[j];
          if (segment.equals("italic"))
            fontStyle = fontStyle | FontStyle.Italic;
          else if (segment.equals("bold"))
            fontStyle = fontStyle | FontStyle.Bold;
          else if (segment.equals("underline"))
            fontStyle = fontStyle | FontStyle.Underline;
        }
      }

      String foreground = null;
      if (entry.settings.foreground != null) {
        foreground = entry.settings.foreground;
      }

      String background = null;
      if (entry.settings.background != null) {
        background = entry.settings.background;
      }

      for (int j = 0, lenJ = scopes.length; j < lenJ; j++) {
        String _scope = scopes[j].trim();

        String[] segments = _scope.split(" ");

        String scope = segments[segments.length - 1];
        List<String> parentScopes = null;
        if (segments.length > 1) {
          parentScopes = Arrays.asList(segments);
          parentScopes.remove(parentScopes.size() - 1);
          Collections.reverse(parentScopes);
        }

        result
            .add(new ParsedThemeRule(scope, parentScopes.toArray(new String[0]), i, fontStyle, foreground, background));
      }
    }

    return result.toArray(new ParsedThemeRule[0]);
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

  /**
   * Resolve rules (i.e. inheritance).
   */
  static Theme resolveParsedThemeRules(ParsedThemeRule[] parsedThemeRules) {

    // Sort rules lexicographically, and then by index if necessary
    Arrays.sort(parsedThemeRules, new Comparator<ParsedThemeRule>() {
      public int compare(ParsedThemeRule a, ParsedThemeRule b) {
        int r = a.scope.compareTo(b.scope);
        if (r != 0) {
          return r;
        }
        r = strArrCmp(a.parentScopes, b.parentScopes);
        if (r != 0) {
          return r;
        }
        return a.index - b.index;
      }
    });

    List<ParsedThemeRule> parsedThemeRuleList = Arrays.asList(parsedThemeRules);

    // Determine defaults
    int defaultFontStyle = FontStyle.None;
    String defaultForeground = "#000000";
    String defaultBackground = "#ffffff";
    while (parsedThemeRuleList.size() >= 1 && parsedThemeRuleList.get(0).scope.isEmpty()) {
      ParsedThemeRule incomingDefaults = parsedThemeRuleList.remove(0);
      if (incomingDefaults.fontStyle != FontStyle.NotSet) {
        defaultFontStyle = incomingDefaults.fontStyle;
      }
      if (incomingDefaults.foreground != null) {
        defaultForeground = incomingDefaults.foreground;
      }
      if (incomingDefaults.background != null) {
        defaultBackground = incomingDefaults.background;
      }
    }
    ColorMap colorMap = new ColorMap();
    ThemeTrieElementRule defaults = new ThemeTrieElementRule(0, null, defaultFontStyle,
        colorMap.getId(defaultForeground), colorMap.getId(defaultBackground));

    ThemeTrieElement root = new ThemeTrieElement(new ThemeTrieElementRule(0, null, FontStyle.NotSet, 0, 0),
        new ThemeTrieElementRule[0]);
    for (ParsedThemeRule rule : parsedThemeRuleList) {
      root.insert(0, rule.scope, rule.parentScopes, rule.fontStyle, colorMap.getId(rule.foreground),
          colorMap.getId(rule.background));
    }

    return new Theme(colorMap, defaults, root);
  }

}