package me.textmate.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.oniguruma.IOnigCaptureIndex;

public class RegExpSource {
  public static String HAS_BACK_REFERENCES = "\\\\(\\d+)";
  public static String BACK_REFERENCING_END = "\\\\(\\d+)";

  public String source;
  public final int ruleId;
  public boolean hasAnchor;
  public final boolean hasBackReferences;
  private IRegExpSourceAnchorCache _anchorCache;

  public RegExpSource(String regExpSource, int ruleId) {
    this(regExpSource, ruleId, true);
  }

  public RegExpSource(String regExpSource, int ruleId, boolean handleAnchors) {
    if (handleAnchors) {
      this._handleAnchors(regExpSource);
    } else {
      this.source = regExpSource;
      this.hasAnchor = false;
    }

    if (this.hasAnchor) {
      this._anchorCache = this._buildAnchorCache();
    }

    this.ruleId = ruleId;
    this.hasBackReferences = Pattern.matches(HAS_BACK_REFERENCES, this.source);

    // console.log('input: ' + regExpSource + ' => ' + this.source + ', ' + this.hasAnchor);
  }

  public RegExpSource clone() {
    return new RegExpSource(this.source, this.ruleId, true);
  }

  public void setSource(String newSource) {
    if (this.source == newSource) {
      return;
    }
    this.source = newSource;

    if (this.hasAnchor) {
      this._anchorCache = this._buildAnchorCache();
    }
  }

  private void _handleAnchors(String regExpSource) {
    if (regExpSource != null) {
      int pos;
      int len;
      Character ch;
      Character nextCh;
      int lastPushedPos = 0;
      List<String> output = new ArrayList<String>();

      boolean hasAnchor = false;
      for (pos = 0, len = regExpSource.length(); pos < len; pos++) {
        ch = regExpSource.charAt(pos);

        if (ch == '\\') {
          if (pos + 1 < len) {
            nextCh = regExpSource.charAt(pos + 1);
            if (nextCh == 'z') {
              output.add(regExpSource.substring(lastPushedPos, pos));
              output.add("$(?!\\n)(?<!\\n)");
              lastPushedPos = pos + 2;
            } else if (nextCh == 'A' || nextCh == 'G') {
              hasAnchor = true;
            }
            pos++;
          }
        }
      }

      this.hasAnchor = hasAnchor;
      if (lastPushedPos == 0) {
        // No \z hit
        this.source = regExpSource;
      } else {
        output.add(regExpSource.substring(lastPushedPos, len));
        this.source = String.join("", output);
      }
    } else {
      this.hasAnchor = false;
      this.source = regExpSource;
    }
  }

  public String resolveBackReferences(final String lineText, IOnigCaptureIndex[] captureIndices) {
    Pattern p = Pattern.compile(BACK_REFERENCING_END);
    Matcher m = p.matcher(this.source);
    StringBuffer s = new StringBuffer();
    while (m.find()) {
      IOnigCaptureIndex capture = captureIndices[Integer.parseInt(m.group(1))];
      if (capture == null) {
        m.appendReplacement(s, "");
      } else {
        String v = lineText.substring(capture.getStart(), capture.getEnd());
        m.appendReplacement(s, v);
      }
    }
    return s.toString();
  }

  private IRegExpSourceAnchorCache _buildAnchorCache() {
    String[] A0_G0_result = new String[this.source.length()];
    String[] A0_G1_result = new String[this.source.length()];
    String[] A1_G0_result = new String[this.source.length()];
    String[] A1_G1_result = new String[this.source.length()];

    int pos;
    int len;
    Character ch;
    Character nextCh;

    for (pos = 0, len = this.source.length(); pos < len; pos++) {
      ch = this.source.charAt(pos);
      A0_G0_result[pos] = ch.toString();
      A0_G1_result[pos] = ch.toString();
      A1_G0_result[pos] = ch.toString();
      A1_G1_result[pos] = ch.toString();

      if (ch == '\\') {
        if (pos + 1 < len) {
          nextCh = this.source.charAt(pos + 1);
          if (nextCh == 'A') {
            A0_G0_result[pos + 1] = "\uFFFF";
            A0_G1_result[pos + 1] = "\uFFFF";
            A1_G0_result[pos + 1] = "A";
            A1_G1_result[pos + 1] = "A";
          } else if (nextCh == 'G') {
            A0_G0_result[pos + 1] = "\uFFFF";
            A0_G1_result[pos + 1] = "G";
            A1_G0_result[pos + 1] = "\uFFFF";
            A1_G1_result[pos + 1] = "G";
          } else {
            A0_G0_result[pos + 1] = nextCh.toString();
            A0_G1_result[pos + 1] = nextCh.toString();
            A1_G0_result[pos + 1] = nextCh.toString();
            A1_G1_result[pos + 1] = nextCh.toString();
          }
          pos++;
        }
      }
    }

    IRegExpSourceAnchorCache res = new IRegExpSourceAnchorCache();
    res.A0_G0 = String.join("", A0_G0_result);
    res.A0_G1 = String.join("", A0_G1_result);
    res.A1_G0 = String.join("", A1_G0_result);
    res.A1_G1 = String.join("", A1_G1_result);
    return res;
  }

  public String resolveAnchors(boolean allowA, boolean allowG) {
    if (!this.hasAnchor) {
      return this.source;
    }

    if (allowA) {
      if (allowG) {
        return this._anchorCache.A1_G1;
      } else {
        return this._anchorCache.A1_G0;
      }
    } else {
      if (allowG) {
        return this._anchorCache.A0_G1;
      } else {
        return this._anchorCache.A0_G0;
      }
    }
  }
}