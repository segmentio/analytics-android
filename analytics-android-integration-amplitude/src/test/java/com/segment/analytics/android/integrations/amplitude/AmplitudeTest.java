package com.segment.analytics.android.integrations.amplitude;

import android.app.Application;

import com.amplitude.api.AmplitudeClient;
import com.amplitude.api.Identify;
import com.amplitude.api.Revenue;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import com.segment.analytics.test.TrackPayloadBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.Utils.createTraits;
import static com.segment.analytics.android.integrations.amplitude.AmplitudeIntegration.getStringSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AmplitudeTest {

  @Mock Application application;
  @Mock AmplitudeClient amplitude;
  @Mock Analytics analytics;

  private AmplitudeIntegration integration;
  private AmplitudeIntegration.Provider mockProvider = new AmplitudeIntegration.Provider() {
    @Override
    public AmplitudeClient get() {
      return amplitude;
    }
  };

  @Before
  public void setUp() {
    initMocks(this);

    when(analytics.getApplication()).thenReturn(application);
    when(analytics.logger("Amplitude")).thenReturn(Logger.with(VERBOSE));

    integration =
        new AmplitudeIntegration(mockProvider, analytics, new ValueMap()
                .putValue("apiKey", "foo"));

    Mockito.reset(amplitude);
  }

  @Test
  public void factory() {
    assertEquals(AmplitudeIntegration.FACTORY.key(), "Amplitude");
  }

  @Test
  public void initialize() {
    integration = new AmplitudeIntegration(mockProvider, analytics,
        new ValueMap().putValue("apiKey", "foo")
            .putValue("trackAllPagesV2", true)
            .putValue("trackAllPages", true)
            .putValue("trackCategorizedPages", false)
            .putValue("trackNamedPages", true)
            .putValue("enableLocationListening", false)
            .putValue("useAdvertisingIdForDeviceId", true));

    assertEquals(integration.trackAllPagesV2, true);
    assertEquals(integration.trackAllPages, true);
    assertEquals(integration.trackCategorizedPages, false);
    assertEquals(integration.trackNamedPages, true);

    verify(amplitude).initialize(application, "foo");
    verify(amplitude).enableForegroundTracking(application);
    verify(amplitude).trackSessionEvents(false);
    verify(amplitude).disableLocationListening();
    verify(amplitude).useAdvertisingIdForDeviceId();
  }

  @Test
  public void initializeWithDefaultArguments() {
    integration =
        new AmplitudeIntegration(mockProvider, analytics, new ValueMap().putValue("apiKey", "foo"));

    assertEquals(integration.trackAllPages, false);
    assertEquals(integration.trackCategorizedPages, false);
    assertEquals(integration.trackNamedPages, false);

    verify(amplitude).initialize(application, "foo");
    verify(amplitude).enableForegroundTracking(application);
    verify(amplitude).trackSessionEvents(false);
  }

  @Test
  public void track() {
    integration.track((new TrackPayload.Builder())
            .anonymousId("foo")
            .event("foo")
            .properties(new Properties())
            .build());

    verify(amplitude)
        .logEvent(eq("foo"), toStringEq(new JSONObject()), isNull(JSONObject.class), eq(false));
    verifyNoMoreInteractions(amplitude);
  }

  @Test
  public void trackWithGroups() throws JSONException {
    Map<String, Object> options = new ValueMap().putValue("groups", (new ValueMap()).putValue("foo", "bar"));
    integration.track((new TrackPayload.Builder())
            .anonymousId("foo")
        .event("foo")
        .integration("Amplitude", options)
        .build());

    JSONObject groups = new JSONObject();
    groups.put("foo", "bar");
    verify(amplitude)
        .logEvent(eq("foo"), toStringEq(new JSONObject()), toStringEq(groups), eq(false));
    verifyNoMoreInteractions(amplitude);
  }

  @Test
  public void trackWithListGroups() throws JSONException {
    Map<String, Object> options = new ValueMap()
            .putValue("groups", new ValueMap()
                            .putValue("sports", Arrays.asList("basketball", "tennis")));
    integration.track((new TrackPayload.Builder())
            .anonymousId("foo")
        .event("foo")
        .properties(new Properties())
        .integration("Amplitude", options)
        .build());

    JSONObject groups = new JSONObject();
    groups.put("sports", new JSONArray().put("basketball").put("tennis"));
    verify(amplitude)
        .logEvent(eq("foo"), toStringEq(new JSONObject()), toStringEq(groups), eq(false));
    verifyNoMoreInteractions(amplitude);
  }

  @Test
  public void trackOutOfSession() {
    Map<String, Object> options = new ValueMap().putValue("outOfSession", true);
    integration.track((new TrackPayload.Builder())
            .anonymousId("foo")
            .event("foo")
            .properties(new Properties())
            .integration("Amplitude", options)
            .build());

    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(new JSONObject()), isNull(JSONObject.class), eq(true));
  }

  @Test
  public void trackOutOfSessionOptionsNull() {
    integration.track((new TrackPayload.Builder())
            .anonymousId("foo")
            .event("foo")
            .properties(new Properties())
            .build());

    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(new JSONObject()), isNull(JSONObject.class), eq(false));
  }

  @Test
  public void trackOutOfSessionNotInstanceOfBoolean() {
    Map<String, Object> options = new ValueMap().putValue("outOfSession", "string");
    integration.track((new TrackPayload.Builder())
            .anonymousId("foo")
            .event("foo")
            .properties(new Properties())
            .integration("Amplitude", options)
            .build());

    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(new JSONObject()), isNull(JSONObject.class), eq(false));
  }

  @Test
  public void trackOutOfSessionKeyNotSet() {
    Map<String, Object> options = new ValueMap();
    options.put("randomSetting", "testing");
    integration.track((new TrackPayload.Builder())
            .anonymousId("anonId")
            .event("foo")
            .properties(new Properties())
            .integration("Amplitude", options)
            .build());

    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(new JSONObject()), isNull(JSONObject.class), eq(false));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void trackWithRevenue() {
    Properties properties = new Properties()
        .putRevenue(20)
        .putValue("productId", "bar")
        .putValue("quantity", 10)
        .putValue("receipt", "baz")
        .putValue("receiptSignature", "qux");
    TrackPayload trackPayload =
        new TrackPayloadBuilder().event("foo").properties(properties).build();

    integration.track(trackPayload);
    verify(amplitude)
        .logEvent(eq("foo"), toStringEq(properties.toJsonObject()), isNull(JSONObject.class), eq(false));
    verify(amplitude).logRevenue("bar", 10, 20, "baz", "qux");
  }

  @SuppressWarnings("deprecation")
  @Test
  public void trackWithTotal() {
    Properties properties = new Properties()
        .putTotal(15)
        .putValue("productId", "bar")
        .putValue("quantity", 10)
        .putValue("receipt", "baz")
        .putValue("receiptSignature", "qux");

    integration.track((new TrackPayload.Builder()).anonymousId("anonId").event("foo").properties(properties).build());

    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(properties.toJsonObject()), isNull(JSONObject.class), eq(false));
    verify(amplitude).logRevenue("bar", 10, 15, "baz", "qux");
  }

  @Test
  public void trackWithRevenueV2() {
    integration.useLogRevenueV2 = true;
    Properties properties = new Properties()
        .putRevenue(20)
        .putValue("productId", "bar")
        .putValue("quantity", 10)
        .putValue("receipt", "baz")
        .putValue("receiptSignature", "qux");

    integration.track((new TrackPayload.Builder()).anonymousId("anonId").event("foo").properties(properties).build());

    verify(amplitude)
        .logEvent(eq("foo"), toStringEq(properties.toJsonObject()), isNull(JSONObject.class), eq(false));

    Revenue expectedRevenue = new Revenue()
        .setProductId("bar")
        .setPrice(20)
        .setQuantity(1)
        .setReceipt("baz", "qux")
        .setEventProperties(properties.toJsonObject());

    verify(amplitude).logRevenueV2(expectedRevenue);
  }

  @Test
  public void trackWithRevenueV2Price() {
    Properties properties = new Properties()
            .putValue("productId", "bar")
            .putValue("quantity", 10)
            .putValue("price", 2.00)
            .putValue("receipt", "baz")
            .putValue("receiptSignature", "qux");

    integration.track((new TrackPayload.Builder()).anonymousId("anonId").event("foo").properties(properties).build());

    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(properties.toJsonObject()), isNull(JSONObject.class), eq(false));

    verifyNoMoreInteractions(amplitude);
  }

  @Test public void trackWithRevenueV2PriceAndQuantity() {
    integration.useLogRevenueV2 = true;

    Properties properties = new Properties()
            .putRevenue(20)
            .putValue("productId", "bar")
            .putValue("quantity", 10)
            .putValue("price", 2.00)
            .putValue("receipt", "baz")
            .putValue("receiptSignature", "qux");

    integration.track((new TrackPayload.Builder()).anonymousId("anonId").event("foo").properties(properties).build());
    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(properties.toJsonObject()), isNull(JSONObject.class), eq(false));

    Revenue expectedRevenue = new Revenue().setProductId("bar")
            .setPrice(2)
            .setQuantity(10)
            .setReceipt("baz", "qux")
            .setEventProperties(properties.toJsonObject());

    verify(amplitude).logRevenueV2(expectedRevenue);
  }

  @Test
  public void trackWithTotalV2() {
    integration.useLogRevenueV2 = true;

    Properties properties = new Properties()
        .putTotal(20)
        .putValue("productId", "bar")
        .putValue("quantity", 10)
        .putValue("receipt", "baz")
        .putValue("receiptSignature", "qux");
    TrackPayload trackPayload =
            (new TrackPayload.Builder()).anonymousId("anonId").event("foo").properties(properties).build();

    integration.track(trackPayload);
    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(properties.toJsonObject()), isNull(JSONObject.class), eq(false));

    Revenue expectedRevenue = new Revenue()
        .setProductId("bar")
        .setPrice(20)
        .setQuantity(1)
        .setReceipt("baz", "qux")
        .setEventProperties(properties.toJsonObject());

    verify(amplitude).logRevenueV2(expectedRevenue);
  }

  @Test
  public void trackWithTotalV2Price() {
    integration.useLogRevenueV2 = true;

    Properties properties = new Properties()
            .putTotal(20)
            .putValue("productId", "bar")
            .putValue("quantity", 10)
            .putValue("price", 2.00)
            .putValue("receipt", "baz")
            .putValue("receiptSignature", "qux");

    integration.track((new TrackPayload.Builder()).anonymousId("anonId").event("foo").properties(properties).build());

    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(properties.toJsonObject()), isNull(JSONObject.class), eq(false));

    Revenue expectedRevenue = new Revenue()
            .setProductId("bar")
            .setPrice(2)
            .setQuantity(10)
            .setReceipt("baz", "qux")
            .setEventProperties(properties.toJsonObject());

    verify(amplitude).logRevenueV2(expectedRevenue);
  }

  @Test
  public void trackWithTotalV2PriceAndQuantity() {
    integration.useLogRevenueV2 = true;

    Properties properties = new Properties()
            .putTotal(20)
            .putValue("productId", "bar")
            .putValue("quantity", 10)
            .putValue("price", 2.00)
            .putValue("receipt", "baz")
            .putValue("receiptSignature", "qux");

    integration.track((new TrackPayload.Builder()).anonymousId("anonId").event("foo").properties(properties).build());

    verify(amplitude)
            .logEvent(eq("foo"), toStringEq(properties.toJsonObject()), isNull(JSONObject.class), eq(false));

    Revenue expectedRevenue = new Revenue()
            .setProductId("bar")
            .setPrice(2)
            .setQuantity(10)
            .setReceipt("baz", "qux")
            .setEventProperties(properties.toJsonObject());

    verify(amplitude).logRevenueV2(expectedRevenue);
  }

  @Test
  public void identify() {
    Traits traits = createTraits("foo").putAge(20).putFirstName("bar");
    IdentifyPayload payload = (new IdentifyPayload.Builder()).userId("foo").traits(traits).build();

    integration.identify(payload);

    verify(amplitude).setUserId("foo");
    verify(amplitude).setUserProperties(toStringEq(traits.toJsonObject()));

    verifyNoMoreInteractions(amplitude);
  }

  @Test
  public void identifyWithGroups() {
    Map<String, Object> options = new ValueMap()
            .putValue("groups", new ValueMap().putValue("foo", "bar"));
    Traits traits = createTraits("foo").putAge(20).putFirstName("bar");
    IdentifyPayload payload = (new IdentifyPayload.Builder()).userId("foo")
        .traits(traits)
        .integration("Amplitude", options)
        .build();

    integration.identify(payload);

    verify(amplitude).setUserId("foo");
    verify(amplitude).setUserProperties(toStringEq(traits.toJsonObject()));

    verify(amplitude).setGroup("foo", "bar");
  }

  @Test
  public void identifyWithListGroups() {
    Map<String, Object> group = new ValueMap();
    group.put("sports", Arrays.asList("basketball", "tennis"));
    Map<String, Object> options = new ValueMap().putValue("groups", group);

    Traits traits = createTraits("foo").putAge(20).putFirstName("bar");

    IdentifyPayload payload = (new IdentifyPayload.Builder()).userId("foo")
        .traits(traits)
        .integration("Amplitude", options)
        .build();

    integration.identify(payload);

    verify(amplitude).setUserId("foo");
    verify(amplitude).setUserProperties(toStringEq(traits.toJsonObject()));
    verify(amplitude).setGroup(eq("sports"), toStringEq(new JSONArray().put("basketball").put("tennis")));
  }

  @Test
  public void identifyWithIncrementedTraits() {
    ValueMap settings = new ValueMap()
        .putValue("traitsToIncrement", Arrays.asList("double", "float", "integer", "long", "string"));
    integration.traitsToIncrement = getStringSet(settings, "traitsToIncrement");

    double d = 100.0;
    float f = 100.0f;
    int i = 100;
    long l = 1000L;
    String s = "random string";

    Traits traits = createTraits("foo")
        .putValue("anonymousId", "anonId")
        .putValue("double", d)
        .putValue("float", f)
        .putValue("integer", i)
        .putValue("long", l)
        .putValue("string", s);
    IdentifyPayload payload = (new IdentifyPayload.Builder()).anonymousId("anonId").userId("foo").traits(traits).build();
    integration.identify(payload);

    Identify expectedIdentify = new Identify();
    expectedIdentify.set("anonymousId", "anonId");
    expectedIdentify.set("userId", "foo");
    expectedIdentify.add("double", d);
    expectedIdentify.add("float", f);
    expectedIdentify.add("integer", i);
    expectedIdentify.add("long", l);
    expectedIdentify.add("string", s);

    verify(amplitude).identify(identifyEq(expectedIdentify));
  }

  @Test
  public void identifyWithSetOnce() {
    ValueMap settings = new ValueMap()
        .putValue("traitsToSetOnce", Arrays.asList("double", "float", "integer", "long", "string"));
    integration.traitsToSetOnce = getStringSet(settings, "traitsToSetOnce");

    double d = 100.0;
    float f = 100.0f;
    int i = 100;
    long l = 1000L;
    String s = "random string";

    Traits traits = createTraits("foo")
        .putValue("anonymousId", "anonId")
        .putValue("double", d)
        .putValue("float", f)
        .putValue("integer", i)
        .putValue("long", l)
        .putValue("string", s);
    IdentifyPayload payload = new IdentifyPayload.Builder()
            .userId("foo").anonymousId("anonId").traits(traits).build();
    integration.identify(payload);

    Identify expectedIdentify = new Identify();
    expectedIdentify.set("anonymousId", "anonId");
    expectedIdentify.set("userId", "foo");
    expectedIdentify.setOnce("double", d);
    expectedIdentify.setOnce("float", f);
    expectedIdentify.setOnce("integer", i);
    expectedIdentify.setOnce("long", l);
    expectedIdentify.setOnce("string", s);

    verify(amplitude).identify(identifyEq(expectedIdentify));
  }

  @Test
  // Category is deprecated, but we need to support it for Amplitude
  @SuppressWarnings("deprecation")
  public void screen() {
    integration.trackAllPagesV2 = false;
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;

    integration.screen((new ScreenPayload.Builder()).anonymousId("foo").category("foo").build());

    verifyNoMoreInteractions(amplitude);
  }

  @Test
  public void screenTrackNamedPagesWithName() {
    integration.trackAllPagesV2 = false;
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen((new ScreenPayload.Builder()).anonymousId("foo").name("bar").build());
    verifyAmplitudeLoggedEvent("Viewed bar Screen", new JSONObject());
  }

  @Test
  // Category is deprecated, but we need to support it for Amplitude
  @SuppressWarnings("deprecation")
  public void screenTrackNamedPagesWithCategory() {
    integration.trackAllPagesV2 = false;
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen((new ScreenPayload.Builder()).anonymousId("foo").category("foo").build());
    verifyNoMoreInteractions(amplitude);
  }

  @Test
  // Category is deprecated, but we need to support it for Amplitude
  @SuppressWarnings("deprecation")
  public void screenTrackCategorizedPages() {
    integration.trackAllPagesV2 = false;
    integration.trackAllPages = false;
    integration.trackCategorizedPages = true;
    integration.trackNamedPages = false;

    integration.screen((new ScreenPayload.Builder()).anonymousId("foo").category("foo").build());
    verifyAmplitudeLoggedEvent("Viewed foo Screen", new JSONObject());

    integration.screen((new ScreenPayload.Builder()).anonymousId("foo").name("foo").build());
    verifyNoMoreInteractions(amplitude);
  }

  @Test
  // Category is deprecated, but we need to support it for Amplitude
  @SuppressWarnings("deprecation")
  public void screenTrackAllPages() {
    integration.trackAllPagesV2 = false;
    integration.trackAllPages = true;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;

    integration.screen((new ScreenPayload.Builder()).anonymousId("foo").category("foo").build());
    verifyAmplitudeLoggedEvent("Viewed foo Screen", new JSONObject());

    integration.screen((new ScreenPayload.Builder()).anonymousId("foo").name("bar").build());
    verifyAmplitudeLoggedEvent("Viewed bar Screen", new JSONObject());

    integration.screen((new ScreenPayload.Builder()).anonymousId("foo").category("bar").name("baz").build());
    verifyAmplitudeLoggedEvent("Viewed baz Screen", new JSONObject());
  }

  @Test
  public void screenTrackAllPagesV2() throws JSONException {
    Properties properties = new Properties();
    properties.putValue("bar", "baz");
    integration.screen((new ScreenPayload.Builder()).anonymousId("foo").name("foo").properties(properties).build());
    verifyAmplitudeLoggedEvent("Loaded a Screen", new JSONObject()
        .put("name", "foo").put("bar", "baz"));
  }

  @Test
  public void group() {
    GroupPayload payload = (new GroupPayload.Builder())
            .userId("foo")
        .groupId("testGroupId")
        .build();

    integration.group(payload);

    Identify expectedIdentify = new Identify();
    expectedIdentify.set("library", "segment");

    verify(amplitude).setGroup("[Segment] Group", "testGroupId");
    verify(amplitude).groupIdentify(eq("[Segment] Group"), eq("testGroupId"), identifyEq(expectedIdentify));
  }

  @Test
  public void groupWithGroupName() {
    Traits traits = new Traits().putName("testName");

    GroupPayload payload = (new GroupPayload.Builder())
            .userId("foo")
        .groupId("testGroupId")
        .traits(traits)
        .build();

    integration.group(payload);

    Identify expectedIdentify = new Identify();
    expectedIdentify.set("library", "segment");
    expectedIdentify.set("group_properties", traits.toJsonObject());

    verify(amplitude).setGroup("testName", "testGroupId");
    verify(amplitude).groupIdentify(eq("testName"), eq("testGroupId"), identifyEq(expectedIdentify));
  }

  @Test
  public void groupWithGroupNameSettings() {
    integration.groupTypeTrait = "company";
    integration.groupValueTrait = "companyType";

    Traits traits = new Traits().putValue("company", "Segment").putValue("companyType", "data").putValue("members", 80);

    GroupPayload payload = (new GroupPayload.Builder())
            .userId("foo")
        .groupId("testGroupId")
        .traits(traits)
        .build();

    integration.group(payload);

    Identify expectedIdentify = new Identify();
    expectedIdentify.set("library", "segment");
    expectedIdentify.set("group_properties", traits.toJsonObject());

    verify(amplitude).setGroup("Segment", "data");
    verify(amplitude).groupIdentify(eq("Segment"), eq("data"), identifyEq(expectedIdentify));
  }

  @Test
  public void flush() {
    integration.flush();

    verify(amplitude).uploadEvents();
  }

  @Test
  public void reset() {
    integration.reset();

    verify(amplitude).setUserId(null);
    verify(amplitude).regenerateDeviceId();

    // Previously we called clearUserProperties() which was incorrect.
    verify(amplitude, never()).clearUserProperties();
  }

  @Test
  public void groups() throws JSONException {
    assertNull(AmplitudeIntegration.groups((new TrackPayload.Builder())
            .userId("foo")
        .event("foo")
        .build()));
    assertNull(AmplitudeIntegration.groups((new TrackPayload.Builder())
            .userId("foo")
        .event("foo")
            .integrations(new ValueMap())
        .build()));
    assertNull(AmplitudeIntegration.groups((new TrackPayload.Builder())
            .userId("foo")
        .event("foo")
        .integration("Mixpanel", new ValueMap().putValue("groups", "foo"))
        .build()));
    Map<String, Object> options = new HashMap<>();
    options.put("Amplitude", true);
    assertNull(AmplitudeIntegration.groups((new TrackPayload.Builder())
            .userId("foo")
        .event("foo")
        .integrations(options)
        .build()));
    assertNull(AmplitudeIntegration.groups((new TrackPayload.Builder())
            .userId("foo")
        .event("foo")
        .integration("Amplitude", new ValueMap().putValue("foo", "bar"))
        .build()));

    assertEquals((new JSONObject()).put("foo", "bar").toString(), AmplitudeIntegration.groups((new TrackPayload.Builder())
            .userId("foo")
        .event("foo")
        .integration("Amplitude",
            new ValueMap().putValue("groups", new ValueMap().putValue("foo", "bar"))
        )
        .build()).toString());

    assertEquals((new JSONObject()).put("sports", (new JSONArray()).put("basketball").put("tennis")).toString(),
            AmplitudeIntegration.groups((new TrackPayload.Builder()).userId("foo")
        .event("foo")
        .integration("Amplitude",
            new ValueMap().putValue("groups",
                new ValueMap().putValue("sports", Arrays.asList("basketball", "tennis")))
            )
        .build()).toString());
  }

  private void verifyAmplitudeLoggedEvent(String event, JSONObject jsonObject) {
    verify(amplitude).logEvent(eq(event), toStringEq(jsonObject), isNull(JSONObject.class), eq(false));
  }

  /**
   * Uses the string representation of the object. Useful for JSON objects.
   * @param expected Expected object
   * @param <T> Type
   * @return Argument matcher.
   */
  private static <T> T toStringEq(T expected) {
    return argThat(new ToStringArgumentMatcher<>(expected));
  }

  public static class ToStringArgumentMatcher<T> implements ArgumentMatcher<T> {

    private T expected;

    ToStringArgumentMatcher(T expected) {
      this.expected = expected;
    }

    @Override
    public boolean matches(T other) {
      if (expected == null) {
        return (other == null);
      } else if (other == null) {
        return false;
      }

      return expected.toString().equals(other.toString());
    }

    public String toString() {
      if (expected == null) {
        return "null";
      }
      return expected.toString();
    }

  }

  /**
   * Uses the user properties operations to compare the identify payloads.
   * @param expected Identify payload expected.
   * @return Argument matcher.
   */
  private static Identify identifyEq(Identify expected) {
    return argThat(new IdentifyEqArgumentMatcher(expected));
  }

  private static class IdentifyEqArgumentMatcher implements ArgumentMatcher<Identify> {

    private Identify expected;

    IdentifyEqArgumentMatcher(Identify expected) {
      this.expected = expected;
    }

    @Override
    public boolean matches(Identify other) {
      if (expected == null) {
        return (other == null);
      } else if (other == null) {
        return false;
      }

      return (expected.getUserPropertiesOperations().toString().equals(other.getUserPropertiesOperations().toString()));
    }

    @Override
    public String toString() {
      return expected.getUserPropertiesOperations().toString();
    }
  }

}
