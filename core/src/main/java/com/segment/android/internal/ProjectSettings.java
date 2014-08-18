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

package com.segment.android.internal;

import java.util.List;
import java.util.Map;

public class ProjectSettings {
  //CHECKSTYLE:OFF
  // We could use serializedName but easier this way so the configuration is passed to whichever
  // json library is used without needing multiple annotations
  AmplitudeSettings Amplitude;
  BugsnagSettings Bugsnag;
  CrittercismSettings Crittercism;
  FlurrySettings Flurry;
  GoogleAnalyticsSettings googleAnalytics;
  MixpanelSettings Mixpanel;
  //CHECKSTYLE:ON

  static class AmplitudeSettings {
    String apiKey;
    boolean trackAllPages;
    boolean trackCategorizedPages;
    boolean trackNamedPages;
  }

  static class BugsnagSettings {
    String apiKey;
    boolean useSSL;
  }

  static class CountlySettings {
    String apiKey;
    String serverUrl;
  }

  static class CrittercismSettings {
    String appId;
    boolean includeVersionCode;
    boolean shouldCollectLogcat;
  }

  static class FlurrySettings {
    String apiKey;
    boolean captureUncaughtExceptions;
    boolean useHttps;
    int sessionContinueSeconds;
  }

  static class GoogleAnalyticsSettings {
    boolean sendUserId;
    boolean reportUncaughtExceptions;
    boolean anonymizeIp;
    boolean classic;
    String domain;
    boolean doubleClick;
    boolean enhancedLinkAttribution;
    List<String> ignoredReferrers;
    boolean includeSearch;
    boolean initialPageView;
    String mobileTrackingId;
    String serversideTrackingId;
    boolean serversideClassic;
    int siteSpeedSampleRate;
    String trackingId;
    boolean trackCategorizedPages;
    boolean trackNamedPages;
    Map<String, String> dimensions;
    Map<String, String> metric;
  }

  static class LocalyticsSettings {
    String appKey;
  }

  static class MixpanelSettings {
    String apiKey;
    boolean people;
    String token;
    boolean trackAllPages;
    boolean trackCategorizedPages;
    boolean trackNamedPages;
    List<String> increments;
    boolean legacySuperProperties;
  }

  static class QuantcastSettings {
    String apiKey;
    String pCode;
    String advertise;
  }

  static class TapstreamSettings {
    String accountName;
    String sdkSecret;
    boolean trackAllPages;
    boolean trackCategorizedPages;
    boolean trackNamedPages;
  }
}
