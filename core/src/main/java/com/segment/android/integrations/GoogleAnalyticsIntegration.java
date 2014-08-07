/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.android.integrations;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger.LogLevel;
import com.google.android.gms.analytics.Tracker;
import com.segment.android.Logger;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashSet;
import java.util.Set;

public class GoogleAnalyticsIntegration extends SimpleIntegration {
  private static final String TRACKING_ID = "mobileTrackingId";
  private static final String SAMPLING_FREQUENCY = "sampleFrequency";
  private static final String ANONYMIZE_IP_TRACKING = "anonymizeIp";
  private static final String REPORT_UNCAUGHT_EXCEPTIONS = "reportUncaughtExceptions";
  private static final String USE_HTTPS = "useHttps";

  Tracker tracker = null; // exposed for testing
  private boolean optedOut;

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

  @Override
  public String getKey() {
    return "Google Analytics";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {

    if (TextUtils.isEmpty(settings.getString(TRACKING_ID))) {
      throw new InvalidSettingsException(TRACKING_ID,
          "Google Analytics requires the trackingId (UA-XXXXXXXX-XX) setting.");
    }
  }

  private void initialize(Context context) {

    EasyJSONObject settings = this.getSettings();

    // docs: https://developers.google.com/analytics/devguides/collection/android/v2/parameters

    // The Google Analytics tracking ID to which to send your data. Dashes in the ID must be
    // unencoded. You can disable your tracking by not providing this value.
    String trackingId = settings.getString(TRACKING_ID);
    // The sample rate to use. Default is 100.0. It can be any value between 0.0 and 100.0
    Double sampleFrequency = settings.getDouble(SAMPLING_FREQUENCY, (double) 100);
    // Tells Google Analytics to anonymize the information sent by the tracker objects by
    // removing the last octet of the IP address prior to its storage. Note that this will slightly
    // reduce the accuracy of geographic reporting. false by default.
    boolean anonymizeIp = settings.getBoolean(ANONYMIZE_IP_TRACKING, false);
    // Automatically track an Exception each time an uncaught exception is thrown
    // in your application. false by default.
    boolean reportUncaughtExceptions = settings.getBoolean(REPORT_UNCAUGHT_EXCEPTIONS, false);
    // Log to the server using https
    boolean useHttps = settings.getBoolean(USE_HTTPS, false);

    GoogleAnalytics gaInstance = GoogleAnalytics.getInstance(context);

    if (Logger.isLogging()) gaInstance.getLogger().setLogLevel(LogLevel.VERBOSE);

    if (tracker == null) tracker = gaInstance.newTracker(trackingId);
    tracker.setSampleRate(sampleFrequency);
    tracker.setAnonymizeIp(anonymizeIp);
    tracker.setUseSecure(useHttps);

    if (reportUncaughtExceptions) enableAutomaticExceptionTracking(tracker, context);

    gaInstance.setLocalDispatchPeriod(15);
    tracker.enableAutoActivityTracking(true);

    ready();
  }

  private void enableAutomaticExceptionTracking(Tracker tracker, Context context) {
    UncaughtExceptionHandler myHandler =
        new ExceptionReporter(tracker, Thread.getDefaultUncaughtExceptionHandler(), context);

    Thread.setDefaultUncaughtExceptionHandler(myHandler);
  }

  private void applyOptOut(Activity activity) {
    GoogleAnalytics.getInstance(activity).setAppOptOut(optedOut);
  }

  @Override
  public void onCreate(Context context) {
    initialize(context);
  }

  // onActivityStart and onActivityStop are no longer required in v4 - see
  // tracker.enableAutoActivityTracking keeping for applyOptOut

  @Override
  public void onActivityStart(Activity activity) {
    applyOptOut(activity);
  }

  @Override
  public void onActivityStop(Activity activity) {
    applyOptOut(activity);
  }

  @Override
  public void screen(Screen screen) {
    String screenName = screen.getName();
    if (checkAndPerformEcommerceEvent(screenName, screen.getCategory(), screen.getProperties())) {
      return;
    }

    tracker.setScreenName(screenName);
    tracker.send(new HitBuilders.AppViewBuilder().build());
    tracker.setScreenName(null);
  }

  @Override
  public void track(Track track) {
    Props properties = track.getProperties();
    if (checkAndPerformEcommerceEvent(track.getEvent(), null, properties)) {
      return;
    }
    String category = properties.getString("category", "All");
    String action = track.getEvent();
    String label = properties.getString("label", null);
    Integer value = properties.getInt("value", 0);
    tracker.send(new HitBuilders.EventBuilder().setCategory(category)
        .setAction(action)
        .setLabel(label)
        .setValue(value)
        .build());
  }

  boolean checkAndPerformEcommerceEvent(String event, String categoryName, Props props) {
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

  private void sendItem(String categoryName, Props props) {
    String id = props.getString("id");
    String sku = props.getString("sku");
    String name = props.getString("name");
    double price = props.getDouble("price", 0d);
    long quantity = props.getDouble("quantity", 1d).longValue();
    String category = props.getString("category", categoryName);

    // Specific for GA
    String currency = props.getString("currency");

    tracker.send(new HitBuilders.ItemBuilder() //
        .setTransactionId(id)
        .setName(name)
        .setSku(sku)
        .setCategory(category)
        .setPrice(price)
        .setQuantity(quantity)
        .setCurrencyCode(currency)
        .build());
  }

  private void sendTransaction(Props props) {
    String id = props.getString("id");
    // skip total
    double revenue = props.getDouble("revenue", 0d);
    double shipping = props.getDouble("shipping", 0d);
    double tax = props.getDouble("tax", 0d);

    // Specific for GA
    String affiliation = props.getString("affiliation");
    String currency = props.getString("currency");

    tracker.send(new HitBuilders.TransactionBuilder() //
        .setTransactionId(id)
        .setAffiliation(affiliation)
        .setRevenue(revenue)
        .setTax(tax)
        .setShipping(shipping)
        .setCurrencyCode(currency)
        .build());
  }

  @Override
  public void toggleOptOut(boolean optedOut) {
    this.optedOut = optedOut;
  }
}