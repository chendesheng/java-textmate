package me.textmate.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import me.textmate.Predicate;

public class MatcherCreator<T> {
  private Predicate<String[], T> matchesName;
  private String token;
  private INext tokenizer;

  private Matcher<T> parseOperand() {
    if (this.token.equals("-")) {
      this.token = tokenizer.next();
      final Matcher<T> expressionToNegate = parseOperand();
      return new Matcher<T>() {
        public boolean call(T matcherInput) {
          return expressionToNegate != null && !expressionToNegate.call(matcherInput);
        }
      };
    }
    if (token.equals("(")) {
      token = tokenizer.next();
      Matcher<T> expressionInParents = parseExpression("|");
      if (token.equals(")")) {
        token = tokenizer.next();
      }
      return expressionInParents;
    }
    if (isIdentifier(token)) {
      final List<String> identifiers = new ArrayList<String>();
      do {
        identifiers.add(token);
        token = tokenizer.next();
      } while (isIdentifier(token));

      return new Matcher<T>() {
        public boolean call(T matcherInput) {
          return matchesName.call(identifiers.toArray(new String[0]), matcherInput);
        }
      };
    }
    return null;
  }

  private Matcher<T> parseConjunction() {
    final List<Matcher<T>> matchers = new ArrayList<Matcher<T>>();
    Matcher<T> matcher = parseOperand();
    while (matcher != null) {
      matchers.add(matcher);
      matcher = parseOperand();
    }
    return new Matcher<T>() { // and
      public boolean call(T matcherInput) {
        for (Matcher<T> matcher : matchers) {
          if (!matcher.call(matcherInput)) {
            return false;
          }
        }
        return true;
      }
    };
  }

  private Matcher<T> parseExpression() {
    return parseExpression(",");
  }

  private Matcher<T> parseExpression(String orOperatorToken) {
    final List<Matcher<T>> matchers = new ArrayList<Matcher<T>>();
    Matcher<T> matcher = parseConjunction();
    while (matcher != null) {
      matchers.add(matcher);
      if (token == orOperatorToken) {
        do {
          token = tokenizer.next();
        } while (token == orOperatorToken); // ignore subsequent commas
      } else {
        break;
      }
      matcher = parseConjunction();
    }
    return new Matcher<T>() { // or
      public boolean call(T matcherInput) {
        for (Matcher<T> matcher : matchers) {
          if (matcher.call(matcherInput)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  public Matcher<T> createMatcher(String expression, Predicate<String[], T> matchesName) {
    this.matchesName = matchesName;
    this.tokenizer = newTokenizer(expression);
    this.token = tokenizer.next();
    Matcher<T> result = parseExpression();
    if (result != null) {
      return result;
    } else {
      return new Matcher<T>() {
        public boolean call(T matcherInput) {
          return false;
        }
      };
    }
  }

  private static boolean isIdentifier(String token) {
    return token != null && Pattern.matches("[\\w\\.:]+", token);
  }

  private static INext newTokenizer(final String input) {
    final Pattern regex = Pattern.compile("([\\w\\.:]+|[\\,\\|\\-\\(\\)])");

    final java.util.regex.Matcher matcher = regex.matcher(input);
    return new INext() {
      java.util.regex.Matcher _match = matcher;

      public String next() {
        if (_match == null) {
          return null;
        }
        String res = _match.group(0);
        _match = regex.matcher(input);
        return res;
      }
    };
  }
}

interface INext {
  String next();
}