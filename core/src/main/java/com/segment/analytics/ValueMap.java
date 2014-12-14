package com.segment.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

import static com.segment.analytics.JsonUtils.mapToJson;
import static com.segment.analytics.Utils.getSegmentSharedPreferences;
import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * A class that wraps an existing {@link Map} to expose value type functionality.
 * <p>
 * All {@link java.util.Map} methods will simply be forwarded to a delegate map. This class is
 * meant to subclassed and provide methods to access values in keys.
 * <p>
 * Although it lets you use custom objects for values, note that type information is lost during
 * serialization. You should use one of the coercion methods instead to get objects of a concrete
 * type.
 */
class ValueMap implements Map<String, Object> {
  private final Map<String, Object> delegate;

  ValueMap() {
    delegate = new LinkedHashMap<String, Object>();
  }

  ValueMap(Map<String, Object> map) {
    if (map == null) {
      throw new IllegalArgumentException("Map must not be null.");
    }
    this.delegate = map;
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
    return object == this || delegate.equals(object);
  }

  @Override public int hashCode() {
    return delegate.hashCode();
  }

  @Override public String toString() {
    return delegate.toString();
  }

  /** Helper method to be able to chain put methods. */
  ValueMap putValue(String key, Object value) {
    delegate.put(key, value);
    return this;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a integer or can be coerced to a
   * integer. Returns defaultValue otherwise.
   */
  int getInt(String key, int defaultValue) {
    Object value = get(key);
    if (value instanceof Integer) {
      return (int) value;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue();
    } else if (value instanceof String) {
      try {
        return Integer.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    return defaultValue;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a long or can be coerced to a
   * long. Returns defaultValue otherwise.
   */
  long getLong(String key, long defaultValue) {
    Object value = get(key);
    if (value instanceof Long) {
      return (long) value;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    } else if (value instanceof String) {
      try {
        return Long.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    return defaultValue;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a double or can be coerced to a
   * double. Returns defaultValue otherwise.
   */
  double getDouble(String key, double defaultValue) {
    Object value = get(key);
    if (value instanceof Double) {
      return (double) value;
    }
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else if (value instanceof String) {
      try {
        return Double.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    return defaultValue;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a char or can be coerced to a
   * char. Returns defaultValue otherwise.
   */
  char getChar(String key, char defaultValue) {
    Object value = get(key);
    if (value instanceof Character) {
      return (Character) value;
    }
    if (value != null && value instanceof String) {
      if (((String) value).length() == 1) {
        return ((String) value).charAt(0);
      }
    }
    return defaultValue;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a string or can be coerced to a
   * string. Returns null otherwise.
   * <p>
   * This will return null only if the value does not exist, since all types can have a String
   * representation.
   */
  String getString(String key) {
    Object value = get(key);
    if (value instanceof String) {
      return (String) value;
    } else if (value != null) {
      return String.valueOf(value);
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a boolean or can be coerced to a
   * boolean. Returns defaultValue otherwise.
   */
  boolean getBoolean(String key, boolean defaultValue) {
    Object value = get(key);
    if (value instanceof Boolean) {
      return (boolean) value;
    } else if (value instanceof String) {
      return Boolean.valueOf((String) value);
    }
    return defaultValue;
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
      return Enum.valueOf(enumType, stringValue);
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a {@link ValueMap}. Returns null
   * otherwise.
   */
  ValueMap getValueMap(Object key) {
    Object value = get(key);
    if (value instanceof ValueMap) {
      return (ValueMap) value;
    } else if (value instanceof Map) {
      //noinspection unchecked
      return new ValueMap((Map<String, Object>) value);
    } else {
      return null;
    }
  }

  /**
   * Returns the value mapped by {@code key} if it exists and if it can be coerced to the given
   * type. The expected subclass MUST have a constructor that accepts a {@link Map}.
   */
  <T extends ValueMap> T getValueMap(String key, Class<T> clazz) {
    Object value = get(key);
    return coerceToValueMap(value, clazz);
  }

  /**
   * Coerce an object to a JsonMap. It will first check if the object is already of the expected
   * type. If not, it checks if the object a {@link Map} type, and feeds it to the constructor by
   * reflection.
   */
  private <T extends ValueMap> T coerceToValueMap(Object object, Class<T> clazz) {
    if (object == null) return null;
    if (clazz.isInstance(object)) return (T) object;
    if (object instanceof Map) return createValueMap((Map) object, clazz);
    return null;
  }

  private static <T extends ValueMap> T createValueMap(Map map, Class<T> clazz) {
    try {
      Constructor<T> constructor = clazz.getDeclaredConstructor(Map.class);
      constructor.setAccessible(true);
      return constructor.newInstance(map);
    } catch (Exception e) {
      throw new RuntimeException("Could not create instance of " + clazz.getCanonicalName(), e);
    }
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a List of {@code T}. Returns null
   * otherwise.
   */
  <T extends ValueMap> List<T> getList(Object key, Class<T> clazz) {
    Object value = get(key);
    if (value instanceof List) {
      List list = (List) value;
      try {
        ArrayList<T> real = new ArrayList<T>();
        for (Object item : list) {
          T typedValue = coerceToValueMap(item, clazz);
          if (typedValue != null) {
            real.add(typedValue);
          }
        }
        return real;
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  /** Return a copy of the contents of this map as a {@link JSONObject}. */
  JSONObject toJsonObject() {
    return new JSONObject(delegate);
  }

  /** Return a copy of the contents of this map as a {@code Map<String, String>}. */
  Map<String, String> toStringMap() {
    Map<String, String> map = new HashMap<String, String>();
    for (Map.Entry<String, Object> entry : entrySet()) {
      map.put(entry.getKey(), String.valueOf(entry.getValue()));
    }
    return map;
  }

  /** A class to let you store arbitrary key - {@link ValueMap} pairs. */
  static class Cache<T extends ValueMap> {
    private final SharedPreferences preferences;
    private final String key;
    private final Class<T> clazz;
    private T value;

    Cache(Context context, String key, Class<T> clazz) {
      this.preferences = getSegmentSharedPreferences(context);
      this.key = key;
      this.clazz = clazz;
    }

    T get() {
      if (value == null) {
        String json = preferences.getString(key, null);
        if (isNullOrEmpty(json)) {
          return null;
        }
        try {
          Map<String, Object> map = JsonUtils.jsonToMap(json);
          // todo: offload this to a method, so it can subclassed and skip reflection
          value = ValueMap.createValueMap(map, clazz);
        } catch (IOException ignored) {
          // todo: log
          return null;
        }
      }
      return value;
    }

    boolean isSet() {
      return preferences.contains(key);
    }

    void set(T value) {
      this.value = value;
      try {
        String json = mapToJson(value);
        preferences.edit().putString(key, json).apply();
      } catch (IOException ignored) {
        // todo: log
      }
    }

    void delete() {
      preferences.edit().remove(key).apply();
    }
  }
}
