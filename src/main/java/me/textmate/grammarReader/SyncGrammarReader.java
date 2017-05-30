package me.textmate.grammarReader;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import me.textmate.types.IRawGrammar;

public class SyncGrammarReader {
  private final String _filePath;
  private final IGrammarParser _parser;

  public SyncGrammarReader(String filePath, IGrammarParser parser) {
    this._filePath = filePath;
    this._parser = parser;
  }

  public IRawGrammar load() throws Exception {
    try {
      String contents = Charset.forName("UTF-8")
          .decode(ByteBuffer.wrap(Files.readAllBytes(Paths.get(this._filePath))))
          .toString();
      try {
        return this._parser.call(contents, this._filePath);
      } catch (Exception e) {
        throw new Exception("Error parsing " + this._filePath + ": " + e.getMessage() + ".");
      }
    } catch (Exception e) {
      throw new Exception("Error reading " + this._filePath + ": " + e.getMessage() + ".");
    }

  }
}
