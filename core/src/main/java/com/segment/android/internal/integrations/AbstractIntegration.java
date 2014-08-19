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
 *
 * @param <S> The type of the settings class to provide this integration.
 * @param <T> The backing intance
 */
public abstract class AbstractIntegration<S, T> {
  private final String key;
  private final Context context;
  private final S settings;

  /**
   * Create an integration with the given settings. Check for any specific permissions or features
   * that the integration needs. Also check for any required values in your settings.
   */
  AbstractIntegration(String key, Context context, S settings)
      throws InvalidConfigurationException {
    this.key = key;
    this.context = context;
    this.settings = settings;
  }

  final Context getContext() {
    return context;
  }

  final S getSettings() {
    return settings;
  }

  final String getKey() {
    return key;
  }

  /**
   * The underlying instance for this provider - used for integration specific actions. This could
   * be null for SDK's that maintain a shared instance (e.g. Amplitude).
   */
  public abstract T getUnderlyingInstance();

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
