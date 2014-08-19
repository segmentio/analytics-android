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

package com.segment.android.internal.settings;

import com.amplitude.api.Amplitude;
import com.segment.android.internal.integrations.AmplitudeIntegration;
import com.segment.android.json.JsonMap;
import java.util.List;
import java.util.Map;


public class ProjectSettings extends JsonMap {
  public ProjectSettings(String json) {
    super(json);
  }


  public static class BugsnagSettings {
    public String apiKey;
    public boolean useSSL;
  }

  static class CountlySettings {
    public String apiKey;
    public String serverUrl;
  }

  public static class CrittercismSettings {
    public String appId;
    public boolean includeVersionCode;
    public boolean shouldCollectLogcat;
  }

  public static class FlurrySettings {
    public String apiKey;
    public boolean captureUncaughtExceptions;
    public boolean useHttps;
    public int sessionContinueSeconds;
  }

  public static class GoogleAnalyticsSettings {
    public boolean sendUserId;
    public boolean reportUncaughtExceptions;
    public boolean anonymizeIp;
    public boolean classic;
    public String domain;
    public boolean doubleClick;
    public boolean enhancedLinkAttribution;
    public List<String> ignoredReferrers;
    public boolean includeSearch;
    public boolean initialPageView;
    public String mobileTrackingId;
    public String serversideTrackingId;
    public boolean serversideClassic;
    public int siteSpeedSampleRate;
    public String trackingId;
    public boolean trackCategorizedPages;
    public boolean trackNamedPages;
    public Map<String, String> dimensions;
    public Map<String, String> metric;
  }

  public static class LocalyticsSettings {
    public String appKey;
  }

  public static class MixpanelSettings {
    public String apiKey;
    public boolean people;
    public String token;
    public boolean trackAllPages;
    public boolean trackCategorizedPages;
    public boolean trackNamedPages;
    public List<String> increments;
    public boolean legacySuperProperties;
  }

  public static class QuantcastSettings {
    public String apiKey;
    public String pCode;
    public String advertise;
  }

  public static class TapstreamSettings {
    public String accountName;
    public String sdkSecret;
    public boolean trackAllPages;
    public boolean trackCategorizedPages;
    public boolean trackNamedPages;
  }
}
