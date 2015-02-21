package com.segment.analytics;

import android.Manifest;
import android.app.Application;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.BasePayload;
import org.assertj.core.data.MapEntry;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static com.segment.analytics.Analytics.BundledIntegration.BUGSNAG;
import static com.segment.analytics.Analytics.BundledIntegration.MIXPANEL;
import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.IntegrationManager.ActivityLifecyclePayload;
import static com.segment.analytics.TestUtils.createContext;
import static com.segment.analytics.TestUtils.mockApplication;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class AnalyticsTest {
  Application application;
  @Mock IntegrationManager integrationManager;
  @Mock SegmentDispatcher segmentDispatcher;
  @Mock Stats stats;
  @Mock Traits.Cache traitsCache;
  Traits traits;
  AnalyticsContext analyticsContext;
  @Mock Options defaultOptions;

  private Analytics analytics;

  public static void grantPermission(final Application app, final String permission) {
    ShadowApplication shadowApp = Robolectric.shadowOf(app);
    shadowApp.grantPermissions(permission);
  }

  @Before public void setUp() {
    initMocks(this);
    application = mockApplication();
    traits = Traits.create();
    when(traitsCache.get()).thenReturn(traits);
    analyticsContext = createContext(traits);
    analytics =
        new Analytics(application, integrationManager, segmentDispatcher, stats, traitsCache,
            analyticsContext, defaultOptions, NONE);

    // Used by singleton tests
    grantPermission(Robolectric.application, Manifest.permission.INTERNET);
  }

  @After public void tearDown() {
    assertThat(ShadowLog.getLogs()).isEmpty();
  }

  @Test public void registersForCallbacks() {
    verify(application) //
        .registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));
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
    verify(segmentDispatcher).dispatchEnqueue(argThat(new TypeSafeMatcher<BasePayload>() {
      @Override protected boolean matchesSafely(BasePayload item) {
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

  @Test public void alias() throws Exception {
    try {
      analytics.alias(null);
      fail("null new id should throw error");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("newId must not be null or empty.");
    }

    String anonymousId = traits.anonymousId();
    analytics.alias("foo");
    ArgumentCaptor<AliasPayload> payloadArgumentCaptor =
        ArgumentCaptor.forClass(AliasPayload.class);
    verify(segmentDispatcher).dispatchEnqueue(payloadArgumentCaptor.capture());
    assertThat(payloadArgumentCaptor.getValue()).containsEntry("previousId", anonymousId)
        .containsEntry("userId", "foo");
  }

  @Test public void submitInvokesDispatch() {
    BasePayload payload = mock(BasePayload.class);

    analytics.submit(payload);

    verify(integrationManager).dispatchOperation(payload);
    verify(segmentDispatcher).dispatchEnqueue(payload);
  }

  @Test public void flushInvokesDispatch() throws Exception {
    analytics.flush();

    verify(segmentDispatcher).dispatchFlush(0);
    verify(integrationManager).dispatchFlush();
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

  @Test public void submitLifecycleInvokesDispatch() {
    ActivityLifecyclePayload payload = mock(ActivityLifecyclePayload.class);
    analytics.submit(payload);
    verify(integrationManager).dispatchOperation(payload);
  }

  @Test public void nullIntegrationManagerIsIgnored() throws Exception {
    application = mockApplication();
    analytics =
        new Analytics(application, null, segmentDispatcher, stats, traitsCache, analyticsContext,
            defaultOptions, NONE);

    verify(application, never()) //
        .registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));

    analytics.submit(mock(BasePayload.class));
    try {
      analytics.submit(mock(ActivityLifecyclePayload.class));
      fail("submitting an activity lifecycle event should fail");
    } catch (NullPointerException ignored) {
    }
    analytics.flush();
    analytics.shutdown();
    try {
      analytics.onIntegrationReady(BUGSNAG, mock(Analytics.Callback.class));
      fail("registering callback should fail");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Enable bundled integrations to register for this callback.");
    }
  }

  @Test public void onIntegrationReady() {
    try {
      analytics.onIntegrationReady(null, mock(Analytics.Callback.class));
      fail("registering for null integration should fail");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("bundledIntegration cannot be null.");
    }

    analytics.onIntegrationReady(MIXPANEL, null);
    verify(integrationManager).dispatchRegisterCallback(MIXPANEL.key, null);
  }

  @Test public void shutdown() {
    assertThat(analytics.shutdown).isFalse();
    analytics.shutdown();
    verify(integrationManager).shutdown();
    verify(stats).shutdown();
    verify(segmentDispatcher).shutdown();
    assertThat(analytics.shutdown).isTrue();
  }

  @Test public void shutdownTwice() {
    assertThat(analytics.shutdown).isFalse();
    analytics.shutdown();
    analytics.shutdown();
    verify(integrationManager).shutdown();
    verify(stats).shutdown();
    assertThat(analytics.shutdown).isTrue();
  }

  @Test public void shutdownDisallowedOnCustomSingletonInstance() throws Exception {
    Analytics.singleton = null;
    try {
      Analytics analytics = new Analytics.Builder(Robolectric.application, "foo").build();
      Analytics.setSingletonInstance(analytics);
      analytics.shutdown();
      fail("Calling shutdown() on static singleton instance should throw");
    } catch (UnsupportedOperationException ignored) {
    }
  }

  @Test public void setSingletonInstanceMayOnlyBeCalledOnce() {
    Analytics.singleton = null;

    Analytics analytics = new Analytics.Builder(Robolectric.application, "foo").build();
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

    Analytics.setSingletonInstance(new Analytics.Builder(Robolectric.application, "foo").build());

    Analytics analytics = new Analytics.Builder(Robolectric.application, "foo").build();
    try {
      Analytics.setSingletonInstance(analytics);
      fail("Can't set singleton instance after with().");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Singleton instance already exists.");
    }
  }

  @Test public void setSingleInstanceReturnedFromWith() {
    Analytics.singleton = null;
    Analytics analytics = new Analytics.Builder(Robolectric.application, "foo").build();
    Analytics.setSingletonInstance(analytics);
    assertThat(Analytics.with(Robolectric.application)).isSameAs(analytics);
  }

  @Test public void skippingBundledIntegrationsCreatesInstancesCorrectly() {
    Analytics analytics =
        new Analytics.Builder(Robolectric.application, "foo").skipBundledIntegrations().build();
    assertThat(analytics.segmentDispatcher.integrations).isNotNull().isEmpty();
  }

  @Test public void getSnapshotInvokesStats() throws Exception {
    analytics.getSnapshot();
    verify(stats).createSnapshot();
  }
}
