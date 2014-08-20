package com.segment.android.json;

import com.segment.android.internal.Utils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/**
 * A wrapper around {@link Map} to expose Json functionality. Only the {@link #toString()} method
 * is
 * modified to return a json formatted string. All other methods will be forwarded to another map.
 * <p/>
 * The purpose of this class is to not limit clients to a custom implementation of a Json type,
 * they
 * can use existing {@link Map} and {@link java.util.List} implementations as they see fit. It adds
 * some utility methods, including methods to coerce numeric types from Strings, and a {@link
 * #putValue(String, Object)} to be able to chain method calls.
 * <p/>
 * To create an instance of this class, use one of the static factory methods.
 * <code>JsonMap<Object>
 * map = JsonMap.create();</code> <code>JsonMap<Object> map = JsonMap.decode(json);</code>
 * <code>JsonMap<Object> map = JsonMap.wrap(new HashMap<String, Object>);</code>
 * <p/>
 * Since it implements the {@link Map} interface, you could just as simply do: <code>Map<String,
 * Object> map = JsonMap.create();</code> <code>Map<String, Object> map =
 * JsonMap.decode(json);</code> <code>Map<String, Object> map = JsonMap.wrap(new HashMap<String,
 * Object>);</code>
 * <p/>
 * Although it lets you use custom objects for values, note that type information is lost during
 * serialization. e.g A custom class Person using the default <code>toString</code> implementation.
 * {@code JsonMap<Object> map = JsonMap.decode(); map.put("person", new Person("john", "doe", 32));
 * Person person = (Person) map.get("person"); // no serialization yet String json =
 * map.toString();
 * JsonMap<Object> deserialized = JsonMap.decode(json); // The line below will throw a
 * ClassCastException, since Person was stored as a String Person person = (Person)
 * deserialized.get("person"); // You'd actually get back something like 'Person@123132' for the
 * default toString implementation. }
 * <p/>
 * Only String, Integer, Double, Long and Boolean types are supported. Short, Byte, Float and char
 * are deserialized to one of the above types. Short -> Integer Byte -> Integer Float -> Double
 * Char
 * -> String
 */
public class JsonMap implements Map<String, Object> {
  final Map<String, Object> delegate;

  public JsonMap() {
    this.delegate = new LinkedHashMap<String, Object>();
  }

  public JsonMap(Map<String, Object> delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("Map must not be null.");
    }
    if (delegate instanceof JsonMap) {
      this.delegate = ((JsonMap) delegate).delegate;
    } else {
      this.delegate = delegate;
    }
  }

  public JsonMap(String json) {
    if (Utils.isNullOrEmpty(json)) {
      throw new IllegalArgumentException("Map must not be null.");
    }
    try {
      this.delegate = JsonUtils.toMap(json);
    } catch (JsonConversionException e) {
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
    for (Entry<? extends String, ?> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
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
      return JsonUtils.fromMap(delegate);
    } catch (JsonConversionException e) {
      throw new RuntimeException(e);
    }
  }

  /** Helper method to be able to chain put methods. */
  public JsonMap putValue(String key, Object value) {
    delegate.put(key, value);
    return this;
  }

  // Coercion Methods
  /* The methods return boxed primitives to be able to return null and keep parity with Map. */

  // todo: better null handling

  /**
   * Returns the value mapped by {@code key} if it exists and is a byte or can be coerced to a
   * byte.
   * Returns null otherwise.
   */
  public Byte getByte(Object key) {
    Object value = get(key);
    if (value instanceof Byte) {
      return (Byte) value;
    } else if (value instanceof Number) {
      return ((Number) value).byteValue();
    } else if (value instanceof String) {
      try {
        return Byte.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // Ignore
      }
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a short or can be coerced to a
   * short. Returns null otherwise.
   */
  public Short getShort(Object key) {
    Object value = get(key);
    if (value != null) {
      if (value instanceof Short) {
        return (Short) value;
      } else if (value instanceof Number) {
        return ((Number) value).shortValue();
      } else if (value instanceof String) {
        try {
          return Short.valueOf((String) value);
        } catch (NumberFormatException ignored) {

        }
      }
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a integer or can be coerced to a
   * integer. Returns null otherwise.
   */
  public Integer getInteger(Object key) {
    Object value = get(key);
    if (value instanceof Integer) {
      return (Integer) value;
    } else if (value instanceof Number) {
      return ((Number) value).intValue();
    } else if (value instanceof String) {
      try {
        return Integer.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a long or can be coerced to a
   * long.
   * Returns null otherwise.
   */
  public Long getLong(Object key) {
    Object value = get(key);
    if (value instanceof Long) {
      return (Long) value;
    } else if (value instanceof Number) {
      return ((Number) value).longValue();
    } else if (value instanceof String) {
      try {
        return Long.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a integer or can be coerced to a
   * integer. Returns null otherwise.
   */
  public Float getFloat(Object key) {
    Object value = get(key);
    if (value instanceof Float) {
      return (Float) value;
    } else if (value instanceof Number) {
      return ((Number) value).floatValue();
    } else if (value instanceof String) {
      try {
        return Float.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a double or can be coerced to a
   * double. Returns null otherwise.
   */
  public Double getDouble(Object key) {
    Object value = get(key);
    if (value instanceof Double) {
      return (Double) value;
    } else if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else if (value instanceof String) {
      try {
        return Double.valueOf((String) value);
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a char or can be coerced to a
   * char.
   * Returns null otherwise.
   */
  public Character getChar(Object key) {
    Object value = get(key);
    if (value instanceof Character) {
      return (Character) value;
    } else if (value != null && value instanceof String) {
      if (((String) value).length() == 1) {
        return ((String) value).charAt(0);
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
  public String getString(Object key) {
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
   * boolean. Returns null otherwise.
   */
  public Boolean getBoolean(Object key) {
    Object value = get(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    } else if (value instanceof String) {
      String stringValue = (String) value;
      if ("false".equalsIgnoreCase(stringValue)) {
        return false;
      } else if ("true".equalsIgnoreCase(stringValue)) {
        return true;
      }
    }
    return null;
  }

  /**
   * Returns the value mapped by {@code key} if it exists and is a boolean or can be coerced to a
   * boolean. Returns null otherwise.
   */
  public JsonMap getJsonMap(Object key) {
    Object value = get(key);
    if (value instanceof Map) {
      return new JsonMap((Map<String, Object>) value);
    } else {
      return null;
    }
  }

  public JSONObject toJsonObject() {
    return new JSONObject(delegate);
  }

  public Map<String, String> toStringMap() {
    Map<String, String> map = new HashMap<String, String>();

    Iterator<String> it = keySet().iterator();
    while (it.hasNext()) {
      String key = it.next();
      String value = String.valueOf(get(key));
      map.put(key, value);
    }

    return map;
  }

  public Map<String, Object> delegate() {
    return delegate;
  }

  /** Returns true if the map is null or empty, false otherwise. */
  public static boolean isNullOrEmpty(JsonMap map) {
    return map == null || map.size() == 0;
  }
}
