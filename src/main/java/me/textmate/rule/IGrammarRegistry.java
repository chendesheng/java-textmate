package me.textmate.rule;

import me.textmate.types.IRawGrammar;
import me.textmate.types.IRawRepository;

public interface IGrammarRegistry {
  IRawGrammar getExternalGrammar(String scopeName, IRawRepository repository);
}
