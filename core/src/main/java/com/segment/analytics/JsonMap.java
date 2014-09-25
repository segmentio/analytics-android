package com.segment.analytics;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/**
 * A {@link Map} wrapper to expose Json functionality. Only the {@link #toString()} method is
 * modified to return a json formatted string. All other methods will be forwarded to a delegate
 * map.
 * <p/>
 * The purpose of this class is to not limit clients to a custom implementation of a Json type, they
 * can use existing {@link Map} and {@link java.util.List} implementations as they see fit. It adds
 * some utility methods, including methods to coerce numeric types from Strings, and a {@link
 * #putValue(String, Object)} to be able to chain method calls.
 * <p/>
 * Although it lets you use custom objects for values, note that type information is lost during
 * serialization. You should use one of the coercion methods if you're expecting a type after
 * serialization.
 */
class JsonMap implements Map<String, Object> {
  private final Map<String, Object> delegate;

  JsonMap() {
    this.delegate = new LinkedHashMap<String, Object>();
  }

  JsonMap(Map<String, Object> delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("Map must not be null.");
    }
    if (delegate instanceof JsonMap) {
      this.delegate = ((JsonMap) delegate).delegate; // avoid indirection
    } else {
      this.delegate = delegate;
    }
  }

  JsonMap(String json) {
    try {
      this.delegate = JsonUtils.jsonToMap(json);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public void clear() {
    delegate.clear();
  }

  @Override public boolean containsKey(Object key) {
    return delegate.containsKey(key);
  }

  @Override public boolean containsValue(Object value) {
    return delegate.containsValue(value);
  }

  @Override public Set<Entry<String, Object>> entrySet() {
    return delegate.entrySet();
  }

  @Override public Object get(Object key) {
    return delegate.get(key);
  }

  @Override public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override public Set<String> keySet() {
    return delegate.keySet();
  }

  @Override public Object put(String key, Object value) {
    return delegate.put(key, value);
  }

  @Override public void putAll(Map<? extends String, ?> map) {
    delegate.putAll(map);
  }

  @Override public Object remove(Object key) {
    return delegate.remove(key);
  }

  @Override public int size() {
    return delegate.size();
  }

  @Override public Collection<Object> values() {
    return delegate.values();
  }

  @Override public boolean equals(Object object) {
    return delegate.equals(object);
  }

  @Override public int hashCode() {
    return delegate.hashCode();
  }

  @Override public String toString() {
    try {
      return JsonUtils.mapToJson(delegate);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Helper method to be able to chain put methods. */
  JsonMap putValue(String key, Object value) {
    delegate.put(key, value);
    return this;
  }

  // The methods return boxed primitives to be able to return null and keep parity with Map#get

  /**
   * Returns the value mapped by {@code key} if it exists and is a integer or can be coerced to a
   * integer. Returns null otherwise.
   */
  Integer getInteger(String key) {
    Object value = get(key);
    if (value instanceof Integer) {
      return (Integer) value;
    }
    Integer integerValue = null;
    if (value instanceof Number) {
      integerValue = ((Number) value).intValue();
    } else if (value instanceof String) {
      try {
        integerValue = Integer.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    if (integerValue == null) {
      return null;
    } else {
      cache(key, integerValue);
      return integerValue;
    }
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a long or can be coerced to a
   * long.
   * Returns null otherwise.
   */
  Long getLong(String key) {
    Object value = get(key);
    if (value instanceof Long) {
      return (Long) value;
    }
    Long longValue = null;
    if (value instanceof Number) {
      longValue = ((Number) value).longValue();
    } else if (value instanceof String) {
      try {
        longValue = Long.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    if (longValue == null) {
      return null;
    } else {
      cache(key, longValue);
      return longValue;
    }
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a double or can be coerced to a
   * double. Returns null otherwise.
   */
  Double getDouble(String key) {
    Object value = get(key);
    if (value instanceof Double) {
      return (Double) value;
    }
    Double doubleValue = null;
    if (value instanceof Number) {
      doubleValue = ((Number) value).doubleValue();
    } else if (value instanceof String) {
      try {
        doubleValue = Double.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    if (doubleValue == null) {
      return null;
    } else {
      cache(key, doubleValue);
      return doubleValue;
    }
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a char or can be coerced to a
   * char.
   * Returns null otherwise.
   */
  Character getChar(String key) {
    Object value = get(key);
    if (value instanceof Character) {
      return (Character) value;
    }
    if (value != null && value instanceof String) {
      if (((String) value).length() == 1) {
        Character charValue = ((String) value).charAt(0);
        cache(key, charValue);
        return charValue;
      }
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a string or can be coerced to a
   * string. Returns null otherwise.
   * <p/>
   * This will return null only if the value does not exist, since all types can have a String
   * representation.
   */
  String getString(String key) {
    Object value = get(key);
    if (value instanceof String) {
      return (String) value;
    } else if (value != null) {
      String stringValue = String.valueOf(value);
      cache(key, stringValue);
      return stringValue;
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a boolean or can be coerced to a
   * boolean. Returns null otherwise.
   */
  Boolean getBoolean(String key) {
    Object value = get(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    } else if (value instanceof String) {
      String stringValue = (String) value;
      boolean bool = Boolean.valueOf(stringValue);
      cache(key, bool);
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a enum or can be coerced to a
   * enum.
   * Returns null otherwise.
   */
  <T extends Enum<T>> T getEnum(Class<T> enumType, String key) {
    if (enumType == null) {
      throw new IllegalArgumentException("enumType may not be null");
    }
    Object value = get(key);
    if (enumType.isInstance(value)) {
      return (T) value;
    } else if (value instanceof String) {
      String stringValue = (String) value;
      T enumValue = Enum.valueOf(enumType, stringValue);
      cache(key, enumValue);
      return enumValue;
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a JsonMap. Returns null otherwise.
   */
  JsonMap getJsonMap(Object key) {
    Object value = get(key);
    if (value instanceof Map) {
      return new JsonMap((Map<String, Object>) value);
    } else {
      return null;
    }
  }

  /**
   * Returns the value mapped by {@code key} if it exists and if it can be coerced to the given
   * type. The JsonMap subclass MUST have a map constructor.
   */
  <T extends JsonMap> T getJsonMap(String key, Class<T> clazz) {
    Object value = get(key);
    if (clazz.isInstance(value)) {
      //noinspection unchecked
      return (T) value;
    } else if (value instanceof Map) {
      // Try the map constructor, it's more efficient since we've already parsed the json tree
      try {
        Constructor<T> constructor = clazz.getDeclaredConstructor(Map.class);
        constructor.setAccessible(true);
        T typedValue = constructor.newInstance(value);
        cache(key, typedValue);
        return typedValue;
      } catch (NoSuchMethodException ignored) {
      } catch (InvocationTargetException ignored) {
      } catch (InstantiationException ignored) {
      } catch (IllegalAccessException ignored) {
      }
      throw new AssertionError("Could not find map constructor for " + clazz.getCanonicalName());
    }
    return null;
  }

  JSONObject toJsonObject() {
    return new JSONObject(delegate);
  }

  Map<String, String> toStringMap() {
    Map<String, String> map = new LinkedHashMap<String, String>();
    for (Map.Entry<String, Object> entry : entrySet()) {
      map.put(entry.getKey(), String.valueOf(entry.getValue()));
    }
    return map;
  }

  /** Shallow merge the given map into this map. */
  void merge(Map<String, Object> map) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  private void cache(String key, Object value) {
    try {
      delegate.put(key, value);
    } catch (UnsupportedOperationException e) {
      // ignore
    }
  }
}
