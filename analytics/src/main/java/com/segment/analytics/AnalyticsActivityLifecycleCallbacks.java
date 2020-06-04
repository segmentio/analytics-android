/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014 Segment.io, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class AnalyticsActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
  private Analytics analytics;
  private ExecutorService analyticsExecutor;
  private Boolean shouldTrackApplicationLifecycleEvents;
  private Boolean trackAttributionInformation;
  private Boolean trackDeepLinks;
  private Boolean shouldRecordScreenViews;
  private PackageInfo packageInfo;

  private AtomicBoolean trackedApplicationLifecycleEvents;
  private AtomicInteger numberOfActivities;
  private AtomicBoolean firstLaunch;

  private AnalyticsActivityLifecycleCallbacks(
      Analytics analytics,
      ExecutorService analyticsExecutor,
      Boolean shouldTrackApplicationLifecycleEvents,
      Boolean trackAttributionInformation,
      Boolean trackDeepLinks,
      Boolean shouldRecordScreenViews,
      PackageInfo packageInfo) {
    this.trackedApplicationLifecycleEvents = new AtomicBoolean(false);
    this.numberOfActivities = new AtomicInteger(1);
    this.firstLaunch = new AtomicBoolean(false);
    this.analytics = analytics;
    this.analyticsExecutor = analyticsExecutor;
    this.shouldTrackApplicationLifecycleEvents = shouldTrackApplicationLifecycleEvents;
    this.trackAttributionInformation = trackAttributionInformation;
    this.trackDeepLinks = trackDeepLinks;
    this.shouldRecordScreenViews = shouldRecordScreenViews;
    this.packageInfo = packageInfo;
  }

  public void onStop(@NonNull LifecycleOwner owner) {
    //App in background
    if (shouldTrackApplicationLifecycleEvents) {
      analytics.track("Application Backgrounded");
    }
  }

  public void onStart(@NonNull LifecycleOwner owner) {
    // App in foreground
    if (shouldTrackApplicationLifecycleEvents) {
      Properties properties = new Properties();
      if (firstLaunch.get()) {
        properties
            .putValue("version", packageInfo.versionName)
            .putValue("build", String.valueOf(packageInfo.versionCode));
      }
      properties.putValue("from_background", !firstLaunch.getAndSet(false));
      analytics.track("Application Opened", properties);
    }
  }

  public void onCreate(@NonNull LifecycleOwner owner) {
    // App created
    if (!trackedApplicationLifecycleEvents.getAndSet(true)
        && shouldTrackApplicationLifecycleEvents) {
      numberOfActivities.set(0);
      firstLaunch.set(true);
      analytics.trackApplicationLifecycleEvents();

      if (trackAttributionInformation) {
        analyticsExecutor.submit(
            new Runnable() {
              @Override
              public void run() {
                analytics.trackAttributionInformation();
              }
            });
      }
    }
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle bundle) {
    analytics.runOnMainThread(IntegrationOperation.onActivityCreated(activity, bundle));

    if (trackDeepLinks) {
      trackDeepLink(activity);
    }
  }

  private void trackDeepLink(Activity activity) {
    Intent intent = activity.getIntent();
    if (intent == null || intent.getData() == null) {
      return;
    }

    Properties properties = new Properties();
    Uri uri = intent.getData();
    for (String parameter : uri.getQueryParameterNames()) {
      String value = uri.getQueryParameter(parameter);
      if (value != null && !value.trim().isEmpty()) {
        properties.put(parameter, value);
      }
    }

    properties.put("url", uri.toString());
    analytics.track("Deep Link Opened", properties);
  }

  @Override
  public void onActivityStarted(Activity activity) {
    if (shouldRecordScreenViews) {
      analytics.recordScreenViews(activity);
    }
    analytics.runOnMainThread(IntegrationOperation.onActivityStarted(activity));
  }

  @Override
  public void onActivityResumed(Activity activity) {
    analytics.runOnMainThread(IntegrationOperation.onActivityResumed(activity));
  }

  @Override
  public void onActivityPaused(Activity activity) {
    analytics.runOnMainThread(IntegrationOperation.onActivityPaused(activity));
  }

  @Override
  public void onActivityStopped(Activity activity) {
    analytics.runOnMainThread(IntegrationOperation.onActivityStopped(activity));
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    analytics.runOnMainThread(IntegrationOperation.onActivitySaveInstanceState(activity, bundle));
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    analytics.runOnMainThread(IntegrationOperation.onActivityDestroyed(activity));
  }

  public static class Builder {
    private Analytics analytics;
    private ExecutorService analyticsExecutor;
    private Boolean shouldTrackApplicationLifecycleEvents;
    private Boolean trackAttributionInformation;
    private Boolean trackDeepLinks;
    private Boolean shouldRecordScreenViews;
    private PackageInfo packageInfo;

    public Builder() {}

    public Builder analytics(Analytics analytics) {
      this.analytics = analytics;
      return this;
    }

    Builder analyticsExecutor(ExecutorService analyticsExecutor) {
      this.analyticsExecutor = analyticsExecutor;
      return this;
    }

    Builder shouldTrackApplicationLifecycleEvents(Boolean shouldTrackApplicationLifecycleEvents) {
      this.shouldTrackApplicationLifecycleEvents = shouldTrackApplicationLifecycleEvents;
      return this;
    }

    Builder trackAttributionInformation(Boolean trackAttributionInformation) {
      this.trackAttributionInformation = trackAttributionInformation;
      return this;
    }

    Builder trackDeepLinks(Boolean trackDeepLinks) {
      this.trackDeepLinks = trackDeepLinks;
      return this;
    }

    Builder shouldRecordScreenViews(Boolean shouldRecordScreenViews) {
      this.shouldRecordScreenViews = shouldRecordScreenViews;
      return this;
    }

    Builder packageInfo(PackageInfo packageInfo) {
      this.packageInfo = packageInfo;
      return this;
    }

    public AnalyticsActivityLifecycleCallbacks build() {
      return new AnalyticsActivityLifecycleCallbacks(
          analytics,
          analyticsExecutor,
          shouldTrackApplicationLifecycleEvents,
          trackAttributionInformation,
          trackDeepLinks,
          shouldRecordScreenViews,
          packageInfo);
    }
  }
}
