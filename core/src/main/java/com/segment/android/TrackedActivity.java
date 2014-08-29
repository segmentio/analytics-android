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

package com.segment.android;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

/**
 * This is a base activity that will track all activity lifecycle events.
 * <p/>
 * You should only use this class if you are targeting devices running pre-ICS. If the device is
 * running ICS or higher, this class will do nothing, since we automatically register {@link
 * android.app.Application.ActivityLifecycleCallbacks} to track the lifecycle instead.
 * <p/>
 * Subclasses may override {@link #getSegmentInstance()} to provide their own instance of {@link
 * Segment}. It uses the singleton instance by default.
 */
public class TrackedActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (shouldTrackLifecycleEvent()) {
      getSegmentInstance().onActivityCreated(this, savedInstanceState);
    }
  }

  @Override protected void onStart() {
    super.onStart();
    if (shouldTrackLifecycleEvent()) getSegmentInstance().onActivityStarted(this);
  }

  @Override protected void onResume() {
    super.onResume();
    if (shouldTrackLifecycleEvent()) getSegmentInstance().onActivityResumed(this);
  }

  @Override protected void onPause() {
    super.onPause();
    if (shouldTrackLifecycleEvent()) getSegmentInstance().onActivityPaused(this);
  }

  @Override protected void onStop() {
    super.onStop();
    if (shouldTrackLifecycleEvent()) getSegmentInstance().onActivityStopped(this);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (shouldTrackLifecycleEvent()) {
      getSegmentInstance().onActivitySaveInstanceState(this, outState);
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (shouldTrackLifecycleEvent()) getSegmentInstance().onActivityDestroyed(this);
  }

  /**
   * Subclasses may override this to provide their own instance of {@link Segment}. It uses the
   * singleton instance by default.
   */
  protected Segment getSegmentInstance() {
    return Segment.with(this);
  }

  boolean shouldTrackLifecycleEvent() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;
  }
}
