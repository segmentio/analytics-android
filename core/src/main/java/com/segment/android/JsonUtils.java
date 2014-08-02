package com.segment.android;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class JsonUtils {
  private JsonUtils() {
    throw new AssertionError("No instances.");
  }

  static Map<String, Object> toMap(JSONObject object) throws JSONException {
    Map<String, Object> map = new LinkedHashMap<String, Object>(object.length());

    Iterator<String> keysItr = object.keys();
    while (keysItr.hasNext()) {
      String key = keysItr.next();
      Object value = object.get(key);
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

  static String jsonString(Map<String, Object> map) {
    return new JSONObject(map).toString();
  }
}
