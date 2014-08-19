package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.settings.ProjectSettings;

/**
 * A base class for Integrations. An integration will only be created if the server sends us
 * settings for it.
 *
 * @param <T> The type of the backing instance
 */
public abstract class AbstractIntegration<T> {
  private final String key;

  /**
   * Create an integration with the given settings.
   */
  AbstractIntegration(String key, String className) throws ClassNotFoundException {
    this.key = key;
    Class.forName(className);
  }

  public final String key() {
    return key;
  }

  /** Check for any specific permissions or features that the integration needs. */
  public abstract void validate(Context context) throws InvalidConfigurationException;

  /**
   * Initialize the integration, if an error occurs here, it's probably our fault for not having all
   * the settings.
   */
  public abstract boolean initialize(Context context, ProjectSettings projectSettings)
      throws InvalidConfigurationException;

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
