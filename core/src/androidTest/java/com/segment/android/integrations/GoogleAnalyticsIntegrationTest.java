package com.segment.android.integrations;

import com.google.android.gms.analytics.Tracker;
import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Props;
import java.util.Map;
import org.json.JSONArray;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class GoogleAnalyticsIntegrationTest extends BaseIntegrationTest {

  @Override
  public Integration getIntegration() {
    return new GoogleAnalyticsIntegration();
  }

  @Override
  public EasyJSONObject getSettings() {
    EasyJSONObject settings = new EasyJSONObject();
    settings.put("mobileTrackingId", "UA-27033709-9");
    settings.put("anonymizeIp", false);
    settings.put("reportUncaughtExceptions", true);
    settings.put("mobileHttps", true);
    return settings;
  }

  @Test
  public void testSendsItemEventsCorrectly() throws Exception {
    Tracker mockTracker = mock(Tracker.class);
    ((GoogleAnalyticsIntegration) integration).tracker = mockTracker;

    Props props = new Props();
    props.put("id", "507f1f77bcf86cd799439011");
    props.put("sku", "45790-32");
    props.put("name", "Monopoly: 3rd Edition");
    props.put("price", 18.99d);
    props.put("quantity", 2);
    props.put("category", "games");
    props.put("currency", "USD");

    boolean wasEcommerceEvent =
        ((GoogleAnalyticsIntegration) integration).checkAndPerformEcommerceEvent("Added Product",
            "Games", props);
    assertThat(wasEcommerceEvent).isTrue();

    Map<String, String> params = GoogleAnalyticsIntegration.productToMap("Games", props);
    verify(mockTracker).send(params);
    verifyNoMoreInteractions(mockTracker);
  }

  @Test
  public void testSendsTransactionEventsCorrectly() throws Exception {
    Tracker mockTracker = mock(Tracker.class);
    ((GoogleAnalyticsIntegration) integration).tracker = mockTracker;

    Props props = new Props();
    props.put("id", "507f1f77bcf86cd799439011");
    props.put("revenue", 23.41);
    props.put("shipping", 29.99);
    props.put("tax", 18.99d);
    props.put("affiliation", "A fake store name");
    props.put("currency", "USD");

    boolean wasEcommerceEvent =
        ((GoogleAnalyticsIntegration) integration).checkAndPerformEcommerceEvent("Completed Order",
            "Games", props);
    assertThat(wasEcommerceEvent).isTrue();

    Map<String, String> params = GoogleAnalyticsIntegration.transactionToMap(props);
    verify(mockTracker).send(params);
    verifyNoMoreInteractions(mockTracker);
  }

  @Test
  public void testSendsTransactionEventsWithProducts() throws Exception {
    Tracker mockTracker = mock(Tracker.class);
    ((GoogleAnalyticsIntegration) integration).tracker = mockTracker;

    Props product1 = new Props();
    Props product2 = new Props();
    Props product3 = new Props();
    Props product4 = new Props();
    Props product5 = new Props();

    Props props = new Props();
    props.put("id", "507f1f77bcf86cd799439011");
    props.put("revenue", 23.41);
    props.put("shipping", 29.99);
    props.put("tax", 18.99d);
    props.put("affiliation", "A fake store name");
    props.put("currency", "USD");
    props.put("products",
        new JSONArray().put(product1).put(product2).put(product3).put(product4).put(product5));

    boolean wasEcommerceEvent =
        ((GoogleAnalyticsIntegration) integration).checkAndPerformEcommerceEvent("Completed Order",
            null, props);
    assertThat(wasEcommerceEvent).isTrue();

    verify(mockTracker, times(6)).send(anyMap());
  }

  @Test
  public void testDoesNotSendWrongEvents() throws Exception {
    GoogleAnalyticsIntegration googleAnalyticsIntegration =
        (GoogleAnalyticsIntegration) integration;

    assertThat(googleAnalyticsIntegration.checkAndPerformEcommerceEvent("Completed", null,
        null)).isFalse();

    assertThat(
        googleAnalyticsIntegration.checkAndPerformEcommerceEvent("Order", null, null)).isFalse();

    assertThat(
        googleAnalyticsIntegration.checkAndPerformEcommerceEvent("Product", null, null)).isFalse();

    assertThat(
        googleAnalyticsIntegration.checkAndPerformEcommerceEvent(null, null, null)).isFalse();
  }
}
