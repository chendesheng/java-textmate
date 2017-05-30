package me.textmate.grammarReader;

import me.textmate.types.IRawGrammar;

public interface IGrammarParser {
  IRawGrammar call(String contents, String filename);
}