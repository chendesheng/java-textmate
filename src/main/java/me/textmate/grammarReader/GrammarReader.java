package me.textmate.grammarReader;

import me.textmate.Procedure2;
import me.textmate.types.IRawGrammar;
import me.textmate.grammarReader.SyncGrammarReader;
import com.fasterxml.jackson.databind.*;

public class GrammarReader {
  public static void readGrammar(String filePath, Procedure2<Object, IRawGrammar> callback) {
    // please use readGrammarSync
  }

  public static IRawGrammar readGrammarSync(String filepath) throws Exception {
    SyncGrammarReader reader = new SyncGrammarReader(filepath, getGrammarParser(filepath));
    return reader.load();
  }

  static IGrammarParser getGrammarParser(String filePath) throws Exception {
    if (!filePath.toLowerCase().endsWith(".json")) {
      throw new Exception("only support json format");
    }

    return new IGrammarParser() {
      public IRawGrammar call(String contents, String filename) {
        try {
          ObjectMapper mapper = new ObjectMapper();
          return (IRawGrammar) mapper.readValue(contents, IRawGrammar.class);
        } catch (Exception e) {
          e.printStackTrace();
          System.out.print(e.getMessage());
          return null;
        }
      }
    };
  }
}