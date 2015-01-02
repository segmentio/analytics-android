package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
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
 * @see <a href="https://Segment/docs/integrations/google-analytics/">Google Analytics
 * Integration</a>
 * @see <a href="https://developers.google.com/analytics/devguides/collection/android/v4/">Google
 * Analyitcs Android SDK</a>
 */
public class GoogleAnalyticsIntegration extends AbstractIntegration<Tracker> {
  static final String DEFAULT_CATEGORY = "All";
  static final Pattern COMPLETED_ORDER_PATTERN =
      Pattern.compile("completed *order", Pattern.CASE_INSENSITIVE);
  static final Pattern PRODUCT_EVENT_PATTERN =
      Pattern.compile("((viewed)|(added)|(removed)) *product *.*", Pattern.CASE_INSENSITIVE);
  static final String GOOGLE_ANALYTICS_KEY = "Google Analytics";
  Tracker tracker;
  GoogleAnalytics googleAnalyticsInstance;
  boolean sendUserId;

  @Override void initialize(Context context, ValueMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new IllegalStateException("Google Analytics requires the access state permission.");
    }

    googleAnalyticsInstance = GoogleAnalytics.getInstance(context);
    if (debuggingEnabled) {
      googleAnalyticsInstance.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
    }

    // Look up the mobileTrackingId, if unavailable, fallback to the trackingId
    String trackingId = settings.getString("mobileTrackingId");
    if (isNullOrEmpty(trackingId)) trackingId = settings.getString("trackingId");
    tracker = googleAnalyticsInstance.newTracker(trackingId);

    tracker.setAnonymizeIp(settings.getBoolean("anonymizeIp", false));
    if (settings.getBoolean("reportUncaughtExceptions", false)) {
      Thread.UncaughtExceptionHandler myHandler =
          new ExceptionReporter(tracker, Thread.getDefaultUncaughtExceptionHandler(), context);
      Thread.setDefaultUncaughtExceptionHandler(myHandler);
    }
    // tracker.setSampleRate(settings.getDouble("siteSpeedSampleRate", 1));
    sendUserId = settings.getBoolean("sendUserId", false);
  }

  @Override void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    googleAnalyticsInstance.reportActivityStart(activity);
  }

  @Override void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    googleAnalyticsInstance.reportActivityStop(activity);
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    String screenName = screen.event();
    if (handleProductEvent(screenName, screen.category(), screen.properties())) {
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
    if (handleProductEvent(event, properties.category(), properties)) {
      return;
    }
    if (COMPLETED_ORDER_PATTERN.matcher(event).matches()) {
      List<Properties.Product> products = properties.products();
      if (!isNullOrEmpty(products)) {
        for (Properties.Product product : products) {
          tracker.send(new HitBuilders.ItemBuilder() //
              .setTransactionId(product.id())
              .setName(product.name())
              .setSku(product.sku())
              .setPrice(product.price())
              .setQuantity(product.getLong("quantity", 0))
              .build());
        }
      }
      tracker.send(new HitBuilders.ItemBuilder() //
          .setTransactionId(properties.orderId())
          .setCurrencyCode(properties.currency())
          .setPrice(properties.total())
          .build());
    }

    String category = properties.category();
    if (isNullOrEmpty(category)) category = DEFAULT_CATEGORY;

    String label = properties.getString("label");
    tracker.send(new HitBuilders.EventBuilder().setCategory(category)
        .setAction(event)
        .setLabel(label)
        .setValue((int) properties.value())
        .build());
  }

  @Override void flush() {
    googleAnalyticsInstance.dispatchLocalHits();
  }

  /** Check if event is an ecommerce event. If it is, do it and return true, else return false. */
  boolean handleProductEvent(String event, String category, Properties properties) {
    if (isNullOrEmpty(category)) category = DEFAULT_CATEGORY;

    if (PRODUCT_EVENT_PATTERN.matcher(event).matches()) {
      tracker.send(new HitBuilders.ItemBuilder() //
          .setTransactionId(properties.id())
          .setCurrencyCode(properties.currency())
          .setName(properties.name())
          .setSku(properties.sku())
          .setCategory(category)
          .setPrice(properties.price())
          .setQuantity(properties.getLong("quantity", 0))
          .build());
      return true;
    }
    return false;
  }

  @Override Tracker getUnderlyingInstance() {
    return tracker;
  }

  @Override String key() {
    return GOOGLE_ANALYTICS_KEY;
  }
}
