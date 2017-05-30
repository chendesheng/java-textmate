package me.textmate.main;

import java.util.HashMap;

/**
 * A map from scope name to a language id. Please do not use language id 0.
 */
public class IEmbeddedLanguagesMap extends HashMap<String, Integer> {
  private static final long serialVersionUID = 2L;

  public IEmbeddedLanguagesMap() {
    super();
  }
}