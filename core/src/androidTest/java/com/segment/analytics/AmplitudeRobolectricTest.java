package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
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

import static com.segment.analytics.TestUtils.IdentifyPayloadBuilder;
import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Amplitude.class)
public class AmplitudeRobolectricTest extends AbstractIntegrationTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  AmplitudeIntegration integration;

  @Before public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Amplitude.class);
    integration = new AmplitudeIntegration();
  }

  @Test @Override public void initialize() {
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

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyStatic();
    Amplitude.startSession();
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyStatic();
    Amplitude.endSession();
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test @Override public void track() {
    TrackPayload trackPayload = new TrackPayloadBuilder().event("foo").build();
    integration.track(trackPayload);
    verifyStatic();
    Amplitude.logEvent(eq("foo"), any(JSONObject.class));
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Override public void alias() {

  }

  @Test public void trackWithRevenue() {
    Properties properties = new Properties().putRevenue(20)
        .putValue("productId", "bar")
        .putValue("quantity", 10)
        .putValue("receipt", "baz")
        .putValue("receiptSignature", "qux");
    TrackPayload trackPayload =
        new TrackPayloadBuilder().event("foo").properties(properties).build();
    integration.track(trackPayload);
    verifyStatic();
    Amplitude.logEvent(eq("foo"), any(JSONObject.class));
    verifyStatic();
    Amplitude.logRevenue("bar", 10, 20, "baz", "qux");
  }

  @Test @Override public void identify() {
    IdentifyPayload payload = new IdentifyPayloadBuilder().userId("foo").build();
    integration.identify(payload);
    verifyStatic();
    Amplitude.setUserId("foo");
    verifyStatic();
    Amplitude.setUserProperties(Matchers.<JSONObject>any());
    // todo: verify JSONObject
  }

  @Override public void group() {

  }

  @Test @Override public void screen() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;
    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackNamedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen(new ScreenPayloadBuilder().name("bar").build());
    verifyAmplitudeLoggedEvent("Viewed bar Screen", null);

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackCategorizedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = true;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyAmplitudeLoggedEvent("Viewed foo Screen", null);

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyStatic();
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackAllPages() {
    integration.trackAllPages = true;
    integration.trackCategorizedPages = new Random().nextBoolean();
    integration.trackNamedPages = new Random().nextBoolean();

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyAmplitudeLoggedEvent("Viewed foo Screen", null);

    integration.screen(new ScreenPayloadBuilder().name("bar").build());
    verifyAmplitudeLoggedEvent("Viewed bar Screen", null);

    integration.screen(new ScreenPayloadBuilder().category("bar").name("baz").build());
    verifyAmplitudeLoggedEvent("Viewed baz Screen", null);
  }

  @Test @Override public void flush() {
    integration.flush();
    verifyStatic();
    Amplitude.uploadEvents();
  }

  private void verifyAmplitudeLoggedEvent(String event, JSONObject jsonObject) {
    verifyStatic();
    Amplitude.logEvent(eq(event), any(JSONObject.class));
  }
}
