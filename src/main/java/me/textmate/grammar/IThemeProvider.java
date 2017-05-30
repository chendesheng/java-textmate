package me.textmate.grammar;

import me.textmate.theme.ThemeTrieElementRule;

public interface IThemeProvider {
  ThemeTrieElementRule[] themeMatch(String scopeName);

  ThemeTrieElementRule getDefaults();
}