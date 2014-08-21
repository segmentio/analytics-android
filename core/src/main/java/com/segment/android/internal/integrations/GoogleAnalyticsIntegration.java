package com.segment.android.internal.integrations;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.segment.android.Properties;
import com.segment.android.internal.Logger;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.ProjectSettings;
import com.segment.android.json.JsonMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.segment.android.internal.Utils.hasPermission;
import static com.segment.android.internal.Utils.isNullOrEmpty;

public class GoogleAnalyticsIntegration extends AbstractIntegration<Tracker> {
  Tracker tracker;
  GoogleAnalytics googleAnalyticsInstance;
  boolean optedOut;

  private Set<String> itemEventNames; // sends item events
  private static final String COMPLETED_ORDER_EVENT_NAME = "Completed Order";

  Set<String> getItemEventNames() {
    if (itemEventNames == null) {
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
    return itemEventNames;
  }

  public GoogleAnalyticsIntegration() throws ClassNotFoundException {
    super("Google Analytics", "com.google.android.gms.analytics.GoogleAnalytics");
  }

  @Override public void validate(Context context) throws InvalidConfigurationException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new InvalidConfigurationException(
          "Google Analytics requires the access state permission.");
    }
  }

  @Override public boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException {
    if (!projectSettings.containsKey(key())) {
      return false;
    }
    GoogleAnalyticsSettings settings =
        new GoogleAnalyticsSettings(projectSettings.getJsonMap(key()));

    googleAnalyticsInstance = GoogleAnalytics.getInstance(context);
    googleAnalyticsInstance.getLogger().setLogLevel(Logger.isLogging() ? Log.VERBOSE : Log.ERROR);

    String trackingId = settings.mobileTrackingId();
    if (isNullOrEmpty(trackingId)) trackingId = settings.trackingId();
    tracker = googleAnalyticsInstance.newTracker(trackingId);
    tracker.setAnonymizeIp(settings.anonymizeIp());
    tracker.setSampleRate(settings.siteSpeedSampleRate());
    if (settings.reportUncaughtExceptions()) enableAutomaticExceptionTracking(context);
    tracker.enableAutoActivityTracking(true);

    return true;
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
    String screenName = screen.name();
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

  boolean checkAndPerformEcommerceEvent(String event, String categoryName, Properties props) {
    if (getItemEventNames().contains(event)) {
      sendItem(categoryName, props);
      return true;
    } else if (COMPLETED_ORDER_EVENT_NAME.equals(event)) {
      // Only sent via .track so won't have categoryName
      sendTransaction(props);
      return true;
    }
    return false;
  }

  static Map<String, String> transactionToMap(Properties props) {
    String id = props.getString("id");
    // skip total
    Double revenue = props.getDouble("revenue");
    Double shipping = props.getDouble("shipping");
    Double tax = props.getDouble("tax");

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

  private void sendTransaction(Properties properties) {
    tracker.send(transactionToMap(properties));
    List<JsonMap> products = (List<JsonMap>) properties.get("products");
    if (!isNullOrEmpty(products)) {
      for (JsonMap product : products) {
        tracker.send(productToMap(null, product));
      }
    }
  }

  private void sendItem(String categoryName, Properties props) {
    tracker.send(productToMap(categoryName, props));
  }

  static Map<String, String> productToMap(String categoryName, JsonMap product) {
    String id = product.getString("id");
    String sku = product.getString("sku");
    String name = product.getString("name");
    Double price = product.getDouble("price");
    Long quantity = product.getLong("quantity");
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

  static class GoogleAnalyticsSettings extends JsonMap {
    GoogleAnalyticsSettings(Map<String, Object> delegate) {
      super(delegate);
    }

    Boolean sendUserId() {
      return getBoolean("sendUserId");
    }

    Boolean reportUncaughtExceptions() {
      return getBoolean("reportUncaughtExceptions");
    }

    Boolean anonymizeIp() {
      return getBoolean("anonymizeIp");
    }

    Boolean classic() {
      return getBoolean("classic");
    }

    String domain() {
      return getString("domain");
    }

    Boolean doubleClick() {
      return getBoolean("doubleClick");
    }

    Boolean enhancedLinkAttribution() {
      return getBoolean("enhancedLinkAttribution");
    }

    List<String> ignoredReferrers() {
      // todo: check if valid
      return (List<String>) get("ignoredReferrers");
    }

    Boolean includeSearch() {
      return getBoolean("includeSearch");
    }

    Boolean initialPageView() {
      return getBoolean("initialPageView");
    }

    String mobileTrackingId() {
      return getString("mobileTrackingId");
    }

    String serversideTrackingId() {
      return getString("serversideTrackingId");
    }

    Boolean serversideClassic() {
      return getBoolean("serversideClassic");
    }

    Integer siteSpeedSampleRate() {
      return getInteger("siteSpeedSampleRate");
    }

    String trackingId() {
      return getString("trackingId");
    }

    Boolean trackCategorizedPages() {
      return getBoolean("trackCategorizedPages");
    }

    Boolean trackNamedPages() {
      return getBoolean("trackNamedPages");
    }

    JsonMap dimensions() {
      return getJsonMap("dimensions");
    }

    JsonMap metric() {
      return getJsonMap("metric");
    }
  }

  @Override public Tracker getUnderlyingInstance() {
    return tracker;
  }
}
