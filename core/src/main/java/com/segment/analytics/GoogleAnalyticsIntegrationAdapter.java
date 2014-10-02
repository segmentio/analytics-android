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
import java.util.regex.Pattern;

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
class GoogleAnalyticsIntegrationAdapter extends AbstractIntegrationAdapter<Tracker> {
  static final Pattern COMPLETED_ORDER_PATTERN =
      Pattern.compile("completed *order", Pattern.CASE_INSENSITIVE);
  static final Pattern PRODUCT_EVENT_PATTERN =
      Pattern.compile("((viewed)|(added)|(removed)) *product *.*", Pattern.CASE_INSENSITIVE);
  Tracker tracker;
  GoogleAnalytics googleAnalyticsInstance;
  boolean optedOut;
  boolean sendUserId;

  static Map<String, String> transactionToMap(Properties props) {
    String id = props.getString("userId");
    // skip total
    double revenue = props.getDouble("revenue", 0);
    double tax = props.getDouble("tax", 0);
    double shipping = props.getDouble("shipping", 0);

    // Specific for GA
    String affiliation = props.getString("affiliation");
    String currency = props.getString("currency");

    return new HitBuilders.TransactionBuilder() //
        .setTransactionId(id)
        .setAffiliation(affiliation)
        .setRevenue(revenue)
        .setTax(tax)
        .setShipping(shipping)
        .setCurrencyCode(currency)
        .build();
  }

  static Map<String, String> productToMap(String categoryName, Map<String, Object> rawProduct) {
    JsonMap product = new JsonMap(rawProduct);
    String id = product.getString("id");
    String sku = product.getString("sku");
    String name = product.getString("name");
    double price = product.getDouble("price", 0);
    long quantity = product.getLong("quantity", 1);
    String category = product.getString("category");
    if (isNullOrEmpty(category)) category = categoryName;
    // Specific for GA
    String currency = product.getString("currency");

    return new HitBuilders.ItemBuilder() //
        .setTransactionId(id)
        .setName(name)
        .setSku(sku)
        .setCategory(category)
        .setPrice(price)
        .setQuantity(quantity)
        .setCurrencyCode(currency)
        .build();
  }

  @Override void initialize(Context context, JsonMap settings)
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
    tracker.setAnonymizeIp(settings.getBoolean("anonymizeIp", false));
    if (settings.getBoolean("reportUncaughtExceptions", false)) {
      enableAutomaticExceptionTracking(context);
    }
    tracker.setSampleRate(settings.getDouble("siteSpeedSampleRate", 1));
    sendUserId = settings.getBoolean("sendUserId", false);
  }

  private void enableAutomaticExceptionTracking(Context context) {
    Thread.UncaughtExceptionHandler myHandler =
        new ExceptionReporter(tracker, Thread.getDefaultUncaughtExceptionHandler(), context);
    Thread.setDefaultUncaughtExceptionHandler(myHandler);
  }

  @Override void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    applyOptOut(activity);
  }

  @Override void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    applyOptOut(activity);
  }

  @Override void optOut(boolean optOut) {
    super.optOut(optOut);
    optedOut = optOut;
  }

  private void applyOptOut(Activity activity) {
    GoogleAnalytics.getInstance(activity).setAppOptOut(optedOut);
  }

  @Override void screen(ScreenPayload screen) {
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

  @Override void track(TrackPayload track) {
    Properties properties = track.properties();
    String event = track.event();
    if (checkAndPerformEcommerceEvent(event, null, properties)) {
      return;
    }
    String category = properties.getString("category");
    String label = properties.getString("label");
    int value = properties.getInt("value", 0);
    tracker.send(new HitBuilders.EventBuilder().setCategory(category)
        .setAction(event)
        .setLabel(label)
        .setValue(value)
        .build());
  }

  /** Check if event is an ecommerce event, if it is, do it and return true, or return false. */
  boolean checkAndPerformEcommerceEvent(String event, String category, Properties props) {
    if (PRODUCT_EVENT_PATTERN.matcher(event).matches()) {
      sendItem(category, props);
      return true;
    } else if (COMPLETED_ORDER_PATTERN.matcher(event).matches()) {
      // this is only sent via .track so won't have category
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

  @Override Tracker getUnderlyingInstance() {
    return tracker;
  }

  @Override String key() {
    return "Google Analytics";
  }
}
