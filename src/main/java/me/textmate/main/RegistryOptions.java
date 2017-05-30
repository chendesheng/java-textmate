package me.textmate.main;

/**
 * A registry helper that can locate grammar file paths given scope names.
 */
public abstract class RegistryOptions {
  public IRawTheme theme;

  public abstract String getFilePath(String scopeName);

  public abstract String[] getInjections(String scopeName);
}
