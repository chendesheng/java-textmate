package me.textmate.rule;

import me.oniguruma.OnigScanner;

public class ICompiledRule {
  public OnigScanner scanner;
  public int[] rules;
  public String[] debugRegExps;
}