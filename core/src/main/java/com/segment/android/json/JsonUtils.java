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

package com.segment.android.json;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import static com.segment.android.internal.Utils.isNullOrEmpty;

class JsonUtils {

  /**
   * Converts the given json into a map. The Map will contain values for primitive types and
   * Strings, or recursively Maps and Lists for the same types.
   *
   * Some type information is lost during deserialization. See the chart below to see which types
   * exhibit this behaviour.
   *
   * byte -> integer
   * short -> integer
   * integer -> integer
   * long -> long
   * float -> double
   * double -> double
   * char -> String
   * String -> String
   * boolean -> boolean
   */
  static Map<String, Object> toMap(String json) throws JsonConversionException {
    try {
      JSONObject jsonObject = new JSONObject(json);
      return toMap(jsonObject);
    } catch (JSONException e) {
      throw new JsonConversionException("Could not convert json to a map: " + json, e);
    }
  }

  /** Converts the given string into a list. */
  static JsonList toList(String string) throws JsonConversionException {
    try {
      JSONArray jsonArray = new JSONArray(string);
      return toList(jsonArray);
    } catch (JSONException e) {
      throw new JsonConversionException("Could not parse string as json array: " + string, e);
    }
  }

  /** Convert an JSONObject to a Map recursively. */
  private static Map<String, Object> toMap(JSONObject jsonObject) throws JSONException {
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

  /** Convert an JSONArray to a List recursively. */
  private static JsonList toList(JSONArray array) throws JSONException {
    JsonList list = new JsonList();
    for (int i = 0; i < array.length(); i++) {
      Object value = array.get(i);
      if (value instanceof JSONObject) {
        value = toMap((JSONObject) value);
      }
      list.add(value);
    }
    return list;
  }

  /** Converts the given map to a json formatted string. */
  static String fromMap(Map<String, ?> map) throws JsonConversionException {
    try {
      // Support proper parsing. The default implementation ignores nested Collections
      JSONStringer stringer = new JSONStringer();
      writeTo(map, stringer);
      return stringer.toString();
    } catch (JSONException e) {
      throw new JsonConversionException("Could not convert Map to json string", e);
    }
  }

  /** Recursively write to the stringer for a given map. */
  private static void writeTo(Map<String, ?> map, JSONStringer stringer) throws JSONException {
    stringer.object();
    for (Map.Entry<String, ?> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value == null) {
        stringer.key(key).value(value);
      } else if (value instanceof Map) {
        stringer.key(key);
        writeTo((Map) value, stringer);
      } else if (value instanceof Collection) {
        stringer.key(key);
        writeTo((Collection) value, stringer);
      } else if (value.getClass().isArray()) {
        stringer.key(key);
        writeTo(toList(value), stringer);
      } else {
        stringer.key(key).value(value);
      }
    }
    stringer.endObject();
  }

  /** Coerce an object which is an array to a List. */
  private static JsonList toList(Object array) {
    final int length = Array.getLength(array);
    JsonList values = new JsonList();
    for (int i = 0; i < length; ++i) {
      // don't worry about checking types, we'll do that when we write to the stringer
      values.add(Array.get(array, i));
    }
    return values;
  }

  /** Recursively write to the stringer for a given collection. */
  private static void writeTo(Collection<?> collection, JSONStringer stringer)
      throws JSONException {
    stringer.array();
    if (!isNullOrEmpty(collection)) {
      for (Object value : collection) {
        if (value instanceof Map) {
          writeTo((Map) value, stringer);
        } else {
          stringer.value(value);
        }
      }
    }
    stringer.endArray();
  }

  /** Converts the given map to a json formatted string. */
  static String fromList(List<?> list) throws JsonConversionException {
    try {
      // Support proper parsing. The default implementation ignores nested Collections
      JSONStringer stringer = new JSONStringer();
      writeTo(list, stringer);
      return stringer.toString();
    } catch (JSONException e) {
      throw new JsonConversionException("Could not convert List to json string", e);
    }
  }
}
