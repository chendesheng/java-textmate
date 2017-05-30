package me.textmate.grammar;

import me.textmate.types.IRawGrammar;

public interface IGrammarRepository {
  IRawGrammar lookup(String scopeName);

  String[] injections(String scopeName);
}