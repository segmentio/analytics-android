package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.segment.analytics.Utils.getDefaultValueIfNull;
import static com.segment.analytics.Utils.hasPermission;
import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * Google Analytics is the most popular analytics tool for the web because it’s free and sports a
 * wide range of features. It’s especially good at measuring traffic sources and ad campaigns.
 *
 * @see <a href="http://www.google.com/analytics/">Google Analytics</a>
 * @see <a href="https://segment.io/docs/integrations/google-analytics/">Google Analytics
 * Integration</a>
 * @see <a href="https://developers.google.com/analytics/devguides/collection/android/v4/">Google
 * Analyitcs Android SDK</a>
 */
public class GoogleAnalyticsIntegrationAdapter extends AbstractIntegrationAdapter<Tracker> {
  private static final String COMPLETED_ORDER_EVENT_NAME = "Completed Order";
  private static final Set<String> ITEM_EVENT_NAMES =
      Utils.asSet("Viewed Product", "Added Product", "Removed Product", "Favorited Product",
          "Liked Product", "Shared Product", "Wishlisted Product", "Reviewed Product",
          "Filtered Product", "Sorted Product", "Searched Product", "Viewed Product Image",
          "Viewed Product Reviews", "Viewed Sale Page");
  Tracker tracker;
  GoogleAnalytics googleAnalyticsInstance;
  boolean optedOut;
  boolean sendUserId;

  static Map<String, String> transactionToMap(Properties props) {
    String id = props.getString("userId");
    // skip total
    Double revenue = props.getDouble("revenue");
    Double tax = props.getDouble("tax");
    Double shipping = props.getDouble("shipping");

    // Specific for GA
    String affiliation = props.getString("affiliation");
    String currency = props.getString("currency");

    return new HitBuilders.TransactionBuilder() //
        .setTransactionId(id)
        .setAffiliation(affiliation)
        .setRevenue(getDefaultValueIfNull(revenue, 0d))
        .setTax(getDefaultValueIfNull(tax, 0d))
        .setShipping(getDefaultValueIfNull(shipping, 0d))
        .setCurrencyCode(currency)
        .build();
  }

  static Map<String, String> productToMap(String categoryName, Map<String, Object> rawProduct) {
    JsonMap product = new JsonMap(rawProduct);
    String id = product.getString("id");
    String sku = product.getString("sku");
    String name = product.getString("name");
    Double price = product.getDouble("price");
    Long quantity = product.getLong("quantity");
    String category = getDefaultValueIfNull(product.getString("category"), categoryName);
    // Specific for GA
    String currency = product.getString("currency");

    return new HitBuilders.ItemBuilder() //
        .setTransactionId(id)
        .setName(name)
        .setSku(sku)
        .setCategory(category)
        .setPrice(getDefaultValueIfNull(price, 0d))
        .setQuantity(getDefaultValueIfNull(quantity, 1L))
        .setCurrencyCode(currency)
        .build();
  }

  @Override public void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new InvalidConfigurationException(
          "Google Analytics requires the access state permission.");
    }

    googleAnalyticsInstance = GoogleAnalytics.getInstance(context);
    // todo: set logger level googleAnalyticsInstance.getLogger().setLogLevel();

    // Look up the mobileTrackingId, if unavailable, fallback to the trackingId
    String trackingId = settings.getString("mobileTrackingId");
    if (isNullOrEmpty(trackingId)) trackingId = settings.getString("trackingId");

    tracker = googleAnalyticsInstance.newTracker(trackingId);
    initTracker(context, tracker, settings);
  }

  void initTracker(Context context, Tracker tracker, JsonMap settings) {
    if (settings.containsKey("anonymizeIp")) {
      tracker.setAnonymizeIp(settings.getBoolean("anonymizeIp"));
    }
    if (getDefaultValueIfNull(settings.getBoolean("reportUncaughtExceptions"), false)) {
      enableAutomaticExceptionTracking(context);
    }
    tracker.setSampleRate(getDefaultValueIfNull(settings.getDouble("siteSpeedSampleRate"), 1.0d));
    sendUserId = getDefaultValueIfNull(settings.getBoolean("sendUserId"), false);
  }

  private void enableAutomaticExceptionTracking(Context context) {
    Thread.UncaughtExceptionHandler myHandler =
        new ExceptionReporter(tracker, Thread.getDefaultUncaughtExceptionHandler(), context);
    Thread.setDefaultUncaughtExceptionHandler(myHandler);
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    applyOptOut(activity);
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    applyOptOut(activity);
  }

  @Override public void optOut(boolean optOut) {
    super.optOut(optOut);
    optedOut = optOut;
  }

  private void applyOptOut(Activity activity) {
    GoogleAnalytics.getInstance(activity).setAppOptOut(optedOut);
  }

  @Override
  public void screen(ScreenPayload screen) {
    super.screen(screen);
    String screenName = screen.event();
    if (checkAndPerformEcommerceEvent(screenName, screen.category(), screen.properties())) {
      return;
    }
    tracker.setScreenName(screenName);
    tracker.send(new HitBuilders.AppViewBuilder().build());
    tracker.setScreenName(null);
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    if (sendUserId) {
      tracker.set("&uid", identify.userId());
    }
    for (Map.Entry<String, Object> entry : identify.traits().entrySet()) {
      tracker.set(entry.getKey(), String.valueOf(entry.getValue()));
    }
  }

  @Override
  public void track(TrackPayload track) {
    Properties properties = track.properties();
    String event = track.event();
    if (checkAndPerformEcommerceEvent(event, null, properties)) {
      return;
    }
    String category = properties.getString("category");
    String label = properties.getString("label");
    int value = getDefaultValueIfNull(properties.getInteger("value"), 0);
    tracker.send(new HitBuilders.EventBuilder().setCategory(category)
        .setAction(event)
        .setLabel(label)
        .setValue(value)
        .build());
  }

  /** Check if event is an ecommerce event, if it is, do it and return true, or return false. */
  boolean checkAndPerformEcommerceEvent(String event, String category, Properties props) {
    if (ITEM_EVENT_NAMES.contains(event)) {
      sendItem(category, props);
      return true;
    } else if (COMPLETED_ORDER_EVENT_NAME.equals(event)) {
      // COMPLETED_ORDER_EVENT_NAME is only sent via .track so won't have category
      sendTransaction(props);
      return true;
    }
    return false;
  }

  private void sendTransaction(Properties properties) {
    List<Object> products = (List<Object>) properties.get("products");
    if (!isNullOrEmpty(products)) {
      for (Object product : products) {
        try {
          tracker.send(productToMap(null, (Map<String, Object>) product));
        } catch (ClassCastException e) {
          // todo, log error
        }
      }
    }
    tracker.send(transactionToMap(properties));
  }

  private void sendItem(String categoryName, Properties props) {
    tracker.send(productToMap(categoryName, props));
  }

  @Override public Tracker getUnderlyingInstance() {
    return tracker;
  }

  @Override public String className() {
    return "com.google.android.gms.analytics.GoogleAnalytics";
  }

  @Override public String key() {
    return "Google Analytics";
  }
}
