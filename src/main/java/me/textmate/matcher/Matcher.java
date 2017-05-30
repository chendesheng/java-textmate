package me.textmate.matcher;

public interface Matcher<T> {
  boolean call(T matcherInput);
}
