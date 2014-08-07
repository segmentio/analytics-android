package com.segment.android.integrations;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Props;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;

import static org.fest.assertions.api.Assertions.assertThat;

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
    Tracker mockTracker = Mockito.mock(Tracker.class);
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

    Map<String, String> params = new HitBuilders.ItemBuilder() //
        .setTransactionId(props.getString("id"))
        .setName(props.getString("name"))
        .setSku(props.getString("sku"))
        .setCategory(props.getString("category"))
        .setPrice(props.getDouble("price", 0d))
        .setQuantity(props.getDouble("quantity", 1d).longValue())
        .setCurrencyCode(props.getString("currency"))
        .build();
    Mockito.verify(mockTracker).send(params);
    Mockito.verifyNoMoreInteractions(mockTracker);
  }

  @Test
  public void testSendsTransactionEventsCorrectly() throws Exception {
    Tracker mockTracker = Mockito.mock(Tracker.class);
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

    Map<String, String> params = new HitBuilders.TransactionBuilder() //
        .setTransactionId(props.getString("id"))
        .setAffiliation(props.getString("affiliation"))
        .setRevenue(props.getDouble("revenue"))
        .setTax(props.getDouble("tax"))
        .setShipping(props.getDouble("shipping"))
        .setCurrencyCode(props.getString("currency"))
        .build();
    Mockito.verify(mockTracker).send(params);
    Mockito.verifyNoMoreInteractions(mockTracker);
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
