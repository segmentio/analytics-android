package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.amplitude.api.AmplitudeClient;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
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

import static com.segment.analytics.TestUtils.createTraits;
import static com.segment.analytics.TestUtils.jsonEq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
// The two lines below are only required if your API uses only static methods
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(MyToolSDK.class)
public class MyToolTest {

  // Only required if your API uses only static methods
  @Rule public PowerMockRule powerMockRule = new PowerMockRule();


  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
  @Mock Application context;
  @Mock Analytics analytics;
  AmplitudeIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    integration = new MyToolIntegration();
    integration.amplitude = amplitude;
  }

  @Test public void initialize() {
    integration.initialize(analytics, //
        new ValueMap().putValue("someSetting", "foo")
            .putValue("anotherSetting", true));

    // TODO: verify your SDKs initialize method was called or not
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);

    integration.onActivityCreated(activity, bundle);

    // TODO: verify your SDKs onActivityCreated method was called or not
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);

    integration.onActivityStarted(activity);

    // TODO: verify your SDKs onActivityStarted method was called or not
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);

    integration.onActivityResumed(activity);

    // TODO: verify your SDKs onActivityResumed method was called or not
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);

    integration.onActivityPaused(activity);

    // TODO: verify your SDKs onActivityPaused method was called or not
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);

    integration.onActivityStopped(activity);

    // TODO: verify your SDKs onActivityStopped method was called or not
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);

    integration.onActivitySaveInstanceState(activity, bundle);

    // TODO: verify your SDKs onActivitySaveInstanceState method was called or not
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);

    integration.onActivityDestroyed(activity);

    // TODO: verify your SDKs onActivityDestroyed method was called or not
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().build());

    // TODO: verify your SDKs track method was called or not
  }

  public void alias() {
    integration.alias(new AliasPayloadBuilder().build());

    // TODO: verify your SDKs alias method was called or not
  }

  @Test public void identify() {
    integration.identify(new IdentifyPayloadBuilder().build());

    // TODO: verify your SDKs identify method was called or not
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());

    // TODO: verify your SDKs group method was called or not
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());

    // TODO: verify your SDKs screen method was called or not
  }

  @Test public void flush() {
    integration.flush();

    // TODO: verify your SDKs flush method was called or not
  }

  @Test public void reset() {
    integration.reset();

    // TODO: verify your SDKs reset method was called or not
  }
}
