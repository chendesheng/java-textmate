package me.textmate.rule;

import java.util.function.Function;

public interface IRuleRegistry {
  Rule getRule(int patternId);

  Rule registerRule(Function<Integer, Rule> factory);
}