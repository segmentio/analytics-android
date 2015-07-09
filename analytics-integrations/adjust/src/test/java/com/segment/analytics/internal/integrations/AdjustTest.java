package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Properties;
import com.segment.analytics.Randoms;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONObject;
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

import java.lang.AssertionError;
import java.lang.Override;

import static com.segment.analytics.TestUtils.createTraits;
import static com.segment.analytics.TestUtils.jsonEq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static com.segment.analytics.internal.integrations.AdjustIntegration.ADJUST_TOKEN;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(Adjust.class)
public class AdjustTest {

  @Rule public PowerMockRule powerMockRule = new PowerMockRule();
  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
  @Mock Application context;
  @Mock Analytics analytics;
  AdjustIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Adjust.class);
    integration = new AdjustIntegration();
  }

  @Test public void initialize() {
    when(analytics.getApplication()).thenReturn(context);

    integration.initialize(analytics, new ValueMap().putValue(ADJUST_TOKEN, "foo"));

    verifyStatic();
    Adjust.onCreate(any(AdjustConfig.class));
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);

    integration.onActivityCreated(activity, bundle);

    verifyNoMoreInteractions(Adjust.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);

    integration.onActivityStarted(activity);

    verifyNoMoreInteractions(Adjust.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);

    integration.onActivityResumed(activity);
    verifyStatic();
    Adjust.onResume();
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);

    integration.onActivityPaused(activity);
    verifyStatic();
    Adjust.onPause();
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);

    integration.onActivityStopped(activity);

    verifyNoMoreInteractions(Adjust.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);

    integration.onActivitySaveInstanceState(activity, bundle);

    verifyNoMoreInteractions(Adjust.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);

    integration.onActivityDestroyed(activity);

    verifyNoMoreInteractions(Adjust.class);
  }

  @Test public void track() {
    Properties properties = new Properties();

    integration.track(new TrackPayloadBuilder().event("foo").properties(properties).build());

    verifyStatic();
    Adjust.trackEvent(eventEq(new AdjustEvent("foo")));
    verifyNoMoreInteractions(Adjust.class);
  }

  @Test public void trackWithRevenue() {
    Properties properties = new Properties().putRevenue(20).putCurrency("USD");
    TrackPayload trackPayload =
            new TrackPayloadBuilder().event("foo").properties(properties).build();

    integration.track(trackPayload);

    verifyStatic();
    AdjustEvent event = new AdjustEvent("foo");
    event.setRevenue(20, "USD");
    Adjust.trackEvent(eventEq(event));
    verifyNoMoreInteractions(Adjust.class);
  }

  public void alias() {
    integration.alias(new AliasPayloadBuilder().build());

    verifyNoMoreInteractions(Adjust.class);
  }

  @Test public void identify() {
    Traits traits = createTraits("foo").putAge(20).putFirstName("bar");
    IdentifyPayload payload = new IdentifyPayloadBuilder().traits(traits).build();

    integration.identify(new IdentifyPayloadBuilder().build());
    verifyNoMoreInteractions(Adjust.class);
  }

  @Test
  public void group() {
    integration.group(new GroupPayloadBuilder().build());

    verifyNoMoreInteractions(Adjust.class);
  }

  @Test
  public void screen() {
    integration.screen(new ScreenPayloadBuilder().category("foo").build());

    verifyNoMoreInteractions(Adjust.class);
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(Adjust.class);
  }

  @Test public void reset() {
    integration.reset();

    verifyNoMoreInteractions(Adjust.class);
  }

  private static AdjustEvent eventEq(AdjustEvent proposed) {
    return argThat(new AdjustEventMatcher(proposed));
  }

  public static class AdjustEventMatcher extends TypeSafeMatcher<AdjustEvent> {

    private final AdjustEvent expected;

    AdjustEventMatcher(AdjustEvent expected) {
      this.expected = expected;
    }

    @Override protected boolean matchesSafely(AdjustEvent proposed) {
      try {
        assertThat(expected).isEqualToComparingFieldByField(proposed);
        return true;
      } catch (AssertionError ignored) {
        return false;
      }
    }

    @Override public void describeTo(Description description) {
      description.appendText("valid: " + expected.isValid());
    }
  }
}
