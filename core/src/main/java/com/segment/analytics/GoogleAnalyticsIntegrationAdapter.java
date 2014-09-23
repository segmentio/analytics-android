package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.segment.analytics.Utils.hasPermission;
import static com.segment.analytics.Utils.isNullOrEmpty;
import static com.segment.analytics.Utils.nullOrDefault;

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
  Tracker tracker;
  GoogleAnalytics googleAnalyticsInstance;
  boolean optedOut;

  private static final Set<String> itemEventNames;
  private static final String COMPLETED_ORDER_EVENT_NAME = "Completed Order";

  static {
    itemEventNames = new HashSet<String>();
    itemEventNames.add("Viewed Product");
    itemEventNames.add("Added Product");
    itemEventNames.add("Removed Product");
    itemEventNames.add("Favorited Product");
    itemEventNames.add("Liked Product");
    itemEventNames.add("Shared Product");
    itemEventNames.add("Wishlisted Product");
    itemEventNames.add("Reviewed Product");
    itemEventNames.add("Filtered Product");
    itemEventNames.add("Sorted Product");
    itemEventNames.add("Searched Product");
    itemEventNames.add("Viewed Product Image");
    itemEventNames.add("Viewed Product Reviews");
    itemEventNames.add("Viewed Sale Page");
  }

  @Override public void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new InvalidConfigurationException(
          "Google Analytics requires the access state permission.");
    }

    googleAnalyticsInstance = GoogleAnalytics.getInstance(context);
    googleAnalyticsInstance.getLogger().setLogLevel(Logger.isLogging() ? Log.VERBOSE : Log.ERROR);

    String trackingId = settings.getString("mobileTrackingId");
    if (isNullOrEmpty(trackingId)) trackingId = settings.getString("trackingId");
    tracker = googleAnalyticsInstance.newTracker(trackingId);
    tracker.setAnonymizeIp(settings.getBoolean("anonymizeIp"));
    tracker.setSampleRate(settings.getInteger("siteSpeedSampleRate"));
    if (settings.getBoolean("reportUncaughtExceptions")) enableAutomaticExceptionTracking(context);
    tracker.enableAutoActivityTracking(true);
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

  @Override
  public void track(TrackPayload track) {
    Properties properties = track.properties();
    String event = track.event();
    if (checkAndPerformEcommerceEvent(event, null, properties)) {
      return;
    }
    String category = properties.getString("category");
    String label = properties.getString("label");
    Integer value = properties.getInteger("value");
    tracker.send(new HitBuilders.EventBuilder().setCategory(category)
        .setAction(event)
        .setLabel(label)
        .setValue(value)
        .build());
  }

  boolean checkAndPerformEcommerceEvent(String event, String category, Properties props) {
    if (itemEventNames.contains(event)) {
      sendItem(category, props);
      return true;
    } else if (COMPLETED_ORDER_EVENT_NAME.equals(event)) {
      // Only sent via .track so won't have category
      sendTransaction(props);
      return true;
    }
    return false;
  }

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
        .setRevenue(nullOrDefault(revenue, 0d))
        .setTax(nullOrDefault(tax, 0d))
        .setShipping(nullOrDefault(shipping, 0d))
        .setCurrencyCode(currency)
        .build();
  }

  private void sendTransaction(Properties properties) {
    List<Object> products = (List<Object>) properties.get("products");
    if (!isNullOrEmpty(products)) {
      for (Object product : products) {
        try {
          tracker.send(productToMap(null, (Map<String, Object>) product));
        } catch (ClassCastException e) {
          Logger.e(e, "Could not convert product to JsonMap.");
        }
      }
    }
    tracker.send(transactionToMap(properties));
  }

  private void sendItem(String categoryName, Properties props) {
    tracker.send(productToMap(categoryName, props));
  }

  static Map<String, String> productToMap(String categoryName, Map<String, Object> rawProduct) {
    JsonMap product = new JsonMap(rawProduct);
    String id = product.getString("userId");
    String sku = product.getString("sku");
    String name = product.getString("name");
    Double price = product.getDouble("price");
    Long quantity = product.getLong("quantity");
    String category = nullOrDefault(product.getString("category"), categoryName);
    // Specific for GA
    String currency = product.getString("currency");

    return new HitBuilders.ItemBuilder() //
        .setTransactionId(id)
        .setName(name)
        .setSku(sku)
        .setCategory(category)
        .setPrice(nullOrDefault(price, 0d))
        .setQuantity(nullOrDefault(quantity, 1L))
        .setCurrencyCode(currency)
        .build();
  }

  @Override public Tracker getUnderlyingInstance() {
    return tracker;
  }

  @Override public String className() {
    return "Google Analytics";
  }

  @Override public String key() {
    return "com.google.android.gms.analytics.GoogleAnalytics";
  }
}
