package me.textmate.registry;

import java.util.HashMap;
import java.util.Map;

import me.textmate.grammar.Grammar;
import me.textmate.grammar.IScopeNameSet;
import me.textmate.theme.ThemeTrieElementRule;

import me.textmate.main.IEmbeddedLanguagesMap;
import me.textmate.main.IGrammar;
import me.textmate.theme.Theme;
import me.textmate.types.IRawGrammar;
import me.textmate.grammar.IGrammarRepositoryAndIThemeProvider;

public class SyncRegistry implements IGrammarRepositoryAndIThemeProvider {
  private final Map<String, Grammar> _grammars;
  private final Map<String, IRawGrammar> _rawGrammars;
  private final Map<String, String[]> _injectionGrammars;
  private Theme _theme;

  public SyncRegistry(Theme theme) {
    this._theme = theme;
    this._grammars = new HashMap<String, Grammar>();
    this._rawGrammars = new HashMap<String, IRawGrammar>();
    this._injectionGrammars = new HashMap<String, String[]>();
  }

  public void setTheme(Theme theme) {
    this._theme = theme;
    for (Grammar grammar : this._grammars.values()) {
      grammar.onDidChangeTheme();
    }
  }

  public String[] getColorMap() {
    return this._theme.getColorMap();
  }

  public String[] addGrammar(IRawGrammar grammar, String[] injectionScopeNames) {
    this._rawGrammars.put(grammar.scopeName, grammar);
    IScopeNameSet includedScopes = new IScopeNameSet();
    // collectIncludedScopes(includedScopes, grammar);
    if (injectionScopeNames != null) {
      this._injectionGrammars.put(grammar.scopeName, injectionScopeNames);
      for (String scopeName : injectionScopeNames) {
        includedScopes.put(scopeName, true);
      }
    }
    return null;
  }

  /**
   * Lookup a raw grammar.
   */
  public IRawGrammar lookup(String scopeName) {
    return this._rawGrammars.get(scopeName);
  }

  /**
   * Returns the injections for the given grammar
   */
  public String[] injections(String targetScope) {
    return this._injectionGrammars.get(targetScope);
  }

  /**
   * Get the default theme settings
   */
  public ThemeTrieElementRule getDefaults() {
    return this._theme.getDefaults();
  }

  /**
   * Match a scope in the theme.
   */
  public ThemeTrieElementRule[] themeMatch(String scopeName) {
    return this._theme.match(scopeName);
  }

  public IGrammar grammarForScopeName(String scopeName, int initialLanguage, IEmbeddedLanguagesMap embeddedLanguages) {
    if (!this._grammars.containsKey(scopeName)) {
      IRawGrammar rawGrammar = this._rawGrammars.get(scopeName);
      if (rawGrammar == null) {
        return null;
      }
      this._grammars.put(scopeName, new Grammar(rawGrammar, initialLanguage, embeddedLanguages, this));
    }

    return this._grammars.get(scopeName);
  }
}