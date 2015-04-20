package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.TestUtils.createTraits;
import static com.segment.analytics.TestUtils.jsonEq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(Crittercism.class)
public class CrittercismTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @MockitoAnnotations.Mock Application context;
  CrittercismIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Crittercism.class);
    integration = new CrittercismIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    integration.initialize(context, new ValueMap().putValue("appId", "foo"), NONE);

    CrittercismConfig expectedConfig = new CrittercismConfig();
    verifyStatic();
    Crittercism.initialize(eq(context), eq("foo"), configEq(expectedConfig));
  }

  @Test public void initializeWithArgs() throws IllegalStateException {
    integration.initialize(context, new ValueMap().putValue("appId", "bar")
        .putValue("shouldCollectLogcat", true)
        .putValue("includeVersionCode", true)
        .putValue("customVersionName", "qaz")
        .putValue("enableServiceMonitoring", false), NONE);

    CrittercismConfig expectedConfig = new CrittercismConfig();
    expectedConfig.setLogcatReportingEnabled(true);
    expectedConfig.setVersionCodeToBeIncludedInVersionString(true);
    expectedConfig.setCustomVersionName("qaz");
    expectedConfig.setServiceMonitoringEnabled(false);
    verifyStatic();
    Crittercism.initialize(eq(context), eq("bar"), configEq(expectedConfig));
  }

  public static CrittercismConfig configEq(CrittercismConfig crittercismConfig) {
    return argThat(new CrittercismConfigMatcher(crittercismConfig));
  }

  public static class CrittercismConfigMatcher extends TypeSafeMatcher<CrittercismConfig> {

    private final CrittercismConfig expected;

    CrittercismConfigMatcher(CrittercismConfig expected) {
      this.expected = expected;
    }

    @Override protected boolean matchesSafely(CrittercismConfig item) {
      try {
        assertThat(expected).isEqualToComparingFieldByField(item);
        return true;
      } catch (AssertionError e) {
        return false;
      }
    }

    @Override public void describeTo(Description description) {
      print(expected, description);
    }

    static void print(CrittercismConfig config, Description description) {
      description.appendText("reportLogcat: " + config.isLogcatReportingEnabled());
      boolean includeVersionCode = config.isVersionCodeToBeIncludedInVersionString();
      description.appendText(", includeVersionCode: " + includeVersionCode);
      description.appendText(", customVersionName: " + config.getCustomVersionName());
      description.appendText(", enableServiceMonitoring: " + config.isServiceMonitoringEnabled());
    }
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void identify() {
    Traits traits = createTraits("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    Crittercism.setUsername("foo");
    verifyStatic();
    Crittercism.setMetadata(jsonEq(traits.toJsonObject()));
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("foo").category("bar").build());
    verifyStatic();
    Crittercism.leaveBreadcrumb("Viewed foo Screen");
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verifyStatic();
    Crittercism.leaveBreadcrumb("foo");
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(Crittercism.class);
  }

  @Test public void flush() {
    integration.flush();
    verifyStatic();
    Crittercism.sendAppLoadData();
  }

  @Test public void reset() {
    integration.reset();
    verifyNoMoreInteractions(Crittercism.class);
  }
}
