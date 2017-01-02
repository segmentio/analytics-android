package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import com.segment.analytics.TestUtils.NoDescriptionMatcher;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.integrations.AliasPayload;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import com.segment.analytics.internal.Utils.AnalyticsNetworkExecutorService;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static android.content.Context.MODE_PRIVATE;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class AnalyticsTest {
  private static final String SETTINGS = "{\n"
      + "  \"integrations\": {\n"
      + "    \"test\": {\n"
      + "      \"foo\": \"bar\"\n"
      + "    }\n"
      + "  },\n"
      + "  \"plan\": {\n"
      + "    \n"
      + "  }\n"
      + "}";

  @Mock Traits.Cache traitsCache;
  @Mock Options defaultOptions;
  @Spy AnalyticsNetworkExecutorService networkExecutor;
  @Spy ExecutorService analyticsExecutor = new SynchronousExecutor();
  @Mock Client client;
  @Mock Stats stats;
  @Mock ProjectSettings.Cache projectSettingsCache;
  @Mock Integration integration;
  Integration.Factory factory;
  BooleanPreference optOut;
  Application application;
  Traits traits;
  AnalyticsContext analyticsContext;

  private Analytics analytics;

  public static void grantPermission(final Application app, final String permission) {
    ShadowApplication shadowApp = Shadows.shadowOf(app);
    shadowApp.grantPermissions(permission);
  }

  @Before public void setUp() throws IOException {
    Analytics.INSTANCES.clear();

    initMocks(this);
    application = mockApplication();
    traits = Traits.create();
    when(traitsCache.get()).thenReturn(traits);
    analyticsContext = createContext(traits);
    factory = new Integration.Factory() {
      @Override public Integration<?> create(ValueMap settings, Analytics analytics) {
        return integration;
      }

      @Override public String key() {
        return "test";
      }
    };
    when(projectSettingsCache.get()) //
        .thenReturn(ProjectSettings.create(Cartographer.INSTANCE.fromJson(SETTINGS)));

    SharedPreferences sharedPreferences =
        RuntimeEnvironment.application.getSharedPreferences("analytics-test-qaz", MODE_PRIVATE);
    optOut = new BooleanPreference(sharedPreferences, "opt-out-test", false);

    analytics = new Analytics(application, networkExecutor, stats, traitsCache, analyticsContext,
        defaultOptions, Logger.with(NONE), "qaz", Collections.singletonList(factory), client,
        Cartographer.INSTANCE, projectSettingsCache, "foo", DEFAULT_FLUSH_QUEUE_SIZE,
        DEFAULT_FLUSH_INTERVAL, analyticsExecutor, false, new CountDownLatch(0), false, false,
        optOut, Crypto.none());

    // Used by singleton tests.
    grantPermission(RuntimeEnvironment.application, Manifest.permission.INTERNET);
  }

  @After public void tearDown() {
    RuntimeEnvironment.application.getSharedPreferences("analytics-android-qaz", MODE_PRIVATE)
        .edit()
        .clear()
        .commit();
    assertThat(ShadowLog.getLogs()).isEmpty();
  }

  @Test public void invalidIdentify() {
    try {
      analytics.identify(null, null, null);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Either userId or some traits must be provided.");
    }
  }

  @Test public void identify() {
    analytics.identify("prateek", new Traits().putUsername("f2prateek"), null);

    verify(integration).identify(argThat(new NoDescriptionMatcher<IdentifyPayload>() {
      @Override protected boolean matchesSafely(IdentifyPayload item) {
        return item.userId().equals("prateek") && item.traits().username().equals("f2prateek");
      }
    }));
  }

  @Test public void identifyUpdatesCache() {
    analytics.identify("foo", new Traits().putValue("bar", "qaz"), null);

    assertThat(traits).contains(MapEntry.entry("userId", "foo"))
        .contains(MapEntry.entry("bar", "qaz"));
    assertThat(analyticsContext.traits()).contains(MapEntry.entry("userId", "foo"))
        .contains(MapEntry.entry("bar", "qaz"));
    verify(traitsCache).set(traits);
    verify(integration).identify(argThat(new NoDescriptionMatcher<IdentifyPayload>() {
      @Override protected boolean matchesSafely(IdentifyPayload item) {
        // Exercises a bug where payloads didn't pick up userId in identify correctly.
        // https://github.com/segmentio/analytics-android/issues/169
        return item.userId().equals("foo");
      }
    }));
  }

  @Test public void invalidGroup() {
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

  @Test public void group() {
    analytics.group("segment", new Traits().putEmployees(42), null);

    verify(integration).group(argThat(new NoDescriptionMatcher<GroupPayload>() {
      @Override protected boolean matchesSafely(GroupPayload item) {
        return item.groupId().equals("segment") && item.traits().employees() == 42;
      }
    }));
  }

  @Test public void invalidTrack() {
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

  @Test public void track() {
    analytics.track("wrote tests", new Properties().putUrl("github.com"));
    verify(integration).track(argThat(new NoDescriptionMatcher<TrackPayload>() {
      @Override protected boolean matchesSafely(TrackPayload payload) {
        return payload.event().equals("wrote tests") && //
            payload.properties().url().equals("github.com");
      }
    }));
  }

  @Test public void invalidScreen() throws Exception {
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

  @Test public void screen() {
    analytics.screen("android", "saw tests", new Properties().putUrl("github.com"));
    verify(integration).screen(argThat(new NoDescriptionMatcher<ScreenPayload>() {
      @Override protected boolean matchesSafely(ScreenPayload payload) {
        return payload.name().equals("saw tests") && //
            payload.category().equals("android") && //
            payload.properties().url().equals("github.com");
      }
    }));
  }

  @Test public void optionsDisableIntegrations() {
    analytics.screen("foo", "bar", null, new Options().setIntegration("test", false));
    analytics.track("foo", null, new Options().setIntegration("test", false));
    analytics.group("foo", null, new Options().setIntegration("test", false));
    analytics.identify("foo", null, new Options().setIntegration("test", false));
    analytics.alias("foo", new Options().setIntegration("test", false));

    analytics.screen("foo", "bar", null,
        new Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false));
    analytics.track("foo", null, new Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false));
    analytics.group("foo", null, new Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false));
    analytics.identify("foo", null,
        new Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false));
    analytics.alias("foo", new Options().setIntegration(Options.ALL_INTEGRATIONS_KEY, false));

    verifyNoMoreInteractions(integration);
  }

  @Test public void optOutDisablesEvents() throws IOException {
    analytics.optOut(true);
    analytics.track("foo");
    verifyNoMoreInteractions(integration);
  }

  @Test public void emptyTrackingPlan() throws IOException {
    analytics.projectSettings = ProjectSettings.create(Cartographer.INSTANCE.fromJson("{\n"
        + "  \"integrations\": {\n"
        + "    \"test\": {\n"
        + "      \"foo\": \"bar\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"plan\": {\n"
        + "  }\n"
        + "}"));

    analytics.track("foo");
    verify(integration).track(argThat(new NoDescriptionMatcher<TrackPayload>() {
      @Override protected boolean matchesSafely(TrackPayload payload) {
        return payload.event().equals("foo");
      }
    }));
    verifyNoMoreInteractions(integration);
  }

  @Test public void emptyEventPlan() throws IOException {
    analytics.projectSettings = ProjectSettings.create(Cartographer.INSTANCE.fromJson("{\n"
        + "  \"integrations\": {\n"
        + "    \"test\": {\n"
        + "      \"foo\": \"bar\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"plan\": {\n"
        + "    \"track\": {\n"
        + "    }\n"
        + "  }\n"
        + "}"));

    analytics.track("foo");
    verify(integration).track(argThat(new NoDescriptionMatcher<TrackPayload>() {
      @Override protected boolean matchesSafely(TrackPayload payload) {
        return payload.event().equals("foo");
      }
    }));
    verifyNoMoreInteractions(integration);
  }

  @Test public void trackingPlanDisablesEvent() throws IOException {
    analytics.projectSettings = ProjectSettings.create(Cartographer.INSTANCE.fromJson("{\n"
        + "  \"integrations\": {\n"
        + "    \"test\": {\n"
        + "      \"foo\": \"bar\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"plan\": {\n"
        + "    \"track\": {\n"
        + "      \"foo\": {\n"
        + "        \"enabled\": false\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}"));

    analytics.track("foo");
    verifyNoMoreInteractions(integration);
  }

  @Test public void trackingPlanDisablesEventForSingleIntegration() throws IOException {
    analytics.projectSettings = ProjectSettings.create(Cartographer.INSTANCE.fromJson("{\n"
        + "  \"integrations\": {\n"
        + "    \"test\": {\n"
        + "      \"foo\": \"bar\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"plan\": {\n"
        + "    \"track\": {\n"
        + "      \"foo\": {\n"
        + "        \"enabled\": true,\n"
        + "        \"integrations\": {\n"
        + "          \"test\": false\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}"));

    analytics.track("foo");
    verifyNoMoreInteractions(integration);
  }

  @Test public void trackingPlanDisabledEventCannotBeOverriddenByOptions() throws IOException {
    analytics.projectSettings = ProjectSettings.create(Cartographer.INSTANCE.fromJson("{\n"
        + "  \"integrations\": {\n"
        + "    \"test\": {\n"
        + "      \"foo\": \"bar\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"plan\": {\n"
        + "    \"track\": {\n"
        + "      \"foo\": {\n"
        + "        \"enabled\": false\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}"));

    analytics.track("foo", null, new Options().setIntegration("test", true));
    verifyNoMoreInteractions(integration);
  }

  @Test public void trackingPlanDisabledEventForIntegrationOverriddenByOptions()
      throws IOException {
    analytics.projectSettings = ProjectSettings.create(Cartographer.INSTANCE.fromJson("{\n"
        + "  \"integrations\": {\n"
        + "    \"test\": {\n"
        + "      \"foo\": \"bar\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"plan\": {\n"
        + "    \"track\": {\n"
        + "      \"foo\": {\n"
        + "        \"enabled\": true,\n"
        + "        \"integrations\": {\n"
        + "          \"test\": false\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}"));

    analytics.track("foo", null, new Options().setIntegration("test", true));
    verify(integration).track(argThat(new NoDescriptionMatcher<TrackPayload>() {
      @Override protected boolean matchesSafely(TrackPayload payload) {
        return payload.event().equals("foo");
      }
    }));
    verifyNoMoreInteractions(integration);
  }

  @Test public void invalidAlias() {
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
    analytics.onIntegrationReady("test", callback);
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

  @Test public void trackApplicationLifecycleEventsInstalled() throws NameNotFoundException {
    Analytics.INSTANCES.clear();

    PackageInfo packageInfo = new PackageInfo();
    packageInfo.versionCode = 100;
    packageInfo.versionName = "1.0.0";

    PackageManager packageManager = mock(PackageManager.class);
    when(packageManager.getPackageInfo("com.foo", 0)).thenReturn(packageInfo);
    when(application.getPackageName()).thenReturn("com.foo");
    when(application.getPackageManager()).thenReturn(packageManager);

    final AtomicReference<Application.ActivityLifecycleCallbacks> callback =
        new AtomicReference<>();
    doNothing().when(application)
        .registerActivityLifecycleCallbacks(
            argThat(new NoDescriptionMatcher<Application.ActivityLifecycleCallbacks>() {
              @Override
              protected boolean matchesSafely(Application.ActivityLifecycleCallbacks item) {
                callback.set(item);
                return true;
              }
            }));

    analytics = new Analytics(application, networkExecutor, stats, traitsCache, analyticsContext,
        defaultOptions, Logger.with(NONE), "qaz", Collections.singletonList(factory), client,
        Cartographer.INSTANCE, projectSettingsCache, "foo", DEFAULT_FLUSH_QUEUE_SIZE,
        DEFAULT_FLUSH_INTERVAL, analyticsExecutor, true, new CountDownLatch(0), false, false,
        optOut, Crypto.none());

    callback.get().onActivityCreated(null, null);

    verify(integration).track(argThat(new NoDescriptionMatcher<TrackPayload>() {
      @Override protected boolean matchesSafely(TrackPayload payload) {
        return payload.event().equals("Application Installed") && //
            payload.properties().getString("version").equals("1.0.0") && //
            payload.properties().getInt("build", -1) == 100;
      }
    }));
    verify(integration).track(argThat(new NoDescriptionMatcher<TrackPayload>() {
      @Override protected boolean matchesSafely(TrackPayload payload) {
        return payload.event().equals("Application Opened") && //
            payload.properties().getString("version").equals("1.0.0") && //
            payload.properties().getInt("build", -1) == 100;
      }
    }));

    callback.get().onActivityCreated(null, null);
    verify(integration, times(2)).onActivityCreated(null, null);
    verifyNoMoreInteractions(integration);
  }

  @Test public void trackApplicationLifecycleEventsUpdated() throws NameNotFoundException {
    Analytics.INSTANCES.clear();

    PackageInfo packageInfo = new PackageInfo();
    packageInfo.versionCode = 101;
    packageInfo.versionName = "1.0.1";

    SharedPreferences sharedPreferences =
        RuntimeEnvironment.application.getSharedPreferences("analytics-android-qaz", MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putInt("build", 100);
    editor.putString("version", "1.0.0");
    editor.apply();

    PackageManager packageManager = mock(PackageManager.class);
    when(packageManager.getPackageInfo("com.foo", 0)).thenReturn(packageInfo);
    when(application.getPackageName()).thenReturn("com.foo");
    when(application.getPackageManager()).thenReturn(packageManager);

    final AtomicReference<Application.ActivityLifecycleCallbacks> callback =
        new AtomicReference<>();
    doNothing().when(application)
        .registerActivityLifecycleCallbacks(
            argThat(new NoDescriptionMatcher<Application.ActivityLifecycleCallbacks>() {
              @Override
              protected boolean matchesSafely(Application.ActivityLifecycleCallbacks item) {
                callback.set(item);
                return true;
              }
            }));

    analytics = new Analytics(application, networkExecutor, stats, traitsCache, analyticsContext,
        defaultOptions, Logger.with(NONE), "qaz", Collections.singletonList(factory), client,
        Cartographer.INSTANCE, projectSettingsCache, "foo", DEFAULT_FLUSH_QUEUE_SIZE,
        DEFAULT_FLUSH_INTERVAL, analyticsExecutor, true, new CountDownLatch(0), false, false,
        optOut, Crypto.none());

    callback.get().onActivityCreated(null, null);

    verify(integration).track(argThat(new NoDescriptionMatcher<TrackPayload>() {
      @Override protected boolean matchesSafely(TrackPayload payload) {
        return payload.event().equals("Application Updated") && //
            payload.properties().getString("previous_version").equals("1.0.0") && //
            payload.properties().getInt("previous_build", -1) == 100 && //
            payload.properties().getString("version").equals("1.0.1") && //
            payload.properties().getInt("build", -1) == 101;
      }
    }));
    verify(integration).track(argThat(new NoDescriptionMatcher<TrackPayload>() {
      @Override protected boolean matchesSafely(TrackPayload payload) {
        return payload.event().equals("Application Opened") && //
            payload.properties().getString("version").equals("1.0.1") && //
            payload.properties().getInt("build", -1) == 101;
      }
    }));
  }

  @Test public void recordScreenViews() throws NameNotFoundException {
    Analytics.INSTANCES.clear();

    final AtomicReference<Application.ActivityLifecycleCallbacks> callback =
        new AtomicReference<>();
    doNothing().when(application)
        .registerActivityLifecycleCallbacks(
            argThat(new NoDescriptionMatcher<Application.ActivityLifecycleCallbacks>() {
              @Override
              protected boolean matchesSafely(Application.ActivityLifecycleCallbacks item) {
                callback.set(item);
                return true;
              }
            }));

    analytics = new Analytics(application, networkExecutor, stats, traitsCache, analyticsContext,
        defaultOptions, Logger.with(NONE), "qaz", Collections.singletonList(factory), client,
        Cartographer.INSTANCE, projectSettingsCache, "foo", DEFAULT_FLUSH_QUEUE_SIZE,
        DEFAULT_FLUSH_INTERVAL, analyticsExecutor, false, new CountDownLatch(0), true, false,
        optOut, Crypto.none());

    Activity activity = mock(Activity.class);
    PackageManager packageManager = mock(PackageManager.class);
    ActivityInfo info = mock(ActivityInfo.class);

    when(activity.getPackageManager()).thenReturn(packageManager);
    //noinspection WrongConstant
    when(packageManager.getActivityInfo(any(ComponentName.class), eq(PackageManager.GET_META_DATA)))
        .thenReturn(info);
    when(info.loadLabel(packageManager)).thenReturn("Foo");

    callback.get().onActivityStarted(activity);

    verify(integration).screen(argThat(new NoDescriptionMatcher<ScreenPayload>() {
      @Override protected boolean matchesSafely(ScreenPayload payload) {
        return payload.name().equals("Foo");
      }
    }));
  }

  @Test public void registerActivityLifecycleCallbacks() throws NameNotFoundException {
    Analytics.INSTANCES.clear();

    final AtomicReference<Application.ActivityLifecycleCallbacks> callback =
        new AtomicReference<>();
    doNothing().when(application)
        .registerActivityLifecycleCallbacks(
            argThat(new NoDescriptionMatcher<Application.ActivityLifecycleCallbacks>() {
              @Override
              protected boolean matchesSafely(Application.ActivityLifecycleCallbacks item) {
                callback.set(item);
                return true;
              }
            }));

    analytics = new Analytics(application, networkExecutor, stats, traitsCache, analyticsContext,
        defaultOptions, Logger.with(NONE), "qaz", Collections.singletonList(factory), client,
        Cartographer.INSTANCE, projectSettingsCache, "foo", DEFAULT_FLUSH_QUEUE_SIZE,
        DEFAULT_FLUSH_INTERVAL, analyticsExecutor, false, new CountDownLatch(0), false, false,
        optOut, Crypto.none());

    Activity activity = mock(Activity.class);
    Bundle bundle = new Bundle();

    callback.get().onActivityCreated(activity, bundle);
    verify(integration).onActivityCreated(activity, bundle);

    callback.get().onActivityStarted(activity);
    verify(integration).onActivityStarted(activity);

    callback.get().onActivityResumed(activity);
    verify(integration).onActivityResumed(activity);

    callback.get().onActivityPaused(activity);
    verify(integration).onActivityPaused(activity);

    callback.get().onActivityStopped(activity);
    verify(integration).onActivityStopped(activity);

    callback.get().onActivitySaveInstanceState(activity, bundle);
    verify(integration).onActivitySaveInstanceState(activity, bundle);

    callback.get().onActivityDestroyed(activity);
    verify(integration).onActivityDestroyed(activity);

    verifyNoMoreInteractions(integration);
  }
}
