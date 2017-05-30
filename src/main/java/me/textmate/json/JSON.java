package me.textmate.json;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.textmate.types.ILocation;

public class JSON {
  String source;
  String filename;
  boolean withMetadata;
  JSONStreamState streamState = new JSONStreamState(source);
  JSONToken token = new JSONToken();
  JSONState state = JSONState.ROOT_STATE;
  JSONObject cur = null;
  Stack<JSONState> stateStack = new Stack<JSONState>();
  Stack<JSONObject> objStack = new Stack<JSONObject>();

  void doFail(JSONStreamState streamState, String msg) throws JSONParseException {
    // console.log('Near offset ' + streamState.pos + ': ' + msg + ' ~~~' + streamState.source.substr(streamState.pos, 50) + '~~~');
    throw new JSONParseException("Near offset " + streamState.pos + ": " + msg + " ~~~"
        + streamState.source.substring(streamState.pos, streamState.pos + 50) + "~~~");
  }

  void pushState() {
    stateStack.push(state);
    objStack.push(cur);
  }

  void popState() {
    state = stateStack.pop();
    cur = objStack.pop();
  }

  void fail(String msg) throws JSONParseException {
    doFail(streamState, msg);
  }

  /**
   * precondition: the string is known to be valid JSON (https://www.ietf.org/rfc/rfc4627.txt)
   */
  boolean nextJSONToken(JSONStreamState _state, JSONToken _out) throws JSONParseException {
    _out.value = null;
    _out.type = JSONTokenType.UNKNOWN;
    _out.offset = -1;
    _out.len = -1;
    _out.line = -1;
    _out.character = -1;

    String source = _state.source;
    int pos = _state.pos;
    int len = _state.len;
    int line = _state.line;
    int character = _state.character;

    //------------------------ skip whitespace
    int chCode;
    do {
      if (pos >= len) {
        return false; /*EOS*/
      }

      chCode = source.codePointAt(pos);
      if (chCode == ChCode.SPACE || chCode == ChCode.HORIZONTAL_TAB || chCode == ChCode.CARRIAGE_RETURN) {
        // regular whitespace
        pos++;
        character++;
        continue;
      }

      if (chCode == ChCode.LINE_FEED) {
        // newline
        pos++;
        line++;
        character = 0;
        continue;
      }

      // not whitespace
      break;
    } while (true);

    _out.offset = pos;
    _out.line = line;
    _out.character = character;

    if (chCode == ChCode.QUOTATION_MARK) {
      //------------------------ strings
      _out.type = JSONTokenType.STRING;

      pos++;
      character++;

      do {
        if (pos >= len) {
          return false; /*EOS*/
        }

        chCode = source.codePointAt(pos);
        pos++;
        character++;

        if (chCode == ChCode.BACKSLASH) {
          // skip next char
          pos++;
          character++;
          continue;
        }

        if (chCode == ChCode.QUOTATION_MARK) {
          // end of the string
          break;
        }
      } while (true);

      _out.value = source.substring(_out.offset + 1, pos - 1);

      Pattern p = Pattern.compile("\\u([0-9A-Fa-f]{4})");
      Matcher m = p.matcher(_out.value);
      StringBuffer s = new StringBuffer();
      while (m.find()) {
        m.appendReplacement(s, Character.getName(Integer.parseInt(m.group(1), 16)));
      }
      _out.value = s.toString();

      p = Pattern.compile("\\(.)");
      m = p.matcher(_out.value);
      s = new StringBuffer();
      while (m.find()) {
        Character m0 = m.group(1).charAt(0);
        Character slashed = null;
        switch (m0) {
        case '"':
          slashed = '"';
        case '\\':
          slashed = '\\';
        case '/':
          slashed = '/';
        case 'b':
          slashed = '\b';
        case 'f':
          slashed = '\f';
        case 'n':
          slashed = '\n';
        case 'r':
          slashed = '\r';
        case 't':
          slashed = '\t';
        default:
          doFail(_state, "invalid escape sequence");
        }
        m.appendReplacement(s, slashed.toString());
      }
      _out.value = s.toString();
    } else if (chCode == ChCode.LEFT_SQUARE_BRACKET) {

      _out.type = JSONTokenType.LEFT_SQUARE_BRACKET;
      pos++;
      character++;

    } else if (chCode == ChCode.LEFT_CURLY_BRACKET) {

      _out.type = JSONTokenType.LEFT_CURLY_BRACKET;
      pos++;
      character++;

    } else if (chCode == ChCode.RIGHT_SQUARE_BRACKET) {

      _out.type = JSONTokenType.RIGHT_SQUARE_BRACKET;
      pos++;
      character++;

    } else if (chCode == ChCode.RIGHT_CURLY_BRACKET) {

      _out.type = JSONTokenType.RIGHT_CURLY_BRACKET;
      pos++;
      character++;

    } else if (chCode == ChCode.COLON) {

      _out.type = JSONTokenType.COLON;
      pos++;
      character++;

    } else if (chCode == ChCode.COMMA) {

      _out.type = JSONTokenType.COMMA;
      pos++;
      character++;

    } else if (chCode == ChCode.n) {
      //------------------------ null

      _out.type = JSONTokenType.NULL;
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.u) {
        return false;
        /* INVALID */ }
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.l) {
        return false;
        /* INVALID */ }
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.l) {
        return false;
        /* INVALID */ }
      pos++;
      character++;

    } else if (chCode == ChCode.t) {
      //------------------------ true

      _out.type = JSONTokenType.TRUE;
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.r) {
        return false;
        /* INVALID */ }
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.u) {
        return false;
        /* INVALID */ }
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.e) {
        return false;
        /* INVALID */ }
      pos++;
      character++;

    } else if (chCode == ChCode.f) {
      //------------------------ false

      _out.type = JSONTokenType.FALSE;
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.a) {
        return false;
        /* INVALID */ }
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.l) {
        return false;
        /* INVALID */ }
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.s) {
        return false;
        /* INVALID */ }
      pos++;
      character++;
      chCode = source.codePointAt(pos);
      if (chCode != ChCode.e) {
        return false;
        /* INVALID */ }
      pos++;
      character++;

    } else {
      //------------------------ numbers

      _out.type = JSONTokenType.NUMBER;
      do {
        if (pos >= len) {
          return false;
          /*EOS*/ }

        chCode = source.codePointAt(pos);
        if (chCode == ChCode.DOT || (chCode >= ChCode.D0 && chCode <= ChCode.D9)
            || (chCode == ChCode.e || chCode == ChCode.E) || (chCode == ChCode.MINUS || chCode == ChCode.PLUS)) {
          // looks like a piece of a number
          pos++;
          character++;
          continue;
        }

        // pos--; character--;
        break;
      } while (true);
    }

    _out.len = pos - _out.offset;
    if (_out.value == null) {
      _out.value = source.substring(_out.offset, _out.offset + _out.len);
    }

    _state.pos = pos;
    _state.line = line;
    _state.character = character;

    // console.log('PRODUCING TOKEN: ', _out.value, JSONTokenType[_out.type]);

    return true;
  }

  public static Object parse(String source, String filename, boolean withMetadata) throws JSONParseException {
    return (new JSON()).doParse(source, filename, withMetadata);
  }

  public Object doParse(String source, String filename, boolean withMetadata) throws JSONParseException {
    this.source = source;
    this.filename = filename;
    this.withMetadata = withMetadata;
    this.streamState = new JSONStreamState(source);
    this.token = new JSONToken();
    this.state = JSONState.ROOT_STATE;
    this.cur = null;
    this.stateStack = new Stack<JSONState>();
    this.objStack = new Stack<JSONObject>();

    while (nextJSONToken(streamState, token)) {

      if (state == JSONState.ROOT_STATE) {
        if (cur != null) {
          fail("too many constructs in root");
        }

        if (token.type == JSONTokenType.LEFT_CURLY_BRACKET) {
          cur = JSONObject.newMap();
          if (withMetadata) {
            cur.vscodeTextmateLocation = token.toLocation(filename);
          }
          pushState();
          state = JSONState.DICT_STATE;
          continue;
        }

        if (token.type == JSONTokenType.LEFT_SQUARE_BRACKET) {
          cur = JSONObject.newArray();
          pushState();
          state = JSONState.ARR_STATE;
          continue;
        }

        fail("unexpected token in root");

      }

      if (state == JSONState.DICT_STATE_COMMA) {

        if (token.type == JSONTokenType.RIGHT_CURLY_BRACKET) {
          popState();
          continue;
        }

        if (token.type == JSONTokenType.COMMA) {
          state = JSONState.DICT_STATE_NO_CLOSE;
          continue;
        }

        fail("expected , or }");

      }

      if (state == JSONState.DICT_STATE || state == JSONState.DICT_STATE_NO_CLOSE) {

        if (state == JSONState.DICT_STATE && token.type == JSONTokenType.RIGHT_CURLY_BRACKET) {
          popState();
          continue;
        }

        if (token.type == JSONTokenType.STRING) {
          String keyValue = token.value;

          if (!nextJSONToken(streamState, token) || (/*TS bug*/token.type) != JSONTokenType.COLON) {
            fail("expected colon");
          }
          if (!nextJSONToken(streamState, token)) {
            fail("expected value");
          }

          state = JSONState.DICT_STATE_COMMA;

          if (token.type == JSONTokenType.STRING) {
            cur.put(keyValue, token.value);
            continue;
          }
          if (token.type == JSONTokenType.NULL) {
            cur.put(keyValue, null);
            continue;
          }
          if (token.type == JSONTokenType.TRUE) {
            cur.put(keyValue, true);
            continue;
          }
          if (token.type == JSONTokenType.FALSE) {
            cur.put(keyValue, false);
            continue;
          }
          if (token.type == JSONTokenType.NUMBER) {
            cur.put(keyValue, Double.parseDouble(token.value));
            continue;
          }
          if (token.type == JSONTokenType.LEFT_SQUARE_BRACKET) {
            JSONObject newArr = JSONObject.newArray();
            cur.put(keyValue, newArr);
            pushState();
            state = JSONState.ARR_STATE;
            cur = newArr;
            continue;
          }
          if (token.type == JSONTokenType.LEFT_CURLY_BRACKET) {
            JSONObject newDict = JSONObject.newMap();
            if (withMetadata) {
              newDict.vscodeTextmateLocation = token.toLocation(filename);
            }
            cur.put(keyValue, newDict);
            pushState();
            state = JSONState.DICT_STATE;
            cur = newDict;
            continue;
          }
        }

        fail("unexpected token in dict");
      }

      if (state == JSONState.ARR_STATE_COMMA) {

        if (token.type == JSONTokenType.RIGHT_SQUARE_BRACKET) {
          popState();
          continue;
        }

        if (token.type == JSONTokenType.COMMA) {
          state = JSONState.ARR_STATE_NO_CLOSE;
          continue;
        }

        fail("expecteds, or ]");
      }

      if (state == JSONState.ARR_STATE || state == JSONState.ARR_STATE_NO_CLOSE) {

        if (state == JSONState.ARR_STATE && token.type == JSONTokenType.RIGHT_SQUARE_BRACKET) {
          popState();
          continue;
        }

        state = JSONState.ARR_STATE_COMMA;

        if (token.type == JSONTokenType.STRING) {
          cur.push(token.value);
          continue;
        }
        if (token.type == JSONTokenType.NULL) {
          cur.push(null);
          continue;
        }
        if (token.type == JSONTokenType.TRUE) {
          cur.push(true);
          continue;
        }
        if (token.type == JSONTokenType.FALSE) {
          cur.push(false);
          continue;
        }
        if (token.type == JSONTokenType.NUMBER) {
          cur.push(Double.parseDouble(token.value));
          continue;
        }

        if (token.type == JSONTokenType.LEFT_SQUARE_BRACKET) {
          JSONObject newArr = JSONObject.newArray();
          cur.push(newArr);
          pushState();
          state = JSONState.ARR_STATE;
          cur = newArr;
          continue;
        }
        if (token.type == JSONTokenType.LEFT_CURLY_BRACKET) {
          JSONObject newDict = JSONObject.newMap();
          if (withMetadata) {
            newDict.vscodeTextmateLocation = token.toLocation(filename);
          }
          cur.push(newDict);
          pushState();
          state = JSONState.DICT_STATE;
          cur = newDict;
          continue;
        }

        fail("unexpected token in array");
      }

      fail("unknown state");
    }

    if (objStack.size() != 0) {
      fail("unclosed constructs");
    }

    return cur;
  }

}

class JSONStreamState {
  public String source;
  public int pos;
  public int len;

  public int line;
  public int character;

  public JSONStreamState(String source) {
    this.source = source;
    this.pos = 0;
    this.len = source.length();
    this.line = 1;
    this.character = 0;
  }
}

enum JSONTokenType {
  UNKNOWN, STRING, LEFT_SQUARE_BRACKET, // [
  LEFT_CURLY_BRACKET, // {
  RIGHT_SQUARE_BRACKET, // ]
  RIGHT_CURLY_BRACKET, // }
  COLON, // :
  COMMA, // ,
  NULL, TRUE, FALSE, NUMBER
}

enum JSONState {
  ROOT_STATE, DICT_STATE, DICT_STATE_COMMA, DICT_STATE_NO_CLOSE, ARR_STATE, ARR_STATE_COMMA, ARR_STATE_NO_CLOSE,
}

class ChCode {
  public static int SPACE = 0x20;
  public static int HORIZONTAL_TAB = 0x09;
  public static int CARRIAGE_RETURN = 0x0D;
  public static int LINE_FEED = 0x0A;
  public static int QUOTATION_MARK = 0x22;
  public static int BACKSLASH = 0x5C;

  public static int LEFT_SQUARE_BRACKET = 0x5B;
  public static int LEFT_CURLY_BRACKET = 0x7B;
  public static int RIGHT_SQUARE_BRACKET = 0x5D;
  public static int RIGHT_CURLY_BRACKET = 0x7D;
  public static int COLON = 0x3A;
  public static int COMMA = 0x2C;
  public static int DOT = 0x2E;

  public static int D0 = 0x30;
  public static int D9 = 0x39;

  public static int MINUS = 0x2D;
  public static int PLUS = 0x2B;

  public static int E = 0x45;
  public static int a = 0x61;
  public static int e = 0x65;
  public static int f = 0x66;
  public static int l = 0x6C;
  public static int n = 0x6E;
  public static int r = 0x72;
  public static int s = 0x73;
  public static int t = 0x74;
  public static int u = 0x75;
}

class JSONToken {
  public String value;
  public JSONTokenType type;

  public int offset;
  public int len;

  public int line; /* 1 based line number */
  public int character;

  public JSONToken() {
    this.value = null;
    this.offset = -1;
    this.len = -1;
    this.line = -1;
    this.character = -1;
  }

  public ILocation toLocation(String filename) {
    ILocation result = new ILocation();
    result.filename = filename;
    result.line = this.line;
    result.character = this.character;
    return result;
  }

}
