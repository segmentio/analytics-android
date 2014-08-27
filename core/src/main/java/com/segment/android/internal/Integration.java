package com.segment.android.internal;

public enum Integration {
  AMPLITUDE("Amplitude", "com.amplitude.api.Amplitude"),
  BUGSNAG("Bugsnag", "com.bugsnag.android.Bugsnag"),
  COUNTLY("Countly", "ly.count.android.api.Countly"),
  CRITTERCISM("Crittercism", "com.crittercism.app.Crittercism"),
  FLURRY("Flurry", "com.flurry.android.FlurryAgent"),
  GOOGLE_ANALYTICS("Google Analytics", "com.google.android.gms.analytics.GoogleAnalytics"),
  LOCALYTICS("Localytics", "com.localytics.android.LocalyticsSession"),
  MIXPANEL("Mixpanel", "com.mixpanel.android.mpmetrics.MixpanelAPI"),
  QUANTCAST("Quantcast", "com.quantcast.measurement.service.QuantcastClient"),
  TAPSTREAM("Tapstream", "com.tapstream.sdk.Tapstream");

  private final String key;
  private final String className;

  Integration(String key, String className) {
    this.key = key;
    this.className = className;
  }

  public String className() {
    return className;
  }

  public String key() {
    return key;
  }
}
