package com.segment.analytics;

import com.amplitude.api.Amplitude;
import java.util.Random;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
// todo: verify JSONObject
public class AmplitudeTest extends IntegrationExam {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  AmplitudeIntegrationAdapter amplitudeIntegrationAdapter;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Amplitude.class);
    amplitudeIntegrationAdapter = new AmplitudeIntegrationAdapter();
  }

  @Test
  public void initialize() throws InvalidConfigurationException {
    amplitudeIntegrationAdapter.initialize(context, //
        new JsonMap().putValue("apiKey", "foo")
            .putValue("trackAllPages", true)
            .putValue("trackCategorizedPages", false)
            .putValue("trackNamedPages", true)
    );
    verifyStatic();
    Amplitude.initialize(context, "foo");
    assertThat(amplitudeIntegrationAdapter.trackAllPages).isTrue();
    assertThat(amplitudeIntegrationAdapter.trackCategorizedPages).isFalse();
    assertThat(amplitudeIntegrationAdapter.trackNamedPages).isTrue();
    // Verify default args
    amplitudeIntegrationAdapter.initialize(context, //
        new JsonMap().putValue("apiKey", "foo"));
    assertThat(amplitudeIntegrationAdapter.trackAllPages).isFalse();
    assertThat(amplitudeIntegrationAdapter.trackCategorizedPages).isFalse();
    assertThat(amplitudeIntegrationAdapter.trackNamedPages).isFalse();
  }

  @Test
  public void track() {
    TrackPayload trackPayload = trackPayload("foo");
    amplitudeIntegrationAdapter.track(trackPayload);
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
    amplitudeIntegrationAdapter.track(trackPayload);
    verifyStatic();
    Amplitude.logEvent(eq("foo"), any(JSONObject.class));
    Amplitude.logRevenue("bar", 10, 20, "baz", "qux");
  }

  @Test
  public void activityLifecycle() {
    amplitudeIntegrationAdapter.onActivityResumed(activity);
    verifyStatic();
    Amplitude.startSession();

    amplitudeIntegrationAdapter.onActivityPaused(activity);
    verifyStatic();
    Amplitude.endSession();

    amplitudeIntegrationAdapter.onActivityStarted(activity);
    amplitudeIntegrationAdapter.onActivityStopped(activity);
    amplitudeIntegrationAdapter.onActivityCreated(activity, bundle);
    amplitudeIntegrationAdapter.onActivityDestroyed(activity);
    amplitudeIntegrationAdapter.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void identify() {
    IdentifyPayload payload = identifyPayload("michael");
    amplitudeIntegrationAdapter.identify(payload);
    verifyStatic();
    Amplitude.setUserId("michael");
    verifyStatic();
    Amplitude.setUserProperties(any(JSONObject.class));
  }

  @Test
  public void screenTrackNothing() {
    amplitudeIntegrationAdapter.trackAllPages = false;
    amplitudeIntegrationAdapter.trackCategorizedPages = false;
    amplitudeIntegrationAdapter.trackNamedPages = false;
    amplitudeIntegrationAdapter.screen(screenPayload("foo", "bar"));
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackNamedPages() {
    amplitudeIntegrationAdapter.trackAllPages = false;
    amplitudeIntegrationAdapter.trackCategorizedPages = false;
    amplitudeIntegrationAdapter.trackNamedPages = true;

    amplitudeIntegrationAdapter.screen(screenPayload(null, "bar"));
    verifyAmplitudeEvent("Viewed bar Screen", null);

    amplitudeIntegrationAdapter.screen(screenPayload("foo", null));
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackCategorizedPages() {
    amplitudeIntegrationAdapter.trackAllPages = false;
    amplitudeIntegrationAdapter.trackCategorizedPages = true;
    amplitudeIntegrationAdapter.trackNamedPages = false;

    amplitudeIntegrationAdapter.screen(screenPayload("foo", null));
    verifyAmplitudeEvent("Viewed foo Screen", null);

    amplitudeIntegrationAdapter.screen(screenPayload(null, "bar"));
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackAllPages() {
    amplitudeIntegrationAdapter.trackAllPages = true;
    amplitudeIntegrationAdapter.trackCategorizedPages = new Random().nextBoolean();
    amplitudeIntegrationAdapter.trackNamedPages = new Random().nextBoolean();

    amplitudeIntegrationAdapter.screen(screenPayload("foo", null));
    verifyAmplitudeEvent("Viewed foo Screen", null);

    amplitudeIntegrationAdapter.screen(screenPayload(null, "bar"));
    verifyAmplitudeEvent("Viewed bar Screen", null);

    amplitudeIntegrationAdapter.screen(screenPayload("bar", "baz"));
    verifyAmplitudeEvent("Viewed baz Screen", null);
  }

  @Test
  public void flush() {
    amplitudeIntegrationAdapter.flush();
    verifyStatic();
    Amplitude.uploadEvents();
  }

  private void verifyAmplitudeEvent(String event, JSONObject jsonObject) {
    verifyStatic();
    Amplitude.logEvent(eq(event), any(JSONObject.class));
  }
}