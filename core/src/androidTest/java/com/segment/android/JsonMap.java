package com.segment.android;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A Map wrapper that exposes additional methods to coerce types lost during serialization.
 * For instance, a float will be deserialized as a double. getFloat() will try to coerce that value
 * for you.
 *
 * The types on the left get mapped to the types on the right.
 * byte -> integer
 * short -> integer
 * integer -> integer
 * long -> long
 * float -> double
 * double -> double
 * char -> String
 * String -> String
 * boolean -> boolean
 *
 * In addition to these, Strings can be coerced to Numbers as well.
 */
public class JsonMap implements Map<String, V> {
  @Override public void clear() {

  }

  @Override public boolean containsKey(Object key) {
    return false;
  }

  @Override public boolean containsValue(Object value) {
    return false;
  }

  @Override public Set<Entry<String, V>> entrySet() {
    return null;
  }

  @Override public V get(Object key) {
    return null;
  }

  @Override public boolean isEmpty() {
    return false;
  }

  @Override public Set<String> keySet() {
    return null;
  }

  @Override public V put(String key, V value) {
    return null;
  }

  @Override public void putAll(Map<? extends String, ? extends V> map) {

  }

  @Override public V remove(Object key) {
    return null;
  }

  @Override public int size() {
    return 0;
  }

  @Override public Collection<V> values() {
    return null;
  }
}
