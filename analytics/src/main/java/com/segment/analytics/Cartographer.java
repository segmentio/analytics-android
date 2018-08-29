/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cartographer creates {@link Map} objects from JSON encoded streams and decodes {@link Map}
 * objects into JSON streams. Use {@link Builder} to construct instances.
 */
public class Cartographer {
  static final Cartographer INSTANCE = new Builder().lenient(true).prettyPrint(false).build();

  private final boolean isLenient;
  private final boolean prettyPrint;

  Cartographer(boolean isLenient, boolean prettyPrint) {
    this.isLenient = isLenient;
    this.prettyPrint = prettyPrint;
  }

  /**
   * Deserializes the specified json into a {@link Map}. If you have the Json in a {@link Reader}
   * form instead of a {@link String}, use {@link #fromJson(Reader)} instead.
   */
  public Map<String, Object> fromJson(String json) throws IOException {
    if (json == null) {
      throw new IllegalArgumentException("json == null");
    }
    if (json.length() == 0) {
      throw new IllegalArgumentException("json empty");
    }
    return fromJson(new StringReader(json));
  }

  /**
   * Deserializes the json read from the specified {@link Reader} into a {@link Map}. If you have
   * the Json in a String form instead of a {@link Reader}, use {@link #fromJson(String)} instead.
   */
  public Map<String, Object> fromJson(Reader reader) throws IOException {
    if (reader == null) {
      throw new IllegalArgumentException("reader == null");
    }
    JsonReader jsonReader = new JsonReader(reader);
    jsonReader.setLenient(isLenient);
    try {
      return readerToMap(jsonReader);
    } finally {
      reader.close();
    }
  }

  /**
   * Serializes the map into it's json representation and returns it as a String. If you want to
   * write the json to {@link Writer} instead of retrieving it as a String, use {@link #toJson(Map,
   * Writer)} instead.
   */
  public String toJson(Map<?, ?> map) {
    StringWriter stringWriter = new StringWriter();
    try {
      toJson(map, stringWriter);
    } catch (IOException e) {
      throw new AssertionError(e); // No I/O writing to a Buffer.
    }
    return stringWriter.toString();
  }

  /**
   * Serializes the map into it's json representation into the provided {@link Writer}. If you want
   * to retrieve the json as a string, use {@link #toJson(Map)} instead.
   */
  public void toJson(Map<?, ?> map, Writer writer) throws IOException {
    if (map == null) {
      throw new IllegalArgumentException("map == null");
    }
    if (writer == null) {
      throw new IllegalArgumentException("writer == null");
    }

    JsonWriter jsonWriter = new JsonWriter(writer);
    jsonWriter.setLenient(isLenient);
    if (prettyPrint) {
      jsonWriter.setIndent("  ");
    }
    try {
      mapToWriter(map, jsonWriter);
    } finally {
      jsonWriter.close();
    }
  }

  // Decoding

  /** Reads the {@link JsonReader} into a {@link Map}. */
  private static Map<String, Object> readerToMap(JsonReader reader) throws IOException {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    reader.beginObject();
    while (reader.hasNext()) {
      map.put(reader.nextName(), readValue(reader));
    }
    reader.endObject();
    return map;
  }

  /** Reads the {@link JsonReader} into a {@link List}. */
  private static List<Object> readerToList(JsonReader reader) throws IOException {
    // todo: try to infer the type of the List?
    List<Object> list = new ArrayList<Object>();
    reader.beginArray();
    while (reader.hasNext()) {
      list.add(readValue(reader));
    }
    reader.endArray();
    return list;
  }

  /** Reads the next value in the {@link JsonReader}. */
  private static Object readValue(JsonReader reader) throws IOException {
    JsonToken token = reader.peek();
    switch (token) {
      case BEGIN_OBJECT:
        return readerToMap(reader);
      case BEGIN_ARRAY:
        return readerToList(reader);
      case BOOLEAN:
        return reader.nextBoolean();
      case NULL:
        reader.nextNull(); // consume the null token
        return null;
      case NUMBER:
        return reader.nextDouble();
      case STRING:
        return reader.nextString();
      default:
        throw new IllegalStateException("Invalid token " + token);
    }
  }

  // Encoding

  /** Encode the given {@link Map} into the {@link JsonWriter}. */
  private static void mapToWriter(Map<?, ?> map, JsonWriter writer) throws IOException {
    writer.beginObject();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      writer.name(String.valueOf(entry.getKey()));
      writeValue(entry.getValue(), writer);
    }
    writer.endObject();
  }

  /** Print the json representation of a List to the given writer. */
  private static void listToWriter(List<?> list, JsonWriter writer) throws IOException {
    writer.beginArray();
    for (Object value : list) {
      writeValue(value, writer);
    }
    writer.endArray();
  }

  /**
   * Print the json representation of an array to the given writer. Primitive arrays cannot be cast
   * to Object[], to this method accepts the raw object and uses {@link Array#getLength(Object)} and
   * {@link Array#get(Object, int)} to read the array.
   */
  private static void arrayToWriter(Object array, JsonWriter writer) throws IOException {
    writer.beginArray();
    for (int i = 0, size = Array.getLength(array); i < size; i++) {
      writeValue(Array.get(array, i), writer);
    }
    writer.endArray();
  }

  /**
   * Writes the given {@link Object} to the {@link JsonWriter}.
   *
   * @throws IOException
   */
  private static void writeValue(Object value, JsonWriter writer) throws IOException {
    if (value == null) {
      writer.nullValue();
    } else if (value instanceof Number) {
      writer.value((Number) value);
    } else if (value instanceof Boolean) {
      writer.value((Boolean) value);
    } else if (value instanceof List) {
      listToWriter((List) value, writer);
    } else if (value instanceof Map) {
      mapToWriter((Map) value, writer);
    } else if (value.getClass().isArray()) {
      arrayToWriter(value, writer);
    } else {
      writer.value(String.valueOf(value));
    }
  }

  /** Fluent API to construct instances of {@link Cartographer}. */
  public static class Builder {
    private boolean isLenient;
    private boolean prettyPrint;

    /**
     * Configure this parser to be be liberal in what it accepts. By default, this parser is strict
     * and only accepts JSON as specified by <a href="http://www.ietf.org/rfc/rfc4627.txt">RFC
     * 4627</a>. See {@link JsonReader#setLenient(boolean)} for more details.
     * </ul>
     */
    public Builder lenient(boolean isLenient) {
      this.isLenient = isLenient;
      return this;
    }

    /**
     * Configures Cartographer to output Json that fits in a page for pretty printing. This option
     * only affects Json serialization.
     */
    public Builder prettyPrint(boolean prettyPrint) {
      this.prettyPrint = prettyPrint;
      return this;
    }

    public Cartographer build() {
      return new Cartographer(isLenient, prettyPrint);
    }
  }
}
