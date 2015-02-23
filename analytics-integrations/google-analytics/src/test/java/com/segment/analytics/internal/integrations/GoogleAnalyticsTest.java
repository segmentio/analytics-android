package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.regex.Pattern;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(GoogleAnalytics.class)
// todo: These tests do not run in the IDE http://pastebin.com/YZZTcZa8
public class GoogleAnalyticsTest {
  GoogleAnalyticsIntegration integration;
  @Mock GoogleAnalytics googleAnalytics;
  @Mock Tracker tracker;
  @Mock Application context;

  @Before public void setUp() {
    initMocks(this);
    mockStatic(GoogleAnalytics.class);
    integration = new GoogleAnalyticsIntegration();
    integration.googleAnalyticsInstance = googleAnalytics;
    integration.tracker = tracker;
  }

  @Test public void initialize() throws IllegalStateException {
    // TODO
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreGoogleInteractions();
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verify(googleAnalytics).reportActivityStart(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verify(googleAnalytics).reportActivityStop(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreGoogleInteractions();
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test public void identify() {
    integration.sendUserId = false;
    Traits traits = createTraits("foo").putAge(20);

    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(tracker).set("age", "20");
    verify(tracker).set("userId", "foo");
    verifyNoMoreGoogleInteractions();
  }

  @Test public void identifyWithUserId() {
    Traits traits = createTraits("foo").putAge(20);

    integration.sendUserId = true;
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(tracker).set("&uid", "foo");
    verify(tracker).set("age", "20");
    verify(tracker).set("userId", "foo");
    verifyNoMoreGoogleInteractions();
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreGoogleInteractions();
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("foo")
        .setLabel(null)
        .setValue(0)
        .build());
    verifyNoMoreGoogleInteractions();

    Properties properties =
        new Properties().putValue(51).putValue("label", "bar").putCategory("baz");
    integration.track(new TrackPayloadBuilder().properties(properties).event("foo").build());
    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("baz")
        .setAction("foo")
        .setLabel("bar")
        .setValue(51)
        .build());
    verifyNoMoreGoogleInteractions();
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreGoogleInteractions();
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(anyMapOf(String.class, String.class));
    inOrder.verify(tracker).setScreenName(null);
    verifyNoMoreGoogleInteractions();
  }

  @Test public void flush() {
    integration.flush();
    verify(googleAnalytics).dispatchLocalHits();
    verifyNoMoreGoogleInteractions();
  }

  @Test public void completedOrderEventsAreDetectedCorrectly() {
    assertThat(GoogleAnalyticsIntegration.COMPLETED_ORDER_PATTERN) //
        .matches("Completed Order")
        .matches("completed Order")
        .matches("Completed order")
        .matches("completed order")
        .matches("completed           order")
        .doesNotMatch("completed")
        .doesNotMatch("order")
        .doesNotMatch("completed orde")
        .doesNotMatch("")
        .doesNotMatch("ompleted order");
  }

  @Test public void productEventsAreAreDetectedCorrectly() {
    assertThat(GoogleAnalyticsIntegration.PRODUCT_EVENT_NAME_PATTERN) //
        .matches("Viewed Product Category")
        .matches("VIEweD prODUct")
        .matches("adDed Product")
        .matches("Removed Product")
        .matches("Viewed      Product")
        .doesNotMatch("removed")
        .doesNotMatch("Viewed")
        .doesNotMatch("Viewed")
        .doesNotMatch("adDed");
  }

  PatternAssert assertThat(Pattern pattern) {
    return new PatternAssert(pattern);
  }

  private void verifyNoMoreGoogleInteractions() {
    verifyNoMoreInteractions(GoogleAnalytics.class);
    verifyNoMoreInteractions(googleAnalytics);
    verifyNoMoreInteractions(tracker);
  }

  static class PatternAssert extends AbstractAssert<PatternAssert, Pattern> {
    public PatternAssert(Pattern actual) {
      super(actual, PatternAssert.class);
    }

    public PatternAssert matches(String text) {
      isNotNull();
      Assertions.assertThat(actual.matcher(text).matches())
          .overridingErrorMessage("Expected <%s> to match pattern <%s> but did not.", text,
              actual.pattern())
          .isTrue();
      return this;
    }

    public PatternAssert doesNotMatch(String text) {
      isNotNull();
      Assertions.assertThat(actual.matcher(text).matches())
          .overridingErrorMessage("Expected <%s> to not match patter <%s> but did.", text,
              actual.pattern())
          .isFalse();
      return this;
    }
  }
}
