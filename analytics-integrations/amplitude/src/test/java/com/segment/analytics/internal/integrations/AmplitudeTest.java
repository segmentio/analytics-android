package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.amplitude.api.Amplitude;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.Random;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.TestUtils.JSONObjectMatcher.jsonEq;
import static com.segment.analytics.TestUtils.createTraits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(Amplitude.class)
public class AmplitudeTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @MockitoAnnotations.Mock Application context;
  AmplitudeIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Amplitude.class);
    integration = new AmplitudeIntegration();
  }

  @Test public void initialize() {
    integration.initialize(context, //
        new ValueMap().putValue("apiKey", "foo")
            .putValue("trackAllPages", true)
            .putValue("trackCategorizedPages", false)
            .putValue("trackNamedPages", true), VERBOSE);
    verifyStatic();
    Amplitude.initialize(context, "foo");
    assertThat(integration.trackAllPages).isTrue();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isTrue();
    // Verify default args
    integration.initialize(context, //
        new ValueMap().putValue("apiKey", "foo"), VERBOSE);
    assertThat(integration.trackAllPages).isFalse();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isFalse();
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyStatic();
    Amplitude.startSession();
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyStatic();
    Amplitude.endSession();
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test public void track() {
    Properties properties = new Properties();
    integration.track(new TrackPayloadBuilder().event("foo").properties(properties).build());
    verifyStatic();
    Amplitude.logEvent(eq("foo"), jsonEq(properties.toJsonObject()));
    verifyNoMoreInteractions(Amplitude.class);
  }

  public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(Amplitude.class);
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
    Amplitude.logEvent(eq("foo"), jsonEq(properties.toJsonObject()));
    verifyStatic();
    Amplitude.logRevenue("bar", 10, 20, "baz", "qux");
  }

  @Test public void identify() {
    Traits traits = createTraits("foo").putAge(20).putFirstName("bar");
    IdentifyPayload payload = new IdentifyPayloadBuilder().traits(traits).build();
    integration.identify(payload);
    verifyStatic();
    Amplitude.setUserId("foo");
    verifyStatic();
    Amplitude.setUserProperties(jsonEq(traits.toJsonObject()));
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test public void screen() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;
    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackNamedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen(new ScreenPayloadBuilder().name("bar").build());
    verifyAmplitudeLoggedEvent("Viewed bar Screen", new JSONObject());

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackCategorizedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = true;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyAmplitudeLoggedEvent("Viewed foo Screen", new JSONObject());

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreInteractions(Amplitude.class);
  }

  @Test
  public void screenTrackAllPages() {
    integration.trackAllPages = true;
    integration.trackCategorizedPages = new Random().nextBoolean();
    integration.trackNamedPages = new Random().nextBoolean();

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyAmplitudeLoggedEvent("Viewed foo Screen", new JSONObject());

    integration.screen(new ScreenPayloadBuilder().name("bar").build());
    verifyAmplitudeLoggedEvent("Viewed bar Screen", new JSONObject());

    integration.screen(new ScreenPayloadBuilder().category("bar").name("baz").build());
    verifyAmplitudeLoggedEvent("Viewed baz Screen", new JSONObject());
  }

  @Test public void flush() {
    integration.flush();
    verifyStatic();
    Amplitude.uploadEvents();
  }

  private void verifyAmplitudeLoggedEvent(String event, JSONObject jsonObject) {
    verifyStatic();
    Amplitude.logEvent(eq(event), jsonEq(jsonObject));
  }
}
