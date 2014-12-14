package com.segment.analytics.sample;

import android.app.Application;
import com.segment.analytics.Analytics;

public class SampleApp extends Application {
  @Override public void onCreate() {
    super.onCreate();

    /**
     * We recommend initializing the client in an application class, because some integrations
     * (such as Flurry) need to be notified when the activity is started. We automatically listen
     * for the activity lifecycle but if you initialize in the onCreate of an Activity, the event
     * has already occurred and we aren't notified.
     */
    Analytics.with(this).track("App Launched");
  }
}
