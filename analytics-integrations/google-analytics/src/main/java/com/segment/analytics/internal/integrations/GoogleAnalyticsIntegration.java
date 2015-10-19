package com.segment.analytics.internal.integrations;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.HitBuilders.TransactionBuilder;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Properties.Product;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.Log;
import com.segment.analytics.internal.Utils;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.segment.analytics.Analytics.LogLevel.DEBUG;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
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
  private static final String DIMENSION_PREFIX = "dimension";
  private static final String DIMENSION_PREFIX_KEY = "&cd";
  private static final String METRIC_PREFIX = "metric";
  private static final String METRIC_PREFIX_KEY = "&cm";
  private static final String USER_ID_KEY = "&uid";
  private static final String QUANTITY_KEY = "quantity";
  private static final String LABEL_KEY = "label";

  Tracker tracker;
  GoogleAnalytics googleAnalyticsInstance;
  boolean sendUserId;
  ValueMap customDimensions;
  ValueMap customMetrics;
  Log log;

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
    log = analytics.getLogger().newLogger(GOOGLE_ANALYTICS_KEY);

    if (log.logLevel == VERBOSE) {
      googleAnalyticsInstance.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
      log.verbose("GoogleAnalytics.getInstance(context).getLogger().setLogLevel(VERBOSE);");
    } else if (log.logLevel == DEBUG) {
      googleAnalyticsInstance.getLogger().setLogLevel(Logger.LogLevel.INFO);
      log.verbose("GoogleAnalytics.getInstance(context).getLogger().setLogLevel(INFO);");
    } else if (log.logLevel == INFO) {
      googleAnalyticsInstance.getLogger().setLogLevel(Logger.LogLevel.WARNING);
      log.verbose("GoogleAnalytics.getInstance(context).getLogger().setLogLevel(WARNING);");
    }

    boolean anonymizeIp = settings.getBoolean("anonymizeIp", false);
    tracker.setAnonymizeIp(anonymizeIp);
    log.verbose("tracker.setAnonymizeIp(%s);", anonymizeIp);

    boolean reportUncaughtExceptions = settings.getBoolean("reportUncaughtExceptions", false);
    if (reportUncaughtExceptions) {
      Thread.UncaughtExceptionHandler myHandler =
          new ExceptionReporter(tracker, Thread.getDefaultUncaughtExceptionHandler(), context);
      Thread.setDefaultUncaughtExceptionHandler(myHandler);
      log.verbose("Thread.setDefaultUncaughtExceptionHandler(new ExceptionReporter(...));");
    }

    sendUserId = settings.getBoolean("sendUserId", false);
    customDimensions = settings.getValueMap("dimensions");
    customMetrics = settings.getValueMap("metrics");
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    googleAnalyticsInstance.reportActivityStart(activity);
    log.verbose("GoogleAnalytics.getInstance(context).reportActivityStart(activity);");
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    log.verbose("GoogleAnalytics.getInstance(context).reportActivityStop(activity);");
    googleAnalyticsInstance.reportActivityStop(activity);
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);

    Properties properties = screen.properties();
    String screenName = screen.event();

    sendProductEvent(screenName, screen.category(), properties);

    tracker.setScreenName(screenName);
    log.verbose("tracker.setScreenName(%s);", screenName);

    ScreenViewHitBuilder hitBuilder = new ScreenViewHitBuilder();
    attachCustomDimensionsAndMetrics(hitBuilder, properties);
    Map<String, String> hit = hitBuilder.build();
    tracker.send(hit);
    log.verbose("tracker.send(%s);", hit);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    if (sendUserId) {
      String userId = identify.userId();
      tracker.set(USER_ID_KEY, userId);
      log.verbose("tracker.set(%s, %s);", USER_ID_KEY, userId);
    }

    // Set traits, custom dimensions, and custom metrics on the shared tracker.
    for (Map.Entry<String, Object> entry : identify.traits().entrySet()) {
      String trait = entry.getKey();
      if (customDimensions.containsKey(trait)) {
        String dimension = customDimensions.getString(trait) //
            .replace(DIMENSION_PREFIX, DIMENSION_PREFIX_KEY);
        String value = String.valueOf(entry.getValue());
        tracker.set(dimension, value);
        log.verbose("tracker.set(%s, %s);", dimension, value);
      }
      if (customMetrics.containsKey(trait)) {
        String metric = customMetrics.getString(trait) //
            .replace(METRIC_PREFIX, METRIC_PREFIX_KEY);
        String value = String.valueOf(entry.getValue());
        tracker.set(metric, value);
        log.verbose("tracker.set(%s, %s);", metric, value);
      }
    }
  }

  @Override public void track(TrackPayload track) {
    Properties properties = track.properties();
    String event = track.event();
    String category = properties.category();

    sendProductEvent(event, category, properties);

    if (COMPLETED_ORDER_PATTERN.matcher(event).matches()) {
      List<Product> products = properties.products();
      if (!isNullOrEmpty(products)) {
        for (int i = 0; i < products.size(); i++) {
          Product product = products.get(i);
          ItemHitBuilder hitBuilder = new ItemHitBuilder();
          hitBuilder.setTransactionId(product.id())
              .setName(product.name())
              .setSku(product.sku())
              .setPrice(product.price())
              .setQuantity(product.getLong(QUANTITY_KEY, 0))
              .build();
          attachCustomDimensionsAndMetrics(hitBuilder, properties);
          Map<String, String> hit = hitBuilder.build();
          tracker.send(hit);
          log.verbose("tracker.send(%s);", hit);
        }
      }
      TransactionBuilder transactionBuilder = new TransactionBuilder();
      transactionBuilder.setTransactionId(properties.orderId())
          .setCurrencyCode(properties.currency())
          .setRevenue(properties.total())
          .setTax(properties.tax())
          .setShipping(properties.shipping());
      Map<String, String> transaction = transactionBuilder.build();
      tracker.send(transaction);
      log.verbose("tracker.send(%s);", transaction);
    }

    String label = properties.getString(LABEL_KEY);
    EventHitBuilder eventHitBuilder = new EventHitBuilder();
    eventHitBuilder.setAction(event)
        .setCategory(isNullOrEmpty(category) ? DEFAULT_CATEGORY : category)
        .setLabel(label)
        .setValue((int) properties.value());
    attachCustomDimensionsAndMetrics(eventHitBuilder, properties);
    Map<String, String> eventHit = eventHitBuilder.build();
    tracker.send(eventHit);
    log.verbose("tracker.send(%s);", eventHit);
  }

  /**
   * HitBuilder declares setCustomDimension and setCustomMetric, but it is a protected class, so
   * attachCustomDimensionsAndMetrics can't accept it as a parameter. Write our own wrapper that
   * exposes the required methods.
   */
  interface CustomHitBuilder {
    CustomHitBuilder setCustomDimension(int index, String dimension);

    CustomHitBuilder setCustomMetric(int index, float metric);
  }

  static class EventHitBuilder extends HitBuilders.EventBuilder implements CustomHitBuilder {
    @Override public EventHitBuilder setCustomDimension(int index, String dimension) {
      super.setCustomDimension(index, dimension);
      return this;
    }

    @Override public EventHitBuilder setCustomMetric(int index, float metric) {
      super.setCustomMetric(index, metric);
      return this;
    }
  }

  static class ScreenViewHitBuilder extends HitBuilders.ScreenViewBuilder
      implements CustomHitBuilder {
    @Override public ScreenViewHitBuilder setCustomDimension(int index, String dimension) {
      super.setCustomDimension(index, dimension);
      return this;
    }

    @Override public ScreenViewHitBuilder setCustomMetric(int index, float metric) {
      super.setCustomMetric(index, metric);
      return this;
    }
  }

  static class ItemHitBuilder extends HitBuilders.ItemBuilder implements CustomHitBuilder {
    @Override public ItemHitBuilder setCustomDimension(int index, String dimension) {
      super.setCustomDimension(index, dimension);
      return this;
    }

    @Override public ItemHitBuilder setCustomMetric(int index, float metric) {
      super.setCustomMetric(index, metric);
      return this;
    }
  }

  /** Set custom dimensions and metrics on the hit. */
  void attachCustomDimensionsAndMetrics(CustomHitBuilder hitBuilder, Properties properties) {
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      String property = entry.getKey();
      if (customDimensions.containsKey(property)) {
        int dimension =
            extractNumber(customDimensions.getString(property), DIMENSION_PREFIX.length());
        hitBuilder.setCustomDimension(dimension, String.valueOf(entry.getValue()));
      }
      if (customMetrics.containsKey(property)) {
        int metric = extractNumber(customMetrics.getString(property), METRIC_PREFIX.length());
        hitBuilder.setCustomMetric(metric, Utils.coerceToFloat(entry.getValue(), 0));
      }
    }
  }

  // e.g. extractNumber("dimension3", 8) returns 3
  // e.g. extractNumber("dimension9", 8) returns 9
  private static int extractNumber(String text, int start) {
    if (isNullOrEmpty(text)) {
      return 0;
    }
    return Integer.parseInt(text.substring(start, text.length()));
  }

  @Override public void flush() {
    googleAnalyticsInstance.dispatchLocalHits();
    log.verbose("GoogleAnalytics.getInstance(context).dispatchLocalHits();");
  }

  /** Send a product event. */
  void sendProductEvent(String event, String category, Properties properties) {
    if (!PRODUCT_EVENT_NAME_PATTERN.matcher(event).matches()) {
      return;
    }

    ItemHitBuilder itemHitBuilder = new ItemHitBuilder();
    itemHitBuilder.setTransactionId(properties.productId())
        .setCurrencyCode(properties.currency())
        .setName(properties.name())
        .setSku(properties.sku())
        .setCategory(isNullOrEmpty(category) ? DEFAULT_CATEGORY : category)
        .setPrice(properties.price())
        .setQuantity(properties.getLong(QUANTITY_KEY, 0))
        .build();
    attachCustomDimensionsAndMetrics(itemHitBuilder, properties);
    Map<String, String> itemHit = itemHitBuilder.build();
    tracker.send(itemHit);
    log.verbose("tracker.send(%s);", itemHit);
  }

  @Override public Tracker getUnderlyingInstance() {
    return tracker;
  }

  @Override public String key() {
    return GOOGLE_ANALYTICS_KEY;
  }
}
