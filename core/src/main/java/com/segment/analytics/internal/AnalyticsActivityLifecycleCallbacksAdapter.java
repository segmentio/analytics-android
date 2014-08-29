/*
 * Copyright 2014 Prateek Srivastava
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.segment.analytics.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import com.segment.analytics.Analytics;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class AnalyticsActivityLifecycleCallbacksAdapter
    implements Application.ActivityLifecycleCallbacks {
  final Analytics analytics;

  public static void registerActivityLifecycleCallbacks(Application application,
      Analytics analytics) {
    application.registerActivityLifecycleCallbacks(
        new AnalyticsActivityLifecycleCallbacksAdapter(analytics));
  }

  AnalyticsActivityLifecycleCallbacksAdapter(Analytics analytics) {
    this.analytics = analytics;
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    analytics.onActivityCreated(activity, savedInstanceState);
  }

  @Override public void onActivityStarted(Activity activity) {
    analytics.onActivityStarted(activity);
  }

  @Override public void onActivityResumed(Activity activity) {
    analytics.onActivityResumed(activity);
  }

  @Override public void onActivityPaused(Activity activity) {
    analytics.onActivityPaused(activity);
  }

  @Override public void onActivityStopped(Activity activity) {
    analytics.onActivityStopped(activity);
  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    analytics.onActivitySaveInstanceState(activity, outState);
  }

  @Override public void onActivityDestroyed(Activity activity) {
    analytics.onActivityDestroyed(activity);
  }
}
