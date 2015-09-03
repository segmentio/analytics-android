package com.segment.analytics.internal.integrations;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.Utils;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.segment.analytics.internal.Utils.hasPermission;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Google Analytics is the most popular analytics tool for the web because it’s free and sports a
 * wide range of features. It’s especially good at measuring traffic sources and ad campaigns.
 *
 * @see <a href="http://www.google.com/analytics/">Google Analytics</a>
 * @see <a href="https://segment.com/docs/integrations/google-analytics/">Google Analytics
 * Integration</a>
 * @see <a href="https://developers.google.com/analytics/devguides/collection/android/v4/">Google
 * Analyitcs Android SDK</a>
 */
public class GoogleAnalyticsIntegration extends AbstractIntegration<Tracker> {

  static final String DEFAULT_CATEGORY = "All";
  static final Pattern COMPLETED_ORDER_PATTERN =
      Pattern.compile("completed *order", Pattern.CASE_INSENSITIVE);
  static final Pattern PRODUCT_EVENT_NAME_PATTERN =
      Pattern.compile("((viewed)|(added)|(removed)) *product *.*", Pattern.CASE_INSENSITIVE);
  static final String GOOGLE_ANALYTICS_KEY = "Google Analytics";
  Tracker tracker;
  GoogleAnalytics googleAnalyticsInstance;
  boolean sendUserId;
  ValueMap customDimensions;
  ValueMap customMetrics;

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    Context context = analytics.getApplication();
    if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      throw new IllegalStateException("Google Analytics requires the access state permission.");
    }
    String mobileTrackingId = settings.getString("mobileTrackingId");
    if (isNullOrEmpty(mobileTrackingId)) {
      throw new IllegalStateException("mobileTrackingId is required.");
    }

    googleAnalyticsInstance = GoogleAnalytics.getInstance(context);
    tracker = googleAnalyticsInstance.newTracker(mobileTrackingId);

    Analytics.LogLevel logLevel = analytics.getLogLevel();
    if (logLevel == Analytics.LogLevel.INFO) {
      googleAnalyticsInstance.getLogger().setLogLevel(Logger.LogLevel.INFO);
    } else if (logLevel == Analytics.LogLevel.VERBOSE) {
      googleAnalyticsInstance.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
    }

    tracker.setAnonymizeIp(settings.getBoolean("anonymizeIp", false));
    if (settings.getBoolean("reportUncaughtExceptions", false)) {
      Thread.UncaughtExceptionHandler myHandler =
          new ExceptionReporter(tracker, Thread.getDefaultUncaughtExceptionHandler(), context);
      Thread.setDefaultUncaughtExceptionHandler(myHandler);
    }
    // tracker.setSampleRate(settings.getDouble("siteSpeedSampleRate", 1));
    sendUserId = settings.getBoolean("sendUserId", false);
    customDimensions = settings.getValueMap("dimensions");
    customMetrics = settings.getValueMap("metrics");
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    googleAnalyticsInstance.reportActivityStart(activity);
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    googleAnalyticsInstance.reportActivityStop(activity);
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    Properties properties = screen.properties();
    String screenName = screen.event();
    if (handleProductEvent(screenName, screen.category(), screen.properties())) {
      return;
    }

    tracker.setScreenName(screenName);

    HitBuilders.AppViewBuilder hit = new HitBuilders.AppViewBuilder();

    // Set Custom Dimensions and Metrics on the screenview hit
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      String property = entry.getKey();
      if (customDimensions.containsKey(property)) {
        int dimension =
            Integer.parseInt(customDimensions.getString(property).replace("dimension", ""));
        hit.setCustomDimension(dimension, String.valueOf(entry.getValue()));
      }
      if (customMetrics.containsKey(property)) {
        int metric = Integer.parseInt(customMetrics.getString(property).replace("metric", ""));
        hit.setCustomMetric(metric, Utils.getFloat(entry.getValue(), 0));
      }
    }

    tracker.send(hit.build());
    tracker.setScreenName(null);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    if (sendUserId) {
      tracker.set("&uid", identify.userId());
    }

    // Set Traits, Custom Dimensions, and Custom Metrics on the shared tracker
    for (Map.Entry<String, Object> entry : identify.traits().entrySet()) {
      String trait = entry.getKey();
      tracker.set(trait, String.valueOf(entry.getValue()));

      if (customDimensions.containsKey(trait)) {
        int dimension =
            Integer.parseInt(customDimensions.getString(trait).replace("dimension", ""));
        tracker.set("&cd" + dimension, String.valueOf(entry.getValue()));
      }
      if (customMetrics.containsKey(trait)) {
        int metric = Integer.parseInt(customMetrics.getString(trait).replace("metric", ""));
        tracker.set("&cm" + metric, String.valueOf(entry.getValue()));
      }
    }
  }

  @Override public void track(TrackPayload track) {
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

    HitBuilders.EventBuilder hit = new HitBuilders.EventBuilder().setCategory(category)
        .setAction(event)
        .setLabel(label)
        .setValue((int) properties.value());

    // Set Custom Dimensions and Metrics on the event hit
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      String property = entry.getKey();
      if (customDimensions.containsKey(property)) {
        int dimension =
            Integer.parseInt(customDimensions.getString(property).replace("dimension", ""));
        hit.setCustomDimension(dimension, String.valueOf(entry.getValue()));
      }
      if (customMetrics.containsKey(property)) {
        int metric = Integer.parseInt(customMetrics.getString(property).replace("metric", ""));
        hit.setCustomMetric(metric, Utils.getFloat(entry.getValue(), 0));
      }
    }

    tracker.send(hit.build());
  }

  @Override public void flush() {
    googleAnalyticsInstance.dispatchLocalHits();
  }

  /** Check if event is an ecommerce event. If it is, do it and return true, else return false. */
  boolean handleProductEvent(String event, String category, Properties properties) {
    if (isNullOrEmpty(category)) category = DEFAULT_CATEGORY;

    if (PRODUCT_EVENT_NAME_PATTERN.matcher(event).matches()) {
      tracker.send(new HitBuilders.ItemBuilder() //
          .setTransactionId(properties.productId())
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

  @Override public Tracker getUnderlyingInstance() {
    return tracker;
  }

  @Override public String key() {
    return GOOGLE_ANALYTICS_KEY;
  }
}
