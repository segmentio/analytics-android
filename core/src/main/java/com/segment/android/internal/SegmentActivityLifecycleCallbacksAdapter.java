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

package com.segment.android.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import com.segment.android.Segment;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class SegmentActivityLifecycleCallbacksAdapter
    implements Application.ActivityLifecycleCallbacks {
  final Segment segment;

  public static void registerActivityLifecycleCallbacks(Application application, Segment segment) {
    application.registerActivityLifecycleCallbacks(
        new SegmentActivityLifecycleCallbacksAdapter(segment));
  }

  SegmentActivityLifecycleCallbacksAdapter(Segment segment) {
    this.segment = segment;
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    segment.onActivityCreated(activity, savedInstanceState);
  }

  @Override public void onActivityStarted(Activity activity) {
    segment.onActivityStarted(activity);
  }

  @Override public void onActivityResumed(Activity activity) {
    segment.onActivityResumed(activity);
  }

  @Override public void onActivityPaused(Activity activity) {
    segment.onActivityPaused(activity);
  }

  @Override public void onActivityStopped(Activity activity) {
    segment.onActivityStopped(activity);
  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    segment.onActivitySaveInstanceState(activity, outState);
  }

  @Override public void onActivityDestroyed(Activity activity) {
    segment.onActivityDestroyed(activity);
  }
}
