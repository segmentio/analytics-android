package com.segment.android;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * A base class that encapsulates Json functionality. Specifically it exposes constructor that takes
 * in a Json string, and it's toString will return a json formatted string.
 */
public abstract class Json<T extends Json<T>> {
  private final Map<String, Object> map;

  public static class JsonConversionException extends RuntimeException {
    public JsonConversionException(String detailMessage, Throwable throwable) {
      super(detailMessage, throwable);
    }
  }

  /** Simplest implementation of Json for public API. */
  private static class Simple extends Json<Simple> {
    Simple() {
    }

    Simple(int initialCapacity) {
      super(initialCapacity);
    }

    Simple(Map<String, Object> map) {
      super(map);
    }

    Simple(String json) {
      super(json);
    }

    @Override protected Simple self() {
      return this;
    }
  }

  /** Create a new empty {@code Json} instance. */
  public static Json create() {
    return new Simple();
  }

  /**
   * Creates a new {@code Json} instance with the specified capacity.
   *
   * @param initialCapacity the initial capacity of this json.
   * @throws IllegalArgumentException when the capacity is less than zero.
   */
  public static Json create(int initialCapacity) {
    return new Simple(initialCapacity);
  }

  /**
   * Constructs a new {@code Json} from a json formatted string.
   *
   * @param json the json to parse from
   * @throws RuntimeException if the json could not be parsed.
   */
  public static Json create(String json) {
    return new Simple(json);
  }

  /** Creates a new {@code Json} instance backed by the given map. */
  public static Json create(Map<String, Object> map) {
    return new Simple(map);
  }

  /** Constructs a new empty {@code Json} instance. */
  Json() {
    map = new LinkedHashMap<String, Object>();
  }

  /**
   * Constructs a new {@code Json} instance with the specified capacity.
   *
   * @param initialCapacity the initial capacity of this json.
   * @throws IllegalArgumentException when the capacity is less than zero.
   */
  Json(int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException(
          "Initial capacity (" + initialCapacity + ") must not be less than zero.");
    }
    map = new LinkedHashMap<String, Object>(initialCapacity);
  }

  /** Constructs a new {@code Json} instance backed by the given map. */
  Json(Map<String, Object> map) {
    this.map = map;
  }

  /**
   * Constructs a new {@code Json} from a json formatted string.
   *
   * @param json the json to parse from
   * @throws JsonConversionException if the json could not be parsed.
   */
  Json(String json) {
    try {
      JSONObject jsonObject = new JSONObject(json);
      map = toMap(jsonObject);
    } catch (JSONException e) {
      throw new JsonConversionException("Could not parse json.", e);
    }
  }

  // Put Methods
  public T put(String key, int value) {
    return putObject(key, value);
  }

  public int getInt(String key) {
    return (Integer) get(key);
  }

  public T put(String key, short value) {
    return putObject(key, value);
  }

  public short getShort(String key) {
    return (Short) get(key);
  }

  public T put(String key, long value) {
    return putObject(key, value);
  }

  public long getLong(String key) {
    return (Long) get(key);
  }

  public T put(String key, double value) {
    return putObject(key, value);
  }

  public double getDouble(String key) {
    return (Double) get(key);
  }

  public T put(String key, float value) {
    return putObject(key, value);
  }

  public float getFloat(String key) {
    return (Float) get(key);
  }

  public T put(String key, boolean value) {
    return putObject(key, value);
  }

  public boolean getBoolean(String key) {
    return (Boolean) get(key);
  }

  public T put(String key, String value) {
    return putObject(key, value);
  }

  public String getString(String key) {
    return (String) get(key);
  }

  public T put(String key, Json<?> value) {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }
    return put(key, value.map);
  }

  public Json getJson(String key) {
    Object value = get(key);
    if (value == null) {
      return create();
    } else if (value instanceof Json) {
      return (Json) value;
    } else if (value instanceof Map) {
      return create((Map) get(key));
    } else {
      throw new ClassCastException("Could not cast " + value + " to Json or Map");
    }
  }

  public T put(String key, Map<String, ?> value) {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }
    return putObject(key, value);
  }

  public Map<String, ?> getMap(String key) {
    return (Map) get(key);
  }

  public T put(String key, Enum<?> anEnum) {
    return putObject(key, anEnum);
  }

  public Enum<?> getEnum(String key) {
    return (Enum) get(key);
  }

  protected abstract T self();

  // Private put method, don't expose this since we want to limit types that can be inserted.
  private T putObject(String key, Object value) {
    assertKeyNotNull(key);
    map.put(key, value);
    return self();
  }

  /**
   * Returns the value of the mapping with the specified key.
   *
   * @param key the key.
   * @return the value of the mapping with the specified key, or {@code null} if no mapping for the
   * specified key is found.
   *
   * @throws NullPointerException if the key is {@code null}.
   */
  public Object get(String key) {
    assertKeyNotNull(key);
    return map.get(key);
  }

  public boolean has(String key) {
    assertKeyNotNull(key);
    return map.get(key) != null;
  }

  private void assertKeyNotNull(String key) {
    if (Utils.isNullOrEmpty(key)) {
      throw new IllegalArgumentException("Key must not be null.");
    }
  }

  public JSONObject toJsonObject() {
    return new JSONObject(map);
  }

  public int size() {
    return map.size();
  }

  public static boolean isNullOrEmpty(Json json) {
    return json == null || json.size() == 0;
  }

  @Override public boolean equals(Object o) {
    return o == this || o instanceof Json && map.equals(((Json) o).map);
  }

  @Override public int hashCode() {
    return map.hashCode();
  }

  @Override public String toString() {
    try {
      // Support proper parsing. The default implementation ignores Arrays, Collections and Maps.
      JSONStringer stringer = new JSONStringer();
      writeTo(map, stringer);
      return stringer.toString();
    } catch (JSONException e) {
      return null;
    }
  }

  /* Recursively write to the stringer for a given map. */
  static void writeTo(Map<String, Object> map, JSONStringer stringer) throws JSONException {
    stringer.object();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value == null) {
        stringer.key(key).value(value);
      } else if (value instanceof Map) {
        stringer.key(key);
        writeTo((Map) value, stringer);
      } else if (value instanceof Collection) {
        writeTo((Collection) value, stringer);
      } else if (value.getClass().isArray()) {
        writeTo(toList(value), stringer);
      } else {
        stringer.key(key).value(value);
      }
    }
    stringer.endObject();
  }

  static List<Object> toList(Object array) {
    final int length = Array.getLength(array);
    List<Object> values = new ArrayList<Object>(length);
    for (int i = 0; i < length; ++i) {
      values.add(Array.get(array, i));
    }
    return values;
  }

  static void writeTo(Collection<Object> collection, JSONStringer stringer) throws JSONException {
    stringer.array();
    for (Object value : collection) {
      if (value instanceof Map) {
        writeTo((Map) value, stringer);
      } else {
        stringer.value(value);
      }
    }
    stringer.endArray();
  }

  static Map<String, Object> toMap(JSONObject jsonObject) throws JSONException {
    Map<String, Object> map = new LinkedHashMap<String, Object>(jsonObject.length());

    Iterator<String> keysItr = jsonObject.keys();
    while (keysItr.hasNext()) {
      String key = keysItr.next();
      Object value = jsonObject.get(key);
      if (value instanceof JSONArray) {
        value = toList((JSONArray) value);
      } else if (value instanceof JSONObject) {
        value = toMap((JSONObject) value);
      }
      map.put(key, value);
    }
    return map;
  }

  static List<Object> toList(JSONArray array) throws JSONException {
    List<Object> list = new ArrayList<Object>();
    for (int i = 0; i < array.length(); i++) {
      list.add(array.get(i));
    }
    return list;
  }
}
