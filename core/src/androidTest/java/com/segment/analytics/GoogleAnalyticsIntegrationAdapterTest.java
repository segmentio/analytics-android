package com.segment.analytics;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.GoogleAnalyticsIntegrationAdapter.COMPLETED_ORDER_PATTERN;
import static com.segment.analytics.GoogleAnalyticsIntegrationAdapter.PRODUCT_EVENT_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class GoogleAnalyticsIntegrationAdapterTest {

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
}
