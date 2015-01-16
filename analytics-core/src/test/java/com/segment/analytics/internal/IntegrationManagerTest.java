package com.segment.analytics.internal;

import android.content.Context;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.model.ProjectSettings;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.TestUtils.mockApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class IntegrationManagerTest {

  IntegrationManager integrationManager;

  @Mock SegmentHTTPApi segmentHTTPApi;
  @Mock Stats stats;
  @Mock ValueMap.Cache<ProjectSettings> projectSettingsCache;
  @Mock Logger logger;

  Context context;

  @Before public void setUp() throws IOException {
    initMocks(this);

    context = mockApplication();
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);

    integrationManager =
        new IntegrationManager(context, segmentHTTPApi, projectSettingsCache, stats, logger, true);
  }

  @Test public void addsKeysCorrectly() throws Exception {
    assertThat(integrationManager.bundledIntegrations).hasSameSizeAs(
        integrationManager.integrations);
  }

  @Test public void initializesIntegrations() throws Exception {
    final AbstractIntegration mockIntegration = mock(AbstractIntegration.class);
    when(mockIntegration.key()).thenReturn("Foo");
    integrationManager.initialized = false;
    integrationManager.integrations.clear();
    integrationManager.integrations.add(mockIntegration);

    ValueMap fooMap =
        new ValueMap().putValue("trackNamedPages", true).putValue("trackAllPages", false);

    integrationManager.performInitialize(
        ProjectSettings.create(new ValueMap().putValue("Foo", fooMap), System.currentTimeMillis()));

    verify(mockIntegration).initialize(context, new ValueMap(fooMap), true);

    assertThat(integrationManager.initialized).isTrue();

    // exercise a bug where we added an integration twice, once on load and once on initialize
    assertThat(integrationManager.integrations).containsExactly(mockIntegration);
  }

  @Test public void forwardsCorrectly() {
    integrationManager.initialized = true;
    final AbstractIntegration<Void> mockIntegration = mock(AbstractIntegration.class);
    integrationManager.integrations.clear();
    integrationManager.integrations.add(mockIntegration);
    integrationManager.integrations.add(mockIntegration);
    integrationManager.integrations.add(mockIntegration);

    integrationManager.performOperation(new IntegrationManager.IntegrationOperation() {
      @Override public void run(AbstractIntegration integration) {
        integration.alias(mock(AliasPayload.class));
      }

      @Override public String id() {
        return null;
      }
    });
    verify(mockIntegration, times(3)).alias(any(AliasPayload.class));

    integrationManager.performOperation(new IntegrationManager.IntegrationOperation() {
      @Override public void run(AbstractIntegration integration) {
        integration.flush();
      }

      @Override public String id() {
        return null;
      }
    });
    verify(mockIntegration, times(3)).flush();
  }
}
