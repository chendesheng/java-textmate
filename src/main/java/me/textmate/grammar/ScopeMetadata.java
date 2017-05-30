package me.textmate.grammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.textmate.main.IEmbeddedLanguagesMap;
import me.textmate.main.StandardTokenType;

import me.textmate.theme.ThemeTrieElementRule;

public class ScopeMetadata {
  public final String scopeName;
  public final int languageId;
  public final int tokenType;
  public final ThemeTrieElementRule[] themeData;

  public ScopeMetadata(String scopeName, int languageId, int tokenType, ThemeTrieElementRule[] themeData) {
    this.scopeName = scopeName;
    this.languageId = languageId;
    this.tokenType = tokenType;
    this.themeData = themeData;
  }
}

class ScopeMetadataProvider {

  private final int _initialLanguage;
  private final IThemeProvider _themeProvider;
  private Map<String, ScopeMetadata> _cache;
  private ScopeMetadata _defaultMetaData;
  private final IEmbeddedLanguagesMap _embeddedLanguages;
  private final Pattern _embeddedLanguagesRegex;

  public ScopeMetadataProvider(int initialLanguage, IThemeProvider themeProvider,
      IEmbeddedLanguagesMap embeddedLanguages) {
    this._initialLanguage = initialLanguage;
    this._themeProvider = themeProvider;
    this.onDidChangeTheme();

    // embeddedLanguages handling
    this._embeddedLanguages = new IEmbeddedLanguagesMap();

    if (embeddedLanguages != null) {
      // If embeddedLanguages are configured, fill in `this._embeddedLanguages`
      for (String scope : embeddedLanguages.keySet().toArray(new String[0])) {
        Integer language = embeddedLanguages.get(scope);
        if (language == 0) {
          System.out.println("Invalid embedded language found at scope " + scope + ": <<" + language + ">>");
          // never hurts to be too careful
          continue;
        }
        this._embeddedLanguages.put(scope, language);
      }
    }

    // create the regex
    String[] escapedScopes = this._embeddedLanguages.keySet().toArray(new String[0]);
    for (int i = 0; i < escapedScopes.length; i++) {
      escapedScopes[i] = Pattern.quote(escapedScopes[i]);
    }

    if (escapedScopes.length == 0) {
      // no scopes registered
      this._embeddedLanguagesRegex = null;
    } else {
      Arrays.sort(escapedScopes);
      List<String> listScopes = Arrays.asList(escapedScopes);
      Collections.reverse(listScopes);
      this._embeddedLanguagesRegex = Pattern.compile("^((" + String.join(")|(", listScopes) + "))($|\\.)");
    }
  }

  public void onDidChangeTheme() {
    this._cache = new HashMap<String, ScopeMetadata>();
    this._defaultMetaData = new ScopeMetadata("", this._initialLanguage, StandardTokenType.Other,
        new ThemeTrieElementRule[] { this._themeProvider.getDefaults() });
  }

  public ScopeMetadata getDefaultMetadata() {
    return this._defaultMetaData;
  }

  /**
   * Escapes regular expression characters in a given string
   */
  private static ScopeMetadata _NULL_SCOPE_METADATA = new ScopeMetadata("", 0, 0, null);

  public ScopeMetadata getMetadataForScope(String scopeName) {
    if (scopeName == null) {
      return ScopeMetadataProvider._NULL_SCOPE_METADATA;
    }
    ScopeMetadata value = this._cache.get(scopeName);
    if (value != null) {
      return value;
    }
    value = this._doGetMetadataForScope(scopeName);
    this._cache.put(scopeName, value);
    return value;
  }

  private ScopeMetadata _doGetMetadataForScope(String scopeName) {
    int languageId = this._scopeToLanguage(scopeName);
    int standardTokenType = ScopeMetadataProvider._toStandardTokenType(scopeName);
    ThemeTrieElementRule[] themeData = this._themeProvider.themeMatch(scopeName);

    return new ScopeMetadata(scopeName, languageId, standardTokenType, themeData);
  }

  /**
   * Given a produced TM scope, return the language that token describes or null if unknown.
   * e.g. source.html => html, source.css.embedded.html => css, punctuation.definition.tag.html => null
   */
  private int _scopeToLanguage(String scope) {
    if (scope == null) {
      return 0;
    }
    if (this._embeddedLanguagesRegex == null) {
      // no scopes registered
      return 0;
    }

    Matcher m = this._embeddedLanguagesRegex.matcher(scope);
    if (m == null) {
      // no scopes matched
      return 0;
    }

    Integer language = this._embeddedLanguages.get(m.group(1));
    if (language == null) {
      return 0;
    }

    return language;
  }

  private static Pattern STANDARD_TOKEN_TYPE_REGEXP = Pattern.compile("\\b(comment|string|regex)\\b");

  private static int _toStandardTokenType(String tokenType) {
    Matcher m = ScopeMetadataProvider.STANDARD_TOKEN_TYPE_REGEXP.matcher(tokenType);
    if (!m.find()) {
      return StandardTokenType.Other;
    }
    char g1 = m.group(1).charAt(0);
    if (g1 == 'c') {
      return StandardTokenType.Comment;
    } else if (g1 == 's') {
      return StandardTokenType.String;
    } else if (g1 == 'r') {
      return StandardTokenType.RegEx;
    }
    throw new Error("Unexpected match for standard token type!");
  }
}