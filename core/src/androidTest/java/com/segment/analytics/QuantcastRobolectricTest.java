package com.segment.analytics;

import com.quantcast.measurement.service.QuantcastClient;
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

import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(QuantcastClient.class)
public class QuantcastRobolectricTest extends IntegrationRobolectricExam {
  final String apiKey = "foo";
  @Rule public PowerMockRule rule = new PowerMockRule();
  QuantcastIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(QuantcastClient.class);
    integration = new QuantcastIntegration(true);
    integration.apiKey = apiKey;
  }

  @Test public void initialize() throws InvalidConfigurationException {
    integration.initialize(context, new JsonMap().putValue("apiKey", "foo"));
    verifyStatic();
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test
  public void activityLifecycle() {
    integration.onActivityStarted(activity);
    verifyStatic();
    QuantcastClient.activityStart(activity, "foo", null, null);

    integration.onActivityStopped(activity);
    verifyStatic();
    QuantcastClient.activityStop();

    integration.onActivityCreated(activity, bundle);
    integration.onActivityResumed(activity);
    integration.onActivityPaused(activity);
    integration.onActivitySaveInstanceState(activity, bundle);
    integration.onActivityDestroyed(activity);
    verifyStatic();
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test
  public void identify() {
    integration.identify(identifyPayload("bar"));
    verifyStatic();
    QuantcastClient.recordUserIdentifier("bar");
  }

  @Test
  public void screenTrackNothing() {
    integration.screen(screenPayload("bar", "baz"));
    verifyStatic();
    QuantcastClient.logEvent("Viewed baz Screen");
  }

  @Test
  public void track() {
    integration.track(trackPayload("bar"));
    verifyStatic();
    QuantcastClient.logEvent("bar");
  }

  @Test
  public void optOut() {
    integration.optOut(true);
    verifyStatic();
    QuantcastClient.setCollectionEnabled(true);

    integration.optOut(false);
    verifyStatic();
    QuantcastClient.setCollectionEnabled(false);
  }
}
