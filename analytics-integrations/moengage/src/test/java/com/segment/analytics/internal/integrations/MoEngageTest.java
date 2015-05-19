package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import com.moe.pushlibrary.MoEHelper;
import com.moe.pushlibrary.utils.MoEHelperConstants;
import com.segment.analytics.Analytics;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Properties;
import com.segment.analytics.TestUtils;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.createContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(MoEngageIntegration.class) public class MoEngageTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();

  @Mock Application application;
  @Mock Context context;
  @Mock Analytics analytics;
  @Mock MoEHelper moeHelper;
  MoEngageIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    mockStatic(MoEHelper.class);
    integration = new MoEngageIntegration();
    integration.helper = moeHelper;

  }

  @Test public void initialize() {
    when(analytics.getApplication()).thenReturn(application);

    integration.initialize(analytics, new ValueMap());
    Analytics.LogLevel logLevel = analytics.getLogLevel();
    if (logLevel == Analytics.LogLevel.VERBOSE || logLevel == Analytics.LogLevel.INFO) {
      assertThat(MoEHelper.APP_DEBUG).isTrue();
    }else{
      assertThat(MoEHelper.APP_DEBUG).isFalse();
    }
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verify(moeHelper).onStart(activity);
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verify(moeHelper).onResume(activity);
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verify(moeHelper).onPause(activity);
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verify(moeHelper).onStop(activity);
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verify(moeHelper).onSaveInstanceState(bundle);
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void track() {

    TrackPayloadBuilder builder = new TrackPayloadBuilder();
    builder.event("foo").properties(new Properties().putCurrency("INR").putPrice(2000));
    integration.track(builder.build());

    Map<String, String> eventProperties = new HashMap<>();
    eventProperties.put("currency", "INR");
    eventProperties.put("price", "2000.0");
    verify(moeHelper).trackEvent("foo", eventProperties);
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void identify() {
    Traits traits = TestUtils.createTraits();
    traits.putEmail("foo@bar.com");
    HashMap<String, Object> traitMap = new HashMap<>();
    traitMap.put(MoEHelperConstants.USER_ATTRIBUTE_USER_EMAIL, traits.email());
    traitMap.put("USER_ATTRIBUTE_SEGMENT_ID", traits.anonymousId());
    AnalyticsContext analyticsContext = createContext(traits).putLocation(
        new AnalyticsContext.Location().putLatitude(20).putLongitude(20));

    integration.identify(
        new IdentifyPayloadBuilder().traits(traits).context(analyticsContext).build());

    verify(moeHelper).setUserAttribute(traitMap);
    verify(moeHelper).setUserLocation(20, 20);

    verifyNoMoreInteractions(moeHelper);
    verifyNoMoreInteractions(MoEHelper.class);

  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }

  @Test public void reset() {
    integration.reset();
    verify(moeHelper).logoutUser();
    verifyNoMoreInteractions(MoEHelper.class);
    verifyNoMoreInteractions(moeHelper);
  }
}