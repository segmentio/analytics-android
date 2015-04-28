package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;
import com.segment.analytics.Analytics;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.TestUtils.createContext;
import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(FlurryAgent.class)
public class FlurryTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
  @Mock Application context;
  @Mock Analytics analytics;
  FlurryIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(FlurryAgent.class);
    integration = new FlurryIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    when(analytics.getApplication()).thenReturn(context);

    integration.initialize(analytics, new ValueMap() //
        .putValue("apiKey", "foo")
        .putValue("sessionContinueSeconds", 20)
        .putValue("captureUncaughtExceptions", true)
        .putValue("reportLocation", false));

    verifyStatic();
    FlurryAgent.setContinueSessionMillis(20000);
    verifyStatic();
    FlurryAgent.setCaptureUncaughtExceptions(true);
    verifyStatic();
    FlurryAgent.setReportLocation(false);
    verifyStatic();
    FlurryAgent.setLogEnabled(false);
    verifyStatic();
    FlurryAgent.setLogEvents(false);
    verifyStatic();
    FlurryAgent.init(context, "foo");
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    FlurryAgent.onStartSession(activity);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    FlurryAgent.onEndSession(activity);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("foo").category("bar").build());
    verifyStatic();
    FlurryAgent.onPageView();
    verifyStatic();
    FlurryAgent.logEvent(eq("foo"), Matchers.<Map<String, String>>any());
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("bar").build());
    verifyStatic();
    FlurryAgent.logEvent(eq("bar"), Matchers.<Map<String, String>>any());
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void identify() {
    integration.identify(new IdentifyPayloadBuilder().traits(createTraits("foo")).build());
    verifyStatic();
    FlurryAgent.setUserId("foo");
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void identifyWithTraits() {
    Traits traits = createTraits("bar").putAge(3).putGender("f");
    AnalyticsContext analyticsContext = createContext(traits).putLocation(
        new AnalyticsContext.Location().putLatitude(20).putLongitude(20));
    integration.identify(
        new IdentifyPayloadBuilder().traits(traits).context(analyticsContext).build());
    verifyStatic();
    FlurryAgent.setUserId("bar");
    verifyStatic();
    FlurryAgent.setAge(3);
    verifyStatic();
    FlurryAgent.setGender(Constants.FEMALE);
    verifyStatic();
    FlurryAgent.setLocation(20, 20);
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void reset() {
    integration.reset();
    verifyNoMoreInteractions(FlurryAgent.class);
  }
}
