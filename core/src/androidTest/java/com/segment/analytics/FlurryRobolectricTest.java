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
  FlurryIntegrationAdapter adapter;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(FlurryAgent.class);
    adapter = new FlurryIntegrationAdapter(true);
    adapter.apiKey = apiKey;
  }

  @Test public void initialize() throws InvalidConfigurationException {
    adapter.initialize(context, //
        new JsonMap().putValue("apiKey", apiKey)
            .putValue("sessionContinueSeconds", 20)
            .putValue("captureUncaughtExceptions", true)
            .putValue("useHttps", false)
    );
    verifyStatic();
    FlurryAgent.setContinueSessionMillis(20000);
    verifyStatic();
    FlurryAgent.setCaptureUncaughtExceptions(true);
    verifyStatic();
    FlurryAgent.setUseHttps(false);
  }

  @Test public void activityLifecycle() {
    adapter.onActivityStarted(activity);
    verifyStatic();
    FlurryAgent.onStartSession(activity, apiKey);

    adapter.onActivityStopped(activity);
    verifyStatic();
    FlurryAgent.onEndSession(activity);

    adapter.onActivityResumed(activity);
    adapter.onActivityPaused(activity);
    adapter.onActivityCreated(activity, bundle);
    adapter.onActivityDestroyed(activity);
    adapter.onActivitySaveInstanceState(activity, bundle);
    verifyStatic();
    verifyNoMoreInteractions(FlurryAgent.class);
  }

  @Test public void screen() {
    adapter.screen(screenPayload("bar", "baz"));
    verifyStatic();
    FlurryAgent.onPageView();
    verifyStatic();
    FlurryAgent.logEvent(eq("baz"), Matchers.<Map<String, String>>any());
  }

  @Test public void track() {
    adapter.track(trackPayload("bar"));
    verifyStatic();
    FlurryAgent.logEvent(eq("bar"), Matchers.<Map<String, String>>any());
  }

  @Test public void identify() {
    adapter.identify(identifyPayload("bar"));
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
    adapter.identify(identifyPayload);
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
