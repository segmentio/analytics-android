package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.GoogleAnalyticsIntegration.COMPLETED_ORDER_PATTERN;
import static com.segment.analytics.GoogleAnalyticsIntegration.PRODUCT_EVENT_PATTERN;
import static com.segment.analytics.TestUtils.AliasPayloadBuilder;
import static com.segment.analytics.TestUtils.GroupPayloadBuilder;
import static com.segment.analytics.TestUtils.IdentifyPayloadBuilder;
import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(GoogleAnalytics.class)
public class GoogleAnalyticsRobolectricTest extends AbstractIntegrationTest {
  GoogleAnalyticsIntegration integration;
  @Mock GoogleAnalytics googleAnalytics;
  @Mock Tracker tracker;

  @Before @Override public void setUp() {
    super.setUp();
    mockStatic(GoogleAnalytics.class);
    integration = new GoogleAnalyticsIntegration();
    integration.googleAnalyticsInstance = googleAnalytics;
    integration.tracker = tracker;
  }

  @Test @Override public void initialize() throws IllegalStateException {
    // TODO
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verify(googleAnalytics).reportActivityStart(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verify(googleAnalytics).reportActivityStop(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void identify() {
    integration.sendUserId = false;
    Traits traits = new Traits().putAge(20).putUserId("foo");

    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(tracker).set("age", "20");
    verify(tracker).set("userId", "foo");
    verifyNoMoreGoogleInteractions();
  }

  @Test public void identifyWithUserId() {
    Traits traits = new Traits().putAge(20).putUserId("foo");

    integration.sendUserId = true;
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(tracker).set("&uid", "foo");
    verify(tracker).set("age", "20");
    verify(tracker).set("userId", "foo");
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(tracker).send(new HitBuilders.EventBuilder().setCategory(null)
        .setAction("foo")
        .setLabel(null)
        .setValue(0)
        .build());
    verifyNoMoreGoogleInteractions();

    Properties properties = new Properties().putValue(51).putValue("label", "bar");
    integration.track(new TrackPayloadBuilder().properties(properties).event("foo").build());
    verify(tracker).send(new HitBuilders.EventBuilder().setCategory(null)
        .setAction("foo")
        .setLabel("bar")
        .setValue(51)
        .build());
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(anyMap());
    inOrder.verify(tracker).setScreenName(null);
    verifyNoMoreGoogleInteractions();
  }

  @Test @Override public void flush() {
    integration.flush();
    verify(googleAnalytics).dispatchLocalHits();
    verifyNoMoreGoogleInteractions();
  }

  @Test public void completedOrderEventsAreDetectedCorrectly() {
    assertThat(COMPLETED_ORDER_PATTERN.matcher("Completed Order").matches()).isTrue();
    assertThat(COMPLETED_ORDER_PATTERN.matcher("completed Order").matches()).isTrue();
    assertThat(COMPLETED_ORDER_PATTERN.matcher("Completed order").matches()).isTrue();
    assertThat(COMPLETED_ORDER_PATTERN.matcher("completed order").matches()).isTrue();
    assertThat(COMPLETED_ORDER_PATTERN.matcher("completed           order").matches()).isTrue();

    assertThat(COMPLETED_ORDER_PATTERN.matcher("completed").matches()).isFalse();
    assertThat(COMPLETED_ORDER_PATTERN.matcher("order").matches()).isFalse();
    assertThat(COMPLETED_ORDER_PATTERN.matcher("completed orde").matches()).isFalse();
    assertThat(COMPLETED_ORDER_PATTERN.matcher("").matches()).isFalse();
    assertThat(COMPLETED_ORDER_PATTERN.matcher("ompleted order").matches()).isFalse();
  }

  @Test public void productEventsAreAreDetectedCorrectly() {
    assertThat(PRODUCT_EVENT_PATTERN.matcher("Viewed Product Category").matches()).isTrue();
    assertThat(PRODUCT_EVENT_PATTERN.matcher("VIEweD prODUct").matches()).isTrue();
    assertThat(PRODUCT_EVENT_PATTERN.matcher("adDed Product").matches()).isTrue();
    assertThat(PRODUCT_EVENT_PATTERN.matcher("Removed Product").matches()).isTrue();
    assertThat(PRODUCT_EVENT_PATTERN.matcher("Viewed      Product").matches()).isTrue();

    assertThat(PRODUCT_EVENT_PATTERN.matcher("removed").matches()).isFalse();
    assertThat(PRODUCT_EVENT_PATTERN.matcher("Viewed").matches()).isFalse();
    assertThat(PRODUCT_EVENT_PATTERN.matcher("Product").matches()).isFalse();
    assertThat(PRODUCT_EVENT_PATTERN.matcher("adDed").matches()).isFalse();
  }

  private void verifyNoMoreGoogleInteractions() {
    verifyNoMoreInteractions(GoogleAnalytics.class);
    verifyNoMoreInteractions(googleAnalytics);
    verifyNoMoreInteractions(tracker);
  }
}
