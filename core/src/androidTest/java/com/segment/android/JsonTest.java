package com.segment.android;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

import static org.fest.assertions.api.Assertions.assertThat;

public class JsonTest extends BaseAndroidTestCase {
  public void testMap() throws Exception {
    Map<String, Object> map = new LinkedHashMap<String, Object>(5);
    map.put("name", "Adam");
    map.put("id", 34);
    map.put("weight", 82.435d);
    map.put("height", 5.3f);
    map.put("single", true);
    map.put("age", 34);
    String expectedJson =
        "{\"id\":34,\"single\":true,\"height\":5.3,\"weight\":82.435,\"age\":34,\"name\":\"Adam\"}";
    assertThat(new JSONObject(map).toString()).isEqualTo(expectedJson);
  }
}
