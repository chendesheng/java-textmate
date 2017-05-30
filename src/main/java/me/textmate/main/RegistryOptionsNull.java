package me.textmate.main;

public class RegistryOptionsNull extends RegistryOptions {

  public String getFilePath(String scopeName) {
    return null;
  }

  public String[] getInjections(String scopeName) {
    return null;
  }
}