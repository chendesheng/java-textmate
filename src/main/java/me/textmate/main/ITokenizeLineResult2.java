package me.textmate.main;

import me.textmate.grammar.StackElement;

public class ITokenizeLineResult2 {
  /**
   * The tokens in binary format. Each token occupies two array indices. For token i:
   *  - at offset 2*i => startIndex
   *  - at offset 2*i + 1 => metadata
   *
   */
  public int[] tokens;

  /**
   * The `prevState` to be passed on to the next line tokenization.
   */
  public StackElement ruleStack;
}