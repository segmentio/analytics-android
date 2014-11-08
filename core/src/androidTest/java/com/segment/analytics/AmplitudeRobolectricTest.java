package com.segment.analytics;

import com.amplitude.api.Amplitude;
import java.util.Random;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Amplitude.class)
public class AmplitudeRobolectricTest extends IntegrationRobolectricExam {
  @Rule public PowerMockRule rule = new PowerMockRule();

  AmplitudeIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Amplitude.class);
    integration = new AmplitudeIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    integration.initialize(context, //
        new JsonMap().putValue("apiKey", "foo")
            .putValue("trackAllPages", true)
            .putValue("trackCategorizedPages", false)
            .putValue("trackNamedPages", true), true);
    verifyStatic();
    Amplitude.initialize(context, "foo");
    assertThat(integration.trackAllPages).isTrue();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isTrue();
    // Verify default args
    integration.initialize(context, //
        new JsonMap().putValue("apiKey", "foo"), true);
    assertThat(integration.trackAllPages).isFalse();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isFalse();
  }

  @Test
  public void track() {
    TrackPayload trackPayload = trackPayload("foo");
    integration.track(trackPayload);
    verifyStatic();
    Amplitude.logEvent(eq("foo"), any(JSONObject.class));
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void trackWithRevenue() {
    TrackPayload trackPayload = trackPayload("foo");
    trackPayload.properties()
        .putRevenue(20)
        .putValue("productId", "bar")
        .putValue("quantity", 10)
        .putValue("receipt", "baz")
        .putValue("receiptSignature", "qux");
    integration.track(trackPayload);
    verifyStatic();
    Amplitude.logEvent(eq("foo"), any(JSONObject.class));
    verifyStatic();
    Amplitude.logRevenue("bar", 10, 20, "baz", "qux");
  }

  @Test
  public void activityLifecycle() {
    integration.onActivityResumed(activity);
    verifyStatic();
    Amplitude.startSession();

    integration.onActivityPaused(activity);
    verifyStatic();
    Amplitude.endSession();

    integration.onActivityStarted(activity);
    integration.onActivityStopped(activity);
    integration.onActivityCreated(activity, bundle);
    integration.onActivityDestroyed(activity);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void identify() {
    IdentifyPayload payload = identifyPayload("michael");
    integration.identify(payload);
    verifyStatic();
    Amplitude.setUserId("michael");
    verifyStatic();
    Amplitude.setUserProperties(Matchers.<JSONObject>any());
    // todo: verify JSONObject
  }

  @Test
  public void screenTrackNothing() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;
    integration.screen(screenPayload("foo", "bar"));
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackNamedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen(screenPayload(null, "bar"));
    verifyAmplitudeEvent("Viewed bar Screen", null);

    integration.screen(screenPayload("foo", null));
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackCategorizedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = true;
    integration.trackNamedPages = false;

    integration.screen(screenPayload("foo", null));
    verifyAmplitudeEvent("Viewed foo Screen", null);

    integration.screen(screenPayload(null, "bar"));
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackAllPages() {
    integration.trackAllPages = true;
    integration.trackCategorizedPages = new Random().nextBoolean();
    integration.trackNamedPages = new Random().nextBoolean();

    integration.screen(screenPayload("foo", null));
    verifyAmplitudeEvent("Viewed foo Screen", null);

    integration.screen(screenPayload(null, "bar"));
    verifyAmplitudeEvent("Viewed bar Screen", null);

    integration.screen(screenPayload("bar", "baz"));
    verifyAmplitudeEvent("Viewed baz Screen", null);
  }

  @Test
  public void flush() {
    integration.flush();
    verifyStatic();
    Amplitude.uploadEvents();
  }

  private void verifyAmplitudeEvent(String event, JSONObject jsonObject) {
    verifyStatic();
    Amplitude.logEvent(eq(event), any(JSONObject.class));
  }
}
