package com.segment.android;

import com.segment.android.json.JsonMap;
import java.util.Map;
import org.json.JSONObject;

import static com.segment.android.Utils.isNullOrEmpty;

public abstract class SegmentEntity<T extends SegmentEntity<T>> {
  private final JsonMap<Object> jsonMap;

  protected SegmentEntity() {
    jsonMap = JsonMap.create();
  }

  /** Constructs a new {@code Json} instance backed by the given map. */
  SegmentEntity(Map<String, Object> map) {
    this.jsonMap = JsonMap.wrap(map);
  }

  /**
   * Constructs a new {@code Json} from a json formatted string.
   *
   * @param json the json to parse from
   * @throws RuntimeException if the json could not be parsed.
   */
  SegmentEntity(String json) {
    jsonMap = JsonMap.decode(json);
  }

  /** For generics to support chaining in base classes. */
  protected abstract T self();

  // Put Methods
  /* We only need the ones that will sent to our servers. */

  public T put(String key, int value) {
    return putObject(key, value);
  }

  public T put(String key, long value) {
    return putObject(key, value);
  }

  public T put(String key, double value) {
    return putObject(key, value);
  }

  public T put(String key, boolean value) {
    return putObject(key, value);
  }

  public T put(String key, String value) {
    return putObject(key, value);
  }

  /**
   * Although any Map can be passed in, only primitives will be correctly serialized. For complex
   * objects, the toString method of the object will be used to serialize it, so when you retrieve
   * the object, you'll get it back as a String.
   */
  public T put(String key, Map<String, ?> value) {
    return putObject(key, value);
  }

  public T put(String key, SegmentEntity value) {
    return putObject(key, value);
  }

  /**
   * Private put method that delegates to the Json after asserting that the key is not null. Don't
   * expose this, to limit the types that can be inserted.
   */
  private T putObject(String key, Object value) {
    assertKeyNotNull(key);
    jsonMap.put(key, value);
    return self();
  }

  public Object get(String key) {
    assertKeyNotNull(key);
    return jsonMap.get(key);
  }

  public Integer getInteger(String key) {
    assertKeyNotNull(key);
    return jsonMap.getInteger(key);
  }

  public Long getLong(String key) {
    assertKeyNotNull(key);
    return jsonMap.getLong(key);
  }

  public Double getDouble(String key) {
    assertKeyNotNull(key);
    return jsonMap.getDouble(key);
  }

  public Boolean getBoolean(String key) {
    assertKeyNotNull(key);
    return jsonMap.getBoolean(key);
  }

  public String getString(String key) {
    assertKeyNotNull(key);
    return jsonMap.getString(key);
  }

  public JsonMap getJsonMap(String key) {
    assertKeyNotNull(key);
    return jsonMap.getJsonMap(key);
  }

  private void assertKeyNotNull(String key) {
    if (isNullOrEmpty(key)) {
      throw new IllegalArgumentException("Key must not be null.");
    }
  }

  @Override public String toString() {
    return jsonMap.toString();
  }

  public JSONObject toJsonObject() {
    return jsonMap.toJsonObject();
  }
}
