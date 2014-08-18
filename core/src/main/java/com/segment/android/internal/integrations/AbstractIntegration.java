package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;

/**
 * A base class for Integrations. An integration will only be created if the server sends us
 * settings for it.
 */
public abstract class AbstractIntegration<T> {
  private final Context context;

  /**
   * Create an integration with the given settings. Check for any specific permissions or features
   * that the integration needs. Also check for any required values in your settings.
   */
  public AbstractIntegration(Context context, T settings) throws InvalidConfigurationException {
    this.context = context;
  }

  final Context getContext() {
    return context;
  }

  // Application Callbacks, same as Application$ActivityLifecycleCallbacks
  public abstract void onActivityCreated(Activity activity, Bundle savedInstanceState);

  public abstract void onActivityStarted(Activity activity);

  public abstract void onActivityResumed(Activity activity);

  public abstract void onActivityPaused(Activity activity);

  public abstract void onActivityStopped(Activity activity);

  public abstract void onActivitySaveInstanceState(Activity activity, Bundle outState);

  public abstract void onActivityDestroyed(Activity activity);

  // Analytics Actions
  public abstract void identify(IdentifyPayload identify);

  public abstract void group(GroupPayload group);

  public abstract void track(TrackPayload track);

  public abstract void alias(AliasPayload alias);

  public abstract void screen(ScreenPayload screen);
}
