package com.segment.analytics;

import android.Manifest;
import android.app.Application;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.integrations.AliasPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Log;
import com.segment.analytics.internal.Utils.AnalyticsExecutorService;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import org.assertj.core.data.MapEntry;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.TestUtils.SynchronousExecutor;
import static com.segment.analytics.TestUtils.mockApplication;
import static com.segment.analytics.Utils.createContext;
import static com.segment.analytics.internal.Utils.DEFAULT_FLUSH_INTERVAL;
import static com.segment.analytics.internal.Utils.DEFAULT_FLUSH_QUEUE_SIZE;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
public class AnalyticsTest {

  @Mock Traits.Cache traitsCache;
  @Mock Options defaultOptions;
  @Spy AnalyticsExecutorService networkExecutor;
  @Spy ExecutorService analyticsExecutor = new SynchronousExecutor();
  @Mock Client client;
  @Mock Stats stats;
  @Mock ProjectSettings.Cache projectSettingsCache;
  @Mock Integration integration;
  @Mock Integration.Factory factory;
  Application application;
  Traits traits;
  AnalyticsContext analyticsContext;

  private Analytics analytics;

  public static void grantPermission(final Application app, final String permission) {
    ShadowApplication shadowApp = Shadows.shadowOf(app);
    shadowApp.grantPermissions(permission);
  }

  @Before public void setUp() {
    Analytics.INSTANCES.clear();

    initMocks(this);
    application = mockApplication();
    traits = Traits.create();
    when(traitsCache.get()).thenReturn(traits);
    analyticsContext = createContext(traits);
    when(factory.create(any(ValueMap.class), eq(analytics))).thenReturn(integration);

    analytics = new Analytics(application, networkExecutor, stats, traitsCache, analyticsContext,
        defaultOptions, new Log(NONE), "qaz", Collections.singletonMap("test", factory), client,
        Cartographer.INSTANCE, projectSettingsCache, "foo", DEFAULT_FLUSH_QUEUE_SIZE,
        DEFAULT_FLUSH_INTERVAL, analyticsExecutor);

    // Used by singleton tests
    grantPermission(RuntimeEnvironment.application, Manifest.permission.INTERNET);
  }

  @After public void tearDown() {
    assertThat(ShadowLog.getLogs()).isEmpty();
  }

  @Test public void identify() {
    try {
      analytics.identify(null, null, null);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Either userId or some traits must be provided.");
    }
  }

  @Test public void identifyUpdatesCache() {
    analytics.identify("foo", new Traits().putValue("bar", "qaz"), null);

    assertThat(traits).contains(MapEntry.entry("userId", "foo"))
        .contains(MapEntry.entry("bar", "qaz"));
    assertThat(analyticsContext.traits()).contains(MapEntry.entry("userId", "foo"))
        .contains(MapEntry.entry("bar", "qaz"));
    verify(traitsCache).set(traits);
    verify(integration).identify(argThat(new TypeSafeMatcher<IdentifyPayload>() {
      @Override protected boolean matchesSafely(IdentifyPayload item) {
        // Exercises a bug where payloads didn't pick up userId in identify correctly.
        // https://github.com/segmentio/analytics-android/issues/169
        return item.userId().equals("foo");
      }

      @Override public void describeTo(Description description) {
      }
    }));
  }

  @Test public void group() throws Exception {
    try {
      analytics.group(null);
      fail("null groupId should throw exception");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("groupId must not be null or empty.");
    }

    try {
      analytics.group("");
      fail("empty groupId and name should throw exception");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("groupId must not be null or empty.");
    }
  }

  @Test public void track() {
    try {
      analytics.track(null);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("event must not be null or empty.");
    }
    try {
      analytics.track("   ");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("event must not be null or empty.");
    }
  }

  @Test public void screen() throws Exception {
    try {
      analytics.screen(null, null);
      fail("null category and name should throw exception");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("either category or name must be provided.");
    }

    try {
      analytics.screen("", "");
      fail("empty category and name should throw exception");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("either category or name must be provided.");
    }
  }

  @Test public void invalidAliasThrowsError() {
    try {
      analytics.alias(null);
      fail("null new id should throw error");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("newId must not be null or empty.");
    }
  }

  @Test public void alias() {
    String anonymousId = traits.anonymousId();
    analytics.alias("foo");
    ArgumentCaptor<AliasPayload> payloadArgumentCaptor =
        ArgumentCaptor.forClass(AliasPayload.class);
    verify(integration).alias(payloadArgumentCaptor.capture());
    assertThat(payloadArgumentCaptor.getValue()).containsEntry("previousId", anonymousId)
        .containsEntry("userId", "foo");
  }

  @Test public void flush() throws Exception {
    analytics.flush();

    verify(integration).flush();
  }

  @Test public void getSnapshot() throws Exception {
    analytics.getSnapshot();

    verify(stats).createSnapshot();
  }

  @Test public void logoutClearsTraitsAndUpdatesContext() {
    analyticsContext.setTraits(new Traits().putAge(20).putAvatar("bar"));

    analytics.logout();

    verify(traitsCache).delete();
    verify(traitsCache).set(argThat(new TypeSafeMatcher<Traits>() {
      @Override protected boolean matchesSafely(Traits traits) {
        return !isNullOrEmpty(traits.anonymousId());
      }

      @Override public void describeTo(Description description) {
      }
    }));
    assertThat(analyticsContext.traits()).hasSize(1).containsKey("anonymousId");
  }

  @Test public void onIntegrationReadyShouldFailForNullKey() {
    try {
      analytics.onIntegrationReady((String) null, mock(Analytics.Callback.class));
      fail("registering for null integration should fail");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("key cannot be null or empty.");
    }
  }

  @Test public void onIntegrationReady() {
    Analytics.Callback<Void> callback = mock(Analytics.Callback.class);
    analytics.onIntegrationReady((String) null, mock(Analytics.Callback.class));
    verify(callback).onReady(null);
  }

  @Test public void shutdown() {
    assertThat(analytics.shutdown).isFalse();
    analytics.shutdown();
    verify(stats).shutdown();
    verify(networkExecutor).shutdown();
    assertThat(analytics.shutdown).isTrue();

    try {
      analytics.track("foo");
      fail("Enqueuing a message after shutdown should throw.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Cannot enqueue messages after client is shutdown.");
    }

    try {
      analytics.flush();
      fail("Enqueuing a message after shutdown should throw.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Cannot enqueue messages after client is shutdown.");
    }
  }

  @Test public void shutdownTwice() {
    assertThat(analytics.shutdown).isFalse();
    analytics.shutdown();
    analytics.shutdown();
    verify(stats).shutdown();
    assertThat(analytics.shutdown).isTrue();
  }

  @Test public void shutdownDisallowedOnCustomSingletonInstance() throws Exception {
    Analytics.singleton = null;
    try {
      Analytics analytics = new Analytics.Builder(RuntimeEnvironment.application, "foo").build();
      Analytics.setSingletonInstance(analytics);
      analytics.shutdown();
      fail("Calling shutdown() on static singleton instance should throw");
    } catch (UnsupportedOperationException ignored) {
    }
  }

  @Test public void setSingletonInstanceMayOnlyBeCalledOnce() {
    Analytics.singleton = null;

    Analytics analytics = new Analytics.Builder(RuntimeEnvironment.application, "foo").build();
    Analytics.setSingletonInstance(analytics);

    try {
      Analytics.setSingletonInstance(analytics);
      fail("Can't set singleton instance twice.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Singleton instance already exists.");
    }
  }

  @Test public void setSingletonInstanceAfterWithFails() {
    Analytics.singleton = null;

    Analytics.setSingletonInstance(new Analytics.Builder(RuntimeEnvironment.application, "foo") //
        .build());

    Analytics analytics = new Analytics.Builder(RuntimeEnvironment.application, "bar").build();
    try {
      Analytics.setSingletonInstance(analytics);
      fail("Can't set singleton instance after with().");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Singleton instance already exists.");
    }
  }

  @Test public void setSingleInstanceReturnedFromWith() {
    Analytics.singleton = null;
    Analytics analytics = new Analytics.Builder(RuntimeEnvironment.application, "foo").build();
    Analytics.setSingletonInstance(analytics);
    assertThat(Analytics.with(RuntimeEnvironment.application)).isSameAs(analytics);
  }

  @Test public void multipleInstancesWithSameTagThrows() throws Exception {
    new Analytics.Builder(RuntimeEnvironment.application, "foo").build();
    try {
      new Analytics.Builder(RuntimeEnvironment.application, "bar").tag("foo").build();
      fail("Creating client with duplicate should throw.");
    } catch (IllegalStateException expected) {
      assertThat(expected) //
          .hasMessageContaining("Duplicate analytics client created with tag: foo.");
    }
  }

  @Test public void multipleInstancesWithSameTagIsAllowedAfterShutdown() throws Exception {
    new Analytics.Builder(RuntimeEnvironment.application, "foo").build().shutdown();
    new Analytics.Builder(RuntimeEnvironment.application, "bar").tag("foo").build();
  }

  @Test public void getSnapshotInvokesStats() throws Exception {
    analytics.getSnapshot();
    verify(stats).createSnapshot();
  }
}
