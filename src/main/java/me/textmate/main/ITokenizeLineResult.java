package me.textmate.main;

import me.textmate.grammar.StackElement;

public class ITokenizeLineResult {
  public IToken[] tokens;
  /**
   * The `prevState` to be passed on to the next line tokenization.
   */
  public StackElement ruleStack;
}