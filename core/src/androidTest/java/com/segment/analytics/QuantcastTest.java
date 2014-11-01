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
public class QuantcastTest extends IntegrationExam {
  final String apiKey = "foo";
  @Rule public PowerMockRule rule = new PowerMockRule();
  QuantcastIntegrationAdapter adapter;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(QuantcastClient.class);
    adapter = new QuantcastIntegrationAdapter(true);
    adapter.apiKey = apiKey;
  }

  @Test public void initialize() throws InvalidConfigurationException {
    adapter.initialize(context, new JsonMap().putValue("apiKey", "foo"));
    verifyStatic();
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test
  public void activityLifecycle() {
    adapter.onActivityStarted(activity);
    verifyStatic();
    QuantcastClient.activityStart(activity, "foo", null, null);

    adapter.onActivityStopped(activity);
    verifyStatic();
    QuantcastClient.activityStop();

    adapter.onActivityCreated(activity, bundle);
    adapter.onActivityResumed(activity);
    adapter.onActivityPaused(activity);
    adapter.onActivitySaveInstanceState(activity, bundle);
    adapter.onActivityDestroyed(activity);
    verifyStatic();
    verifyNoMoreInteractions(QuantcastClient.class);
  }

  @Test
  public void identify() {
    adapter.identify(identifyPayload("bar"));
    verifyStatic();
    QuantcastClient.recordUserIdentifier("bar");
  }

  @Test
  public void screenTrackNothing() {
    adapter.screen(screenPayload("bar", "baz"));
    verifyStatic();
    QuantcastClient.logEvent("Viewed baz Screen");
  }

  @Test
  public void track() {
    adapter.track(trackPayload("bar"));
    verifyStatic();
    QuantcastClient.logEvent("bar");
  }

  @Test
  public void optOut() {
    adapter.optOut(true);
    verifyStatic();
    QuantcastClient.setCollectionEnabled(true);

    adapter.optOut(false);
    verifyStatic();
    QuantcastClient.setCollectionEnabled(false);
  }
}
