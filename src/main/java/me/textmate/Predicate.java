package me.textmate;

public interface Predicate<T1, T2> {
  boolean call(T1 t1, T2 t2);
}