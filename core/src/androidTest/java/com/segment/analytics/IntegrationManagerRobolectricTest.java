package com.segment.analytics;

import android.content.Context;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.TestUtils.mockApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class IntegrationManagerRobolectricTest {

  IntegrationManager integrationManager;

  @MockitoAnnotations.Mock SegmentHTTPApi segmentHTTPApi;
  @MockitoAnnotations.Mock Stats stats;
  @MockitoAnnotations.Mock StringCache stringCache;
  Context context;

  @Before public void setUp() {
    initMocks(this);
    context = mockApplication();
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    integrationManager =
        new IntegrationManager(context, segmentHTTPApi, stringCache, stats, 20, 30, "foo", true);
  }

  @Test public void createsIntegrationsCorrectly() {
    assertThat(integrationManager.createIntegrationForKey("Amplitude")).isInstanceOf(
        AmplitudeIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("AppsFlyer")).isInstanceOf(
        AppsFlyerIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Bugsnag")).isInstanceOf(
        BugsnagIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Countly")).isInstanceOf(
        CountlyIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Crittercism")).isInstanceOf(
        CrittercismIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Flurry")).isInstanceOf(
        FlurryIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Google Analytics")).isInstanceOf(
        GoogleAnalyticsIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Leanplum")).isInstanceOf(
        LeanplumIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Localytics")).isInstanceOf(
        LocalyticsIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Mixpanel")).isInstanceOf(
        MixpanelIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Quantcast")).isInstanceOf(
        QuantcastIntegration.class);
    assertThat(integrationManager.createIntegrationForKey("Tapstream")).isInstanceOf(
        TapstreamIntegration.class);

    try {
      assertThat(integrationManager.createIntegrationForKey("d"));
      fail("passing an invalid key should fail.");
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("unknown integration key: d");
    }
  }

  @Test public void initializesIntegrations() {
    final AbstractIntegration<Void> fooIntegration = mock(AbstractIntegration.class);
    IntegrationManager integrationManager =
        new IntegrationManager(context, segmentHTTPApi, stringCache, stats, 20, 30, "bar", true) {
          @Override AbstractIntegration createIntegrationForKey(String key) {
            if ("Foo".equals(key)) {
              return fooIntegration;
            }
            throw new IllegalArgumentException("unknown key: " + key);
          }
        };

    integrationManager.bundledIntegrations.clear();
    integrationManager.bundledIntegrations.put("Foo", false);
    integrationManager.initializeIntegrations(
        ProjectSettings.create("{\"Foo\":{\"trackNamedPages\":true,\"trackAllPages\":false}}",
            System.currentTimeMillis()));

    try {
      verify(fooIntegration).initialize(context,
          new JsonMap("{\"trackNamedPages\":true,\"trackAllPages\":false}"), true);
    } catch (InvalidConfigurationException ignored) {
      fail("unexpected exception: ", ignored);
    }

    assertThat(integrationManager.initialized).isTrue();
    assertThat(integrationManager.integrations).containsExactly(fooIntegration);
  }

  @Test public void forwardsCorrectly() {
    integrationManager.initialized = true;
    final AbstractIntegration<Void> fooIntegration = mock(AbstractIntegration.class);
    integrationManager.integrations = new ArrayList<AbstractIntegration>();
    integrationManager.integrations.add(fooIntegration);

    integrationManager.run(new IntegrationManager.IntegrationOperation() {
      @Override public void run(AbstractIntegration integration) {
        integration.alias(mock(AliasPayload.class));
      }

      @Override public String id() {
        return null;
      }
    });
    verify(fooIntegration).alias(Matchers.<AliasPayload>any());
  }
}

