package com.segment.analytics;

import android.content.Context;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.TestUtils.mockApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class IntegrationManagerRobolectricTest {

  IntegrationManager integrationManager;

  @Mock SegmentHTTPApi segmentHTTPApi;
  @Mock Stats stats;
  @Mock StringCache stringCache;
  @Mock Logger logger;

  Context context;

  @Before public void setUp() {
    initMocks(this);
    context = mockApplication();
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    integrationManager =
        new IntegrationManager(context, segmentHTTPApi, stringCache, stats, 20, 30, "foo", logger,
            true);
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

    try {
      assertThat(integrationManager.createIntegrationForKey("Mixed"));
      fail("passing an invalid key should fail.");
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("unknown integration key: Mixed");
    }

    try {
      assertThat(integrationManager.createIntegrationForKey("Mix"));
      fail("passing an invalid key should fail.");
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("unknown integration key: Mix");
    }

    try {
      assertThat(integrationManager.createIntegrationForKey("Mixpaneled"));
      fail("passing an invalid key should fail.");
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage("unknown integration key: Mixpaneled");
    }
  }

  @Test public void initializesIntegrations() {
    final AbstractIntegration<Void> fooIntegration = mock(AbstractIntegration.class);
    IntegrationManager integrationManager =
        new IntegrationManager(context, segmentHTTPApi, stringCache, stats, 20, 30, "bar", logger,
            true) {
          @Override AbstractIntegration createIntegrationForKey(String key) {
            if ("Foo".equals(key)) {
              return fooIntegration;
            }
            throw new IllegalArgumentException("unknown key: " + key);
          }
        };

    integrationManager.bundledIntegrations.clear();
    integrationManager.bundledIntegrations.put("Foo", false);
    integrationManager.performInitialize(
        ProjectSettings.create("{\"Foo\":{\"trackNamedPages\":true,\"trackAllPages\":false}}",
            System.currentTimeMillis()));

    try {
      verify(fooIntegration).initialize(context,
          new JsonMap("{\"trackNamedPages\":true,\"trackAllPages\":false}"), true);
    } catch (IllegalStateException ignored) {
      fail("unexpected exception: ", ignored);
    }

    assertThat(integrationManager.initialized).isTrue();
    assertThat(integrationManager.integrations).containsExactly(fooIntegration);
  }

  @Test public void forwardsCorrectly() {
    integrationManager.initialized = true;
    final AbstractIntegration<Void> fooIntegration = mock(AbstractIntegration.class);
    integrationManager.integrations =
        Arrays.<AbstractIntegration>asList(fooIntegration, fooIntegration, fooIntegration);

    integrationManager.performOperation(new IntegrationManager.IntegrationOperation() {
      @Override public void run(AbstractIntegration integration) {
        integration.alias(mock(AliasPayload.class));
      }

      @Override public String id() {
        return null;
      }
    });
    verify(fooIntegration, times(3)).alias(any(AliasPayload.class));

    integrationManager.performOperation(new IntegrationManager.IntegrationOperation() {
      @Override public void run(AbstractIntegration integration) {
        integration.flush();
      }

      @Override public String id() {
        return null;
      }
    });
    verify(fooIntegration, times(3)).flush();
  }
}

