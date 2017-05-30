package me.textmate.grammar;

public class LocalStackElement {
  public final ScopeListElement scopes;
  public final int endPos;

  public LocalStackElement(ScopeListElement scopes, int endPos) {
    this.scopes = scopes;
    this.endPos = endPos;
  }
}