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
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger.LogLevel;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.segment.android.Logger;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integration.SimpleIntegration;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import java.lang.Thread.UncaughtExceptionHandler;

public class GoogleAnalyticsIntegration extends SimpleIntegration {

  private static class SettingKey {

    private static final String TRACKING_ID = "mobileTrackingId";

    private static final String SAMPLING_FREQUENCY = "sampleFrequency";
    private static final String ANONYMIZE_IP_TRACKING = "anonymizeIp";
    private static final String REPORT_UNCAUGHT_EXCEPTIONS = "reportUncaughtExceptions";
    private static final String USE_HTTPS = "useHttps";
  }

  private Tracker tracker;
  private boolean optedOut;

  @Override
  public String getKey() {
    return "Google Analytics";
  }

  @Override
  public void validate(EasyJSONObject settings) throws InvalidSettingsException {

    if (TextUtils.isEmpty(settings.getString(SettingKey.TRACKING_ID))) {
      throw new InvalidSettingsException(SettingKey.TRACKING_ID,
          "Google Analytics requires the trackingId (UA-XXXXXXXX-XX) setting.");
    }
  }

  private void initialize(Context context) {

    EasyJSONObject settings = this.getSettings();

    // docs: https://developers.google.com/analytics/devguides/collection/android/v2/parameters

    // The Google Analytics tracking ID to which to send your data. Dashes in the ID must be
    // unencoded. You can disable your tracking by not providing this value.
    String trackingId = settings.getString(SettingKey.TRACKING_ID);
    // The sample rate to use. Default is 100.0. It can be any value between 0.0 and 100.0
    Double sampleFrequency = settings.getDouble(SettingKey.SAMPLING_FREQUENCY, Double.valueOf(100));
    // Tells Google Analytics to anonymize the information sent by the tracker objects by
    // removing the last octet of the IP address prior to its storage. Note that this will slightly
    // reduce the accuracy of geographic reporting. false by default.
    boolean anonymizeIp = settings.getBoolean(SettingKey.ANONYMIZE_IP_TRACKING, false);
    // Automatically track an Exception each time an uncaught exception is thrown
    // in your application. false by default.
    boolean reportUncaughtExceptions =
        settings.getBoolean(SettingKey.REPORT_UNCAUGHT_EXCEPTIONS, false);
    // Log to the server using https
    boolean useHttps = settings.getBoolean(SettingKey.USE_HTTPS, false);

    GoogleAnalytics gaInstance = GoogleAnalytics.getInstance(context);

    if (Logger.isLogging()) gaInstance.getLogger().setLogLevel(LogLevel.VERBOSE);

    tracker = gaInstance.getTracker(trackingId);
    tracker.set(Fields.SAMPLE_RATE, "" + sampleFrequency);
    tracker.set(Fields.ANONYMIZE_IP, "" + anonymizeIp);
    tracker.set(Fields.USE_SECURE, "" + useHttps);

    if (reportUncaughtExceptions) enableAutomaticExceptionTracking(tracker, context);

    gaInstance.setDefaultTracker(tracker);

    ready();
  }

  private void enableAutomaticExceptionTracking(Tracker tracker, Context context) {
    UncaughtExceptionHandler myHandler =
        new ExceptionReporter(tracker, GAServiceManager.getInstance(),
            Thread.getDefaultUncaughtExceptionHandler(), context);

    Thread.setDefaultUncaughtExceptionHandler(myHandler);
  }

  private void applyOptOut(Activity activity) {
    GoogleAnalytics.getInstance(activity).setAppOptOut(optedOut);
  }

  @Override
  public void onCreate(Context context) {
    initialize(context);
  }

  @Override
  public void onActivityStart(Activity activity) {
    applyOptOut(activity);
    EasyTracker.getInstance(activity).activityStart(activity);
  }

  @Override
  public void onActivityStop(Activity activity) {
    applyOptOut(activity);
    EasyTracker.getInstance(activity).activityStop(activity);
  }

  @Override
  public void screen(Screen screen) {
    String screenName = screen.getName();
    tracker.send(MapBuilder.createAppView().set(Fields.SCREEN_NAME, screenName).build());
  }

  @Override
  public void track(Track track) {
    Props properties = track.getProperties();

    String category = properties.getString("category", "All");
    String action = track.getEvent();
    String label = properties.getString("label", null);
    Integer value = properties.getInt("value", 0);

    tracker.send(MapBuilder.createEvent(category, action, label, value.longValue()).build());
  }

  @Override
  public void toggleOptOut(boolean optedOut) {
    this.optedOut = optedOut;
  }
}
