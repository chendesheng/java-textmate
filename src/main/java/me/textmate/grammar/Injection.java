package me.textmate.grammar;

import me.textmate.matcher.Matcher;
import me.textmate.types.IRawGrammar;

public class Injection {
  public Matcher<StackElement> matcher;
  public Boolean priorityMatch;
  public int ruleId;
  public IRawGrammar grammar;
}
