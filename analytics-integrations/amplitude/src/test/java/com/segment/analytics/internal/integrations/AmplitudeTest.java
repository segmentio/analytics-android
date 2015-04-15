package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.amplitude.api.AmplitudeClient;
import com.segment.analytics.Properties;
import com.segment.analytics.Randoms;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.TestUtils.createTraits;
import static com.segment.analytics.TestUtils.jsonEq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(AmplitudeClient.class)
public class AmplitudeTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  @Mock AmplitudeClient amplitude;
  AmplitudeIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    integration = new AmplitudeIntegration();
    integration.amplitude = amplitude;
  }

  @Test public void initialize() {
    integration = new AmplitudeIntegration();

    PowerMockito.mockStatic(AmplitudeClient.class);
    PowerMockito.when(AmplitudeClient.getInstance()).thenReturn(amplitude);

    integration.initialize(context, //
        new ValueMap().putValue("apiKey", "foo")
            .putValue("trackAllPages", true)
            .putValue("trackCategorizedPages", false)
            .putValue("trackNamedPages", true), VERBOSE);

    verify(amplitude).initialize(context, "foo");
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

    verifyNoMoreInteractions(amplitude);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);

    integration.onActivityStarted(activity);

    verifyNoMoreInteractions(amplitude);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);

    integration.onActivityResumed(activity);

    verify(amplitude).startSession();
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);

    integration.onActivityPaused(activity);

    verify(amplitude).endSession();
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);

    integration.onActivityStopped(activity);

    verifyNoMoreInteractions(amplitude);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);

    integration.onActivitySaveInstanceState(activity, bundle);

    verifyNoMoreInteractions(amplitude);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);

    integration.onActivityDestroyed(activity);

    verifyNoMoreInteractions(amplitude);
  }

  @Test public void track() {
    Properties properties = new Properties();

    integration.track(new TrackPayloadBuilder().event("foo").properties(properties).build());

    verify(amplitude).logEvent(eq("foo"), jsonEq(properties.toJsonObject()));
    verifyNoMoreInteractions(amplitude);
  }

  public void alias() {
    integration.alias(new AliasPayloadBuilder().build());

    verifyNoMoreInteractions(amplitude);
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

    verify(amplitude).logEvent(eq("foo"), jsonEq(properties.toJsonObject()));
    verify(amplitude).logRevenue("bar", 10, 20, "baz", "qux");
  }

  @Test public void identify() {
    Traits traits = createTraits("foo").putAge(20).putFirstName("bar");
    IdentifyPayload payload = new IdentifyPayloadBuilder().traits(traits).build();

    integration.identify(payload);

    verify(amplitude).setUserId("foo");
    verify(amplitude).setUserProperties(jsonEq(traits.toJsonObject()));
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());

    verifyNoMoreInteractions(amplitude);
  }

  @Test public void screen() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().category("foo").build());

    verifyNoMoreInteractions(amplitude);
  }

  @Test public void screenTrackNamedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen(new ScreenPayloadBuilder().name("bar").build());
    verifyAmplitudeLoggedEvent("Viewed bar Screen", new JSONObject());

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyNoMoreInteractions(amplitude);
  }

  @Test public void screenTrackCategorizedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = true;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyAmplitudeLoggedEvent("Viewed foo Screen", new JSONObject());

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreInteractions(amplitude);
  }

  @Test public void screenTrackAllPages() {
    integration.trackAllPages = true;
    integration.trackCategorizedPages = Randoms.nextBoolean();
    integration.trackNamedPages = Randoms.nextBoolean();

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyAmplitudeLoggedEvent("Viewed foo Screen", new JSONObject());

    integration.screen(new ScreenPayloadBuilder().name("bar").build());
    verifyAmplitudeLoggedEvent("Viewed bar Screen", new JSONObject());

    integration.screen(new ScreenPayloadBuilder().category("bar").name("baz").build());
    verifyAmplitudeLoggedEvent("Viewed baz Screen", new JSONObject());
  }

  @Test public void flush() {
    integration.flush();

    verify(amplitude).uploadEvents();
  }

  private void verifyAmplitudeLoggedEvent(String event, JSONObject jsonObject) {
    verify(amplitude).logEvent(eq(event), jsonEq(jsonObject));
  }
}
