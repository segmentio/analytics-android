package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.apptimize.Apptimize;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Apptimize.class)
public class ApptimizeTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  ApptimizeIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Apptimize.class);
    integration = new com.segment.analytics.internal.integrations.ApptimizeIntegration();
  }

  @Test public void initialize() {
    integration.initialize(context, new ValueMap().putValue("appkey", "foo"), NONE);
    verifyStatic();
    Apptimize.setup(context, "foo");
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("pressPlay").build());
    verifyStatic();
    Apptimize.track(eq("pressPlay"));
  }

  @Test public void trackNumeric() {
    integration.track(new TrackPayloadBuilder().event("pressPlay")
        .properties(new Properties().putValue("value", 3.1))
        .build());
    verifyStatic();
    Apptimize.track(eq("pressPlay"), eq(3.1));
  }

  @Test public void trackNumericInteger() {
    integration.track(new TrackPayloadBuilder().event("pressPlay")
        .properties(new Properties().putValue("value", 3))
        .build());
    verifyStatic();
    Apptimize.track(eq("pressPlay"), eq(3.0));
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void identify() {
    integration.identify(new IdentifyPayloadBuilder().traits(createTraits("randoUserId")).build());
    verifyStatic();
    Apptimize.setUserAttribute("userId", "randoUserId");
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void identifyTraits() {
    Traits traits = createTraits("randoUserId").putAge(78).putCreatedAt("March 15, 1935");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    Apptimize.setUserAttribute("userId", "randoUserId");
    verifyStatic();
    Apptimize.setUserAttribute("age", 78);
    verifyStatic();
    Apptimize.setUserAttribute("createdAt", "March 15, 1935");
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("payments").build());
    verifyStatic();
    Apptimize.track(eq("payments"));
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(Apptimize.class);
  }

  @Test public void reset() {
    integration.reset();
    verifyNoMoreInteractions(Apptimize.class);
  }
}
