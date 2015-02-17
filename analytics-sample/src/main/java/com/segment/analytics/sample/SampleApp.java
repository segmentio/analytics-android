package com.segment.analytics.sample;

import android.app.Application;
import com.segment.analytics.Analytics;

public class SampleApp extends Application {
  private static final String ANALYTICS_WRITE_KEY = "l8v1ga655b";

  @Override public void onCreate() {
    super.onCreate();

    // Initialize a new instance of the Analytics client.
    Analytics analytics = new Analytics.Builder(this, ANALYTICS_WRITE_KEY) //
        .build();

    // Set the initialized instance as a globally accessible instance.
    Analytics.setSingletonInstance(analytics);

    // Now anytime you call Analytics.with, the custom instance will be returned.
    Analytics.with(this).track("App Launched");
  }
}
