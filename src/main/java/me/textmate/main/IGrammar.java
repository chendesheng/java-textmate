package me.textmate.main;

import me.textmate.grammar.StackElement;

public interface IGrammar {
  ITokenizeLineResult tokenizeLine(String lineText, StackElement prevState);

  ITokenizeLineResult2 tokenizeLine2(String lineText, StackElement prevState);
}