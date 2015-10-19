package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.Log;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.regex.Pattern;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(GoogleAnalytics.class)
// todo: These tests do not run in the IDE http://pastebin.com/YZZTcZa8
public class GoogleAnalyticsTest {

  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
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
    integration.customDimensions = new ValueMap();
    integration.customMetrics = new ValueMap();
    integration.sendUserId = false;
    integration.log = Log.with(Analytics.LogLevel.DEBUG);
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
    Traits traits = createTraits("foo").putAge(20);
    IdentifyPayload payload = new IdentifyPayloadBuilder().traits(traits).build();
    integration.identify(payload);

    // If there are no custom dimensions/metrics and `sendUserId` is false,
    // nothing should happen.
    verifyNoMoreGoogleInteractions();
  }

  @Test public void identifyWithUserIdAndWithoutCustomDimensionsAndMetrics() {
    integration.sendUserId = true;

    Traits traits = createTraits("foo").putAge(20);
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

    // If there are no custom dimensions/metrics and `sendUserId` is true,
    // only the userId should be set.
    verify(tracker).set("&uid", "foo");
    verifyNoMoreGoogleInteractions();
  }

  @Test public void identifyWithUserIdAndCustomDimensionsAndMetrics() {
    integration.sendUserId = true;
    integration.customDimensions = new ValueMap().putValue("name", "dimension10");
    integration.customMetrics = new ValueMap().putValue("level", "metric12");

    Traits traits = createTraits("foo").putAge(20).putName("Chris").putValue("level", 13);
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

    // Verify user id is set.
    verify(tracker).set("&uid", "foo");

    // Verify dimensions and metrics are set.
    verify(tracker).set("&cd10", "Chris");
    verify(tracker).set("&cm12", "13");

    // Verify other traits are ignored.
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
  }

  @Test public void trackWithProperties() {
    Properties properties = new Properties() //
        .putValue(51).putValue("label", "bar").putCategory("baz");

    integration.track(new TrackPayloadBuilder().properties(properties).event("foo").build());

    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("baz")
        .setAction("foo")
        .setLabel("bar")
        .setValue(51)
        .build());
    verifyNoMoreGoogleInteractions();
  }

  @Test public void trackWithCustomDimensions() {
    integration.customDimensions = new ValueMap().putValue("custom", "dimension3");

    integration.track(new TrackPayloadBuilder().event("foo")
        .properties(new Properties().putValue("custom", "test"))
        .build());

    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("foo")
        .setLabel(null)
        .setValue(0)
        .setCustomDimension(3, "test")
        .build());
    verifyNoMoreGoogleInteractions();
  }

  @Test public void trackWithCustomMetrics() {
    integration.customMetrics = new ValueMap().putValue("score", "metric5");

    integration.track(new TrackPayloadBuilder().event("foo")
        .properties(new Properties().putValue("score", 50))
        .build());

    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("foo")
        .setLabel(null)
        .setValue(0)
        .setCustomMetric(5, 50)
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
    verifyNoMoreGoogleInteractions();
  }

  @Test public void screenWithCustomDimensions() {
    integration.customDimensions = new ValueMap().putValue("custom", "dimension10");

    integration.screen(new ScreenPayloadBuilder().name("foo")
        .properties(new Properties().putValue("custom", "value"))
        .build());

    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(new HitBuilders.AppViewBuilder() //
        .setCustomDimension(10, "value").build());
    verifyNoMoreGoogleInteractions();
  }

  @Test public void screenWithCustomMetrics() {
    integration.customMetrics = new ValueMap().putValue("count", "metric14");

    integration.screen(new ScreenPayloadBuilder().name("foo")
        .properties(new Properties().putValue("count", 100))
        .build());

    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(new HitBuilders.AppViewBuilder().setCustomMetric(14, 100).build());
    verifyNoMoreGoogleInteractions();
  }

  @Test public void flush() {
    integration.flush();

    verify(googleAnalytics).dispatchLocalHits();
    verifyNoMoreGoogleInteractions();
  }

  @Test public void reset() {
    integration.reset();
    verifyNoMoreGoogleInteractions();
  }

  @Test public void sendProductEvent() {
    Properties properties = new Properties().putProductId("foo")
        .putCurrency("bar")
        .putName("baz")
        .putSku("qaz")
        .putPrice(20)
        .putValue("quantity", 10);

    integration.sendProductEvent("Viewed Product", "sports", properties);

    verify(tracker).send(new HitBuilders.ItemBuilder() //
        .setTransactionId("foo")
        .setCurrencyCode("bar")
        .setName("baz")
        .setSku("qaz")
        .setCategory("sports")
        .setPrice(20)
        .setQuantity(10)
        .build());
  }

  @Test public void sendProductEventWithCustomDimensionsAndMetrics() {
    integration.customDimensions = new ValueMap().putValue("customDimension", "dimension2");
    integration.customMetrics = new ValueMap().putValue("customMetric", "metric3");

    Properties properties = new Properties().putProductId("foo")
        .putCurrency("bar")
        .putName("baz")
        .putSku("qaz")
        .putPrice(20)
        .putValue("quantity", 10)
        .putValue("customMetric", "32.22")
        .putValue("customDimension", "barbaz");
    integration.sendProductEvent("Removed Product", "sports", properties);

    verify(tracker).send(new HitBuilders.ItemBuilder() //
        .setTransactionId("foo")
        .setCurrencyCode("bar")
        .setName("baz")
        .setSku("qaz")
        .setCategory("sports")
        .setPrice(20)
        .setQuantity(10)
        .setCustomDimension(2, "barbaz")
        .setCustomMetric(3, 32.22f)
        .build());
  }

  @Test public void sendProductEventNoMatch() {
    integration.sendProductEvent("Viewed", null, null);

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
