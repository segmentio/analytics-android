package com.segment.android;

import android.app.Activity;
import android.os.Bundle;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;

public interface Analytics {
  // @formatter:off
  // Application Callbacks, same as Application$ActivityLifecycleCallbacks
  void onActivityCreated(Activity activity, Bundle savedInstanceState);
  void onActivityStarted(Activity activity);
  void onActivityResumed(Activity activity);
  void onActivityPaused(Activity activity);
  void onActivityStopped(Activity activity);
  void onActivitySaveInstanceState(Activity activity, Bundle outState);
  void onActivityDestroyed(Activity activity);

  // Analytics Actions
  void identify(IdentifyPayload identify);
  void group(GroupPayload group);
  void track(TrackPayload track);
  void alias(AliasPayload alias);
  void screen(ScreenPayload screen);
  // @formatter:on
}
