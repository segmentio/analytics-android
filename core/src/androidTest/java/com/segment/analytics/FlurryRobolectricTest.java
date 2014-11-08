package com.segment.analytics;

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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(FlurryAgent.class)
public class FlurryRobolectricTest extends IntegrationRobolectricExam {
  final String apiKey = "foo";
  @Rule public PowerMockRule rule = new PowerMockRule();
  FlurryIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(FlurryAgent.class);
    integration = new FlurryIntegration();
    integration.apiKey = apiKey;
  }

  @Test public void initialize() throws InvalidConfigurationException {
    integration.initialize(context, //
        new JsonMap().putValue("apiKey", apiKey)
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

  @Test public void activityLifecycle() {
    integration.onActivityStarted(activity);
    verifyStatic();
    FlurryAgent.onStartSession(activity, apiKey);

    integration.onActivityStopped(activity);
    verifyStatic();
    FlurryAgent.onEndSession(activity);

    integration.onActivityResumed(activity);
    integration.onActivityPaused(activity);
    integration.onActivityCreated(activity, bundle);
    integration.onActivityDestroyed(activity);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void screen() {
    integration.screen(screenPayload("bar", "baz"));
    verifyStatic();
    FlurryAgent.onPageView();
    verifyStatic();
    FlurryAgent.logEvent(eq("baz"), Matchers.<Map<String, String>>any());
  }

  @Test public void track() {
    integration.track(trackPayload("bar"));
    verifyStatic();
    FlurryAgent.logEvent(eq("bar"), Matchers.<Map<String, String>>any());
  }

  @Test public void identify() {
    integration.identify(identifyPayload("bar"));
    verifyStatic();
    FlurryAgent.setUserId("bar");
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void identifyWithTraits() {
    traits.putAge(20);
    traits.putGender("f");
    analyticsContext.putLocation(20, 20, 20);
    IdentifyPayload identifyPayload = identifyPayload("bar");
    integration.identify(identifyPayload);
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
}
