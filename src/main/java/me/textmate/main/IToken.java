package me.textmate.main;

public class IToken {
  public int startIndex;
  public int endIndex;
  public String[] scopes;

  public IToken(int startIndex, int endIndex, String[] scopes) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.scopes = scopes;
  }
}