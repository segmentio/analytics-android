package com.segment.analytics.json;

import com.segment.analytics.BaseAndroidTestCase;
import java.util.Map;
import org.fest.assertions.data.MapEntry;

import static org.fest.assertions.api.Assertions.assertThat;

public class JsonMapTest extends BaseAndroidTestCase {
  JsonMap jsonMap;

  @Override public void setUp() throws Exception {
    super.setUp();
    jsonMap = new JsonMap();
  }

  public void testDisallowsNullMap() throws Exception {
    try {
      new JsonMap((Map) null);
      fail("Null Map should throw exception.");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testEmptyMap() throws Exception {
    assertThat(jsonMap).hasSize(0).isEmpty();

    assertThat(jsonMap.get("foo")).isNull();
    assertThat(jsonMap.getDouble("foo")).isNull();
    assertThat(jsonMap.getString("foo")).isNull();
    assertThat(jsonMap.getBoolean("foo")).isNull();
    assertThat(jsonMap.getInteger("foo")).isNull();
    assertThat(jsonMap.getLong("foo")).isNull();
    assertThat(jsonMap.getChar("foo")).isNull();
    assertThat(jsonMap.getEnum(MyEnum.class, "foo")).isNull();
  }

  public void testConversionsAreCached() throws Exception {
    String stringPi = String.valueOf(Math.PI);

    jsonMap.put("double_pi", Math.PI);
    assertThat(jsonMap).contains(MapEntry.entry("double_pi", Math.PI));
    assertThat(jsonMap.getString("double_pi")).isEqualTo(stringPi);
    assertThat(jsonMap).contains(MapEntry.entry("double_pi", stringPi));

    jsonMap.put("string_pi", stringPi);
    assertThat(jsonMap).contains(MapEntry.entry("string_pi", stringPi));
    assertThat(jsonMap.getDouble("string_pi")).isEqualTo(Math.PI);
    assertThat(jsonMap).contains(MapEntry.entry("string_pi", Math.PI));
  }

  private static enum MyEnum {
    VALUE1, VALUE2
  }

  public void testEnums() throws Exception {
    jsonMap.put("value1", MyEnum.VALUE1);
    jsonMap.put("value2", MyEnum.VALUE2);
    String json = jsonMap.toString();
    assertThat(json).isEqualTo("{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}");

    jsonMap = new JsonMap("{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}");
    assertThat(jsonMap) //
        .contains(MapEntry.entry("value1", "VALUE1")) //
        .contains(MapEntry.entry("value1", "VALUE2"));
    assertThat(jsonMap.getEnum(MyEnum.class, "value1")).isEqualTo(MyEnum.VALUE1);
    assertThat(jsonMap.getEnum(MyEnum.class, "value2")).isEqualTo(MyEnum.VALUE2);
    assertThat(jsonMap) //
        .contains(MapEntry.entry("value1", MyEnum.VALUE1))
        .contains(MapEntry.entry("value2", MyEnum.VALUE2));
  }

  public void testNestedMaps() throws Exception {
    JsonMap nested = new JsonMap();
    nested.put("value", "box");
    jsonMap.put("nested", nested);

    assertThat(jsonMap).hasSize(1).contains(MapEntry.entry("nested", nested));
    assertThat(jsonMap.toString()).isEqualTo("{\"nested\":{\"value\":\"box\"}}");

    jsonMap = new JsonMap("{\"nested\":{\"value\":\"box\"}}");
    assertThat(jsonMap).hasSize(1).contains(MapEntry.entry("nested", nested));
  }

  public void testSettings() throws Exception {
    String json =
        "{\"Amplitude\":{\"trackNamedPages\":true,\"trackCategorizedPages\":true,\"trackAllPages\":false,\"apiKey\":\"ad3c426eb736d7442a65da8174bc1b1b\"},\"Flurry\":{\"apiKey\":\"8DY3D6S7CCWH54RBJ9ZM\",\"captureUncaughtExceptions\":false,\"useHttps\":true,\"sessionContinueSeconds\":10},\"Mixpanel\":{\"people\":true,\"token\":\"f7afe0cb436685f61a2b203254779e02\",\"trackAllPages\":false,\"trackCategorizedPages\":true,\"trackNamedPages\":true,\"increments\":[],\"legacySuperProperties\":false},\"Segment.io\":{\"apiKey\":\"l8v1ga655b\"}}";
    JsonMap jsonMap = new JsonMap(json);

    assertThat(jsonMap.getJsonMap("Amplitude")).isNotNull()
        .hasSize(4)
        .contains(MapEntry.entry("apiKey", "ad3c426eb736d7442a65da8174bc1b1b"))
        .contains(MapEntry.entry("trackNamedPages", true))
        .contains(MapEntry.entry("trackCategorizedPages", true))
        .contains(MapEntry.entry("trackAllPages", false));
    assertThat(jsonMap.getJsonMap("Flurry")).isNotNull()
        .hasSize(4)
        .contains(MapEntry.entry("apiKey", "8DY3D6S7CCWH54RBJ9ZM"))
        .contains(MapEntry.entry("captureUncaughtExceptions", false))
        .contains(MapEntry.entry("useHttps", true))
        .contains(MapEntry.entry("sessionContinueSeconds", 10));
  }
}
