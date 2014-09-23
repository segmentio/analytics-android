/*
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.segment.analytics.Utils.isNullOrEmpty;

class JsonUtils {
  /** Converts the given json formatted string into a map. */
  static Map<String, Object> jsonToMap(String json) throws IOException {
    if (isNullOrEmpty(json)) {
      throw new IllegalArgumentException("Json must not be null or empty.");
    }
    JsonReader jsonReader =
        new JsonReader(new InputStreamReader(new ByteArrayInputStream(json.getBytes())));
    return readerToMap(jsonReader);
  }

  /** Converts the given string into a List. */
  static List<Object> jsonToList(String json) throws IOException {
    if (isNullOrEmpty(json)) {
      throw new IllegalArgumentException("Json must not be null or empty.");
    }
    JsonReader jsonReader =
        new JsonReader(new InputStreamReader(new ByteArrayInputStream(json.getBytes())));
    return readerToList(jsonReader);
  }

  /** Converts the given map to a json format string. */
  static String mapToJson(Map<?, ?> map) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    JsonWriter writer = new JsonWriter(new OutputStreamWriter(bos));
    mapToWriter(map, writer);
    writer.close();
    return bos.toString();
  }

  /** Converts the given list to a json format string. */
  static String listToJson(List<?> list) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    JsonWriter writer = new JsonWriter(new OutputStreamWriter(bos));
    listToWriter(list, writer);
    writer.close();
    return bos.toString();
  }

  // Private APIs

  /** Parse a json reader into a map. */
  private static Map<String, Object> readerToMap(JsonReader reader) throws IOException {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    reader.beginObject();
    while (reader.hasNext()) {
      String key = reader.nextName();
      map.put(key, readValue(reader));
    }
    reader.endObject();
    return map;
  }

  /** Parse a json reader into a list. */
  private static List<Object> readerToList(JsonReader reader) throws IOException {
    List<Object> list = new ArrayList<Object>();

    reader.beginArray();
    while (reader.hasNext()) {
      Object value = readValue(reader);
      list.add(value);
    }
    reader.endArray();

    return list;
  }

  /** Print the json representation of a map to the given writer. */
  private static void mapToWriter(Map<?, ?> map, JsonWriter writer) throws IOException {
    writer.beginObject();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      writer.name(String.valueOf(key));
      write(value, writer);
    }
    writer.endObject();
  }

  /** Print the json representation of a List to the given writer. */
  private static void listToWriter(List<?> list, JsonWriter writer) throws IOException {
    writer.beginArray();
    for (Object value : list) {
      write(value, writer);
    }
    writer.endArray();
  }

  private static void write(Object value, JsonWriter writer) throws IOException {
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
    } else {
      writer.value(String.valueOf(value));
    }
  }

  private static Object readValue(JsonReader reader) throws IOException {
    JsonToken token = reader.peek();
    switch (token) {
      case BEGIN_OBJECT:
        return readerToMap(reader);
      case BEGIN_ARRAY:
        return readerToList(reader);
      case NUMBER:
        return reader.nextDouble();
      case STRING:
        return reader.nextString();
      case BOOLEAN:
        return reader.nextBoolean();
      case NULL:
        reader.nextNull();
        return null;
      default:
        throw new IllegalStateException("Got token " + token);
    }
  }
}
