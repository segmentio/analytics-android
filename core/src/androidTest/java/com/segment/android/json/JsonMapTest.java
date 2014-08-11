package com.segment.android.json;

import com.segment.android.BaseAndroidTestCase;
import org.fest.assertions.data.MapEntry;

import static org.fest.assertions.api.Assertions.assertThat;

public class JsonMapTest extends BaseAndroidTestCase {
  public void testDisallowsNullMap() throws Exception {
    try {
      JsonMap.wrap(null);
      fail("Null Map should throw exception.");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testStringToNumber() throws Exception {
    JsonMap<Object> map = JsonMap.create();

    map.put("double_pi", Math.PI);
    map.put("string_pi", String.valueOf(Math.PI));    // Put a double as a string

    assertThat(map).contains(MapEntry.entry("string_pi", String.valueOf(Math.PI)))
        .contains(MapEntry.entry("double_pi", Math.PI));

    assertThat(map.getDouble("string_pi")).isEqualTo(Math.PI);
    assertThat(map.get("double_pi")).isEqualTo(Math.PI);

    assertThat(map.getInteger("string_pi")).isNull();
    assertThat(map.getInteger("double_pi")).isNull();
  }

  // todo: test to JsonObject
}
