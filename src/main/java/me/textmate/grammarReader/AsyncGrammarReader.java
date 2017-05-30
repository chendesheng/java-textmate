package me.textmate.grammarReader;

import me.textmate.Procedure2;
import me.textmate.types.IRawGrammar;

public class AsyncGrammarReader {
  // private String _filepath;
  // private IGrammarParser _parser;

  public AsyncGrammarReader(String filepath, IGrammarParser parser) {
    // this._filepath = filepath;
    // this._parser = parser;
  }

  public void load(Procedure2<Object, IRawGrammar> callback) throws Exception {
    throw new Exception("not impletmented, please use SyncGrammarReader");
  }
}
