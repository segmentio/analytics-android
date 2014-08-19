package com.segment.android.json;

import com.segment.android.BaseAndroidTestCase;
import java.util.Map;
import org.fest.assertions.data.MapEntry;

import static org.fest.assertions.api.Assertions.assertThat;

public class JsonMapTest extends BaseAndroidTestCase {
  public void testDisallowsNullMap() throws Exception {
    try {
      new JsonMap((Map) null);
      fail("Null Map should throw exception.");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testStringToNumber() throws Exception {
    JsonMap map = new JsonMap();

    map.put("double_pi", Math.PI);
    map.put("string_pi", String.valueOf(Math.PI));    // Put a double as a string

    assertThat(map).contains(MapEntry.entry("string_pi", String.valueOf(Math.PI)))
        .contains(MapEntry.entry("double_pi", Math.PI));

    assertThat(map.getDouble("string_pi")).isEqualTo(Math.PI);
    assertThat(map.get("double_pi")).isEqualTo(Math.PI);

    assertThat(map.getInteger("string_pi")).isNull();
    assertThat(map.getInteger("double_pi")).isEqualTo(3);
  }

  public void testSettings() throws Exception {
    String json =
        "{\"Amplitude\":{\"trackNamedPages\":true,\"trackCategorizedPages\":true,\"trackAllPages\":false,\"apiKey\":\"ad3c426eb736d7442a65da8174bc1b1b\"},\"Flurry\":{\"apiKey\":\"8DY3D6S7CCWH54RBJ9ZM\",\"captureUncaughtExceptions\":false,\"useHttps\":true,\"sessionContinueSeconds\":10},\"Mixpanel\":{\"people\":true,\"token\":\"f7afe0cb436685f61a2b203254779e02\",\"trackAllPages\":false,\"trackCategorizedPages\":true,\"trackNamedPages\":true,\"increments\":[],\"legacySuperProperties\":false},\"Segment.io\":{\"apiKey\":\"l8v1ga655b\"}}";
    JsonMap jsonMap = new JsonMap(json);

    assertThat(jsonMap.getJsonMap("Amplitude")).isNotNull()
        .hasSize(4)
        .containsKey("trackNamedPages")
        .containsKey("trackCategorizedPages");
  }

  // todo: test to JsonObject
}
