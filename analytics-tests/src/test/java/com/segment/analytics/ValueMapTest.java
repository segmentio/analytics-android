package com.segment.analytics;

import com.segment.analytics.core.tests.BuildConfig;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.PROJECT_SETTINGS_JSON_SAMPLE;
import static com.segment.analytics.internal.Utils.NullableConcurrentHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = NONE)
public class ValueMapTest {

  @Mock NullableConcurrentHashMap<String, Object> delegate;
  @Mock Object object;
  ValueMap valueMap;
  Cartographer cartographer;

  @Before public void setUp() {
    initMocks(this);
    valueMap = new ValueMap();
    cartographer = Cartographer.INSTANCE;
  }

  @Test public void disallowsNullMap() throws Exception {
    try {
      new ValueMap(null);
      fail("Null Map should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void emptyMap() throws Exception {
    assertThat(valueMap).hasSize(0).isEmpty();
  }

  @SuppressWarnings({ "ResultOfMethodCallIgnored", "SuspiciousMethodCalls" }) //
  @Test public void methodsAreForwardedCorrectly() {
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

    Map<String, Object> map = new LinkedHashMap<>();
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
    String json = cartographer.toJson(valueMap);
    // todo: the ordering may be different on different versions of Java
    assertThat(json).isIn("{\"value2\":\"VALUE2\",\"value1\":\"VALUE1\"}",
        "{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}");

    valueMap = new ValueMap(cartographer.fromJson("{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}"));
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
    assertThat(cartographer.toJson(valueMap)).isEqualTo("{\"nested\":{\"value\":\"box\"}}");

    valueMap = new ValueMap(cartographer.fromJson("{\"nested\":{\"value\":\"box\"}}"));
    assertThat(valueMap).hasSize(1).contains(MapEntry.entry("nested", nested));
  }

  @Test public void customJsonMapDeserialization() throws Exception {
    Settings settings = new Settings(cartographer.fromJson(PROJECT_SETTINGS_JSON_SAMPLE));
    assertThat(settings).hasSize(4)
        .containsKey("Amplitude")
        .containsKey("Segment")
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
    } catch (AssertionError exception) {
      assertThat(exception) //
          .hasMessage(
              "Could not create instance of com.segment.analytics.ValueMapTest.AmplitudeSettings.\n"
                  + "java.lang.NoSuchMethodException: "
                  + "com.segment.analytics.ValueMapTest$AmplitudeSettings.<init>(java.util.Map)");
    }
  }

  @Test public void projectSettings() throws Exception {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") ValueMap valueMap =
        new ValueMap(cartographer.fromJson(PROJECT_SETTINGS_JSON_SAMPLE));

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

  @Test public void toJsonObject() throws Exception {
    JSONObject jsonObject =
        new ValueMap(cartographer.fromJson(PROJECT_SETTINGS_JSON_SAMPLE)).toJsonObject();

    JSONObject amplitude = jsonObject.getJSONObject("Amplitude");
    assertThat(amplitude).isNotNull();
    assertThat(amplitude.length()).isEqualTo(4);
    assertThat(amplitude.getString("apiKey")).isEqualTo("ad3c426eb736d7442a65da8174bc1b1b");
    assertThat(amplitude.getBoolean("trackNamedPages")).isTrue();
    assertThat(amplitude.getBoolean("trackCategorizedPages")).isTrue();
    assertThat(amplitude.getBoolean("trackAllPages")).isFalse();

    JSONObject flurry = jsonObject.getJSONObject("Flurry");
    assertThat(flurry).isNotNull();
    assertThat(flurry.length()).isEqualTo(4);
    assertThat(flurry.getString("apiKey")).isEqualTo("8DY3D6S7CCWH54RBJ9ZM");
    assertThat(flurry.getBoolean("useHttps")).isTrue();
    assertThat(flurry.getBoolean("captureUncaughtExceptions")).isFalse();
    assertThat(flurry.getDouble("sessionContinueSeconds")).isEqualTo(10.0);
  }

  @Test public void toJsonObjectWithNullValue() throws Exception {
    ValueMap valueMap = new ValueMap();
    valueMap.put("foo", null);

    JSONObject jsonObject = valueMap.toJsonObject();
    assertThat(jsonObject.get("foo")).isEqualTo(JSONObject.NULL);
  }

  private enum MyEnum {
    VALUE1, VALUE2
  }

  static class Settings extends ValueMap {
    Settings(Map<String, Object> map) throws IOException {
      super(map);
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
      throw new AssertionError("string constuctors must not be called when deserializing");
    }
  }
}
