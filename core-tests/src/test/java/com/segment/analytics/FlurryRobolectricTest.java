package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.AliasPayloadBuilder;
import static com.segment.analytics.TestUtils.GroupPayloadBuilder;
import static com.segment.analytics.TestUtils.IdentifyPayloadBuilder;
import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(FlurryAgent.class)
public class FlurryRobolectricTest extends AbstractIntegrationTest {
  final String apiKey = "foo";
  @Rule public PowerMockRule rule = new PowerMockRule();
  FlurryIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(FlurryAgent.class);
    integration = new FlurryIntegration();
    integration.apiKey = apiKey;
  }

  @Test @Override public void initialize() throws IllegalStateException {
    integration.initialize(context, //
        new ValueMap().putValue("apiKey", apiKey)
            .putValue("sessionContinueSeconds", 20)
            .putValue("captureUncaughtExceptions", true)
            .putValue("useHttps", false), true);
    verifyStatic();
    FlurryAgent.setContinueSessionMillis(20000);
    verifyStatic();
    FlurryAgent.setCaptureUncaughtExceptions(true);
    verifyStatic();
    FlurryAgent.setUseHttps(false);
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    FlurryAgent.onStartSession(activity, apiKey);
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    FlurryAgent.onEndSession(activity);
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test @Override public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("foo").category("bar").build());
    verifyStatic();
    FlurryAgent.onPageView();
    verifyStatic();
    FlurryAgent.logEvent(eq("foo"), Matchers.<Map<String, String>>any());
  }

  @Test @Override public void flush() {
    integration.flush();
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test @Override public void track() {
    integration.track(new TrackPayloadBuilder().event("bar").build());
    verifyStatic();
    FlurryAgent.logEvent(eq("bar"), Matchers.<Map<String, String>>any());
  }

  @Test @Override public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test @Override public void identify() {
    integration.identify(new IdentifyPayloadBuilder().userId("foo").build());
    verifyStatic();
    FlurryAgent.setUserId("foo");
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void identifyWithTraits() {
    Traits traits = new Traits().putAge(20).putGender("f").putUserId("bar");
    AnalyticsContext analyticsContext =
        new AnalyticsContext(Robolectric.application, traits).putLocation(20, 20, 20);
    integration.identify(
        new IdentifyPayloadBuilder().traits(traits).context(analyticsContext).build());
    verifyStatic();
    FlurryAgent.setUserId("bar");
    verifyStatic();
    FlurryAgent.setAge(20);
    verifyStatic();
    FlurryAgent.setGender(Constants.FEMALE);
    verifyStatic();
    FlurryAgent.setLocation(20, 20);
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test @Override public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }
}
