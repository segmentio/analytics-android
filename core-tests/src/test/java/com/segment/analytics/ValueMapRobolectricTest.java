package com.segment.analytics;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.PROJECT_SETTINGS_JSON_SAMPLE;
import static com.segment.analytics.Utils.NullableConcurrentHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class ValueMapRobolectricTest {
  @Mock NullableConcurrentHashMap<String, Object> delegate;
  @Mock Object object;
  ValueMap valueMap;

  @Before public void setUp() {
    initMocks(this);
    valueMap = new ValueMap();
  }

  @Test public void disallowsNullMap() throws Exception {
    try {
      new ValueMap((Map) null);
      fail("Null Map should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void emptyMap() throws Exception {
    assertThat(valueMap).hasSize(0).isEmpty();
  }

  @Test public void methodsAreForwardedCorrectly() throws Exception {
    valueMap = new ValueMap(delegate);

    valueMap.clear();
    verify(delegate).clear();

    valueMap.containsKey(object);
    verify(delegate).containsKey(object);

    valueMap.entrySet();
    verify(delegate).entrySet();

    valueMap.get(object);
    verify(delegate).get(object);

    valueMap.isEmpty();
    verify(delegate).isEmpty();

    valueMap.keySet();
    verify(delegate).keySet();

    valueMap.put("foo", object);
    verify(delegate).put("foo", object);

    Map<String, Object> map = new LinkedHashMap<String, Object>();
    valueMap.putAll(map);
    verify(delegate).putAll(map);

    valueMap.remove(object);
    verify(delegate).remove(object);

    valueMap.size();
    verify(delegate).size();

    valueMap.values();
    verify(delegate).values();

    valueMap.putValue("bar", object);
    verify(delegate).put("bar", object);
  }

  @Test public void simpleConversions() throws Exception {
    String stringPi = String.valueOf(Math.PI);

    valueMap.put("double_pi", Math.PI);
    assertThat(valueMap.getString("double_pi")).isEqualTo(stringPi);

    valueMap.put("string_pi", stringPi);
    assertThat(valueMap.getDouble("string_pi", 0)).isEqualTo(Math.PI);
  }

  @Test public void enumDeserialization() throws Exception {
    valueMap.put("value1", MyEnum.VALUE1);
    valueMap.put("value2", MyEnum.VALUE2);
    String json = JsonUtils.mapToJson(valueMap);
    // todo: the ordering may be different on different versions of Java
    assertThat(json).isIn("{\"value2\":\"VALUE2\",\"value1\":\"VALUE1\"}",
        "{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}");

    valueMap = new ValueMap(JsonUtils.jsonToMap("{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}"));
    assertThat(valueMap) //
        .contains(MapEntry.entry("value1", "VALUE1")) //
        .contains(MapEntry.entry("value2", "VALUE2"));
    assertThat(valueMap.getEnum(MyEnum.class, "value1")).isEqualTo(MyEnum.VALUE1);
    assertThat(valueMap.getEnum(MyEnum.class, "value2")).isEqualTo(MyEnum.VALUE2);
  }

  @Test public void allowsNullValues() {
    valueMap.put(null, "foo");
    valueMap.put("foo", null);
  }

  @Test public void nestedMaps() throws Exception {
    ValueMap nested = new ValueMap();
    nested.put("value", "box");
    valueMap.put("nested", nested);

    assertThat(valueMap).hasSize(1).contains(MapEntry.entry("nested", nested));
    assertThat(JsonUtils.mapToJson(valueMap)).isEqualTo("{\"nested\":{\"value\":\"box\"}}");

    valueMap = new ValueMap(JsonUtils.jsonToMap("{\"nested\":{\"value\":\"box\"}}"));
    assertThat(valueMap).hasSize(1).contains(MapEntry.entry("nested", nested));
  }

  @Test public void customJsonMapDeserialization() throws Exception {
    Settings settings = new Settings(PROJECT_SETTINGS_JSON_SAMPLE);
    assertThat(settings).hasSize(4)
        .containsKey("Amplitude")
        .containsKey("Segment.io")
        .containsKey("Flurry")
        .containsKey("Mixpanel");

    // Map Constructor
    MixpanelSettings mixpanelSettings = settings.getMixpanelSettings();
    assertThat(mixpanelSettings) //
        .contains(MapEntry.entry("token", "f7afe0cb436685f61a2b203254779e02"))
        .contains(MapEntry.entry("people", true))
        .contains(MapEntry.entry("trackNamedPages", true))
        .contains(MapEntry.entry("trackCategorizedPages", true))
        .contains(MapEntry.entry("trackAllPages", false));

    try {
      settings.getAmplitudeSettings();
    } catch (AssertionError error) {
      assertThat(error).hasMessageContaining("Could not find map constructor for");
    }
  }

  @Test public void projectSettings() throws Exception {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") ValueMap valueMap =
        new ValueMap(JsonUtils.jsonToMap(PROJECT_SETTINGS_JSON_SAMPLE));

    assertThat(valueMap.getValueMap("Amplitude")).isNotNull()
        .hasSize(4)
        .contains(MapEntry.entry("apiKey", "ad3c426eb736d7442a65da8174bc1b1b"))
        .contains(MapEntry.entry("trackNamedPages", true))
        .contains(MapEntry.entry("trackCategorizedPages", true))
        .contains(MapEntry.entry("trackAllPages", false));
    assertThat(valueMap.getValueMap("Flurry")).isNotNull()
        .hasSize(4)
        .contains(MapEntry.entry("apiKey", "8DY3D6S7CCWH54RBJ9ZM"))
        .contains(MapEntry.entry("captureUncaughtExceptions", false))
        .contains(MapEntry.entry("useHttps", true))
        .contains(MapEntry.entry("sessionContinueSeconds", 10.0));
  }

  private static enum MyEnum {
    VALUE1, VALUE2
  }

  static class Settings extends ValueMap {
    Settings(String json) throws IOException {
      super(JsonUtils.jsonToMap(json));
    }

    AmplitudeSettings getAmplitudeSettings() {
      return getValueMap("Amplitude", AmplitudeSettings.class);
    }

    MixpanelSettings getMixpanelSettings() {
      return getValueMap("Mixpanel", MixpanelSettings.class);
    }
  }

  static class MixpanelSettings extends ValueMap {
    MixpanelSettings(Map<String, Object> delegate) {
      super(delegate);
    }
  }

  static class AmplitudeSettings extends ValueMap {
    AmplitudeSettings(String json) throws IOException {
      super(JsonUtils.jsonToMap(json));
    }
  }
}
