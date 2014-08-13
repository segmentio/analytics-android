package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.segment.android.internal.IntegrationManager;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;

/**
 * A base class for Integrations. An integration will only be created if the server sends us
 * settings for it.
 */
public abstract class Integration {
  private final Context context;
  private final String key;
  private IntegrationManager.State state;

  /**
   * Create an integration with the given settings. Check for any specific permissions or features
   * that the integration needs. Also check for any required values in your settings.
   */
  public Integration(Context context, String key) {
    this.context = context;
    this.key = key;
    state = IntegrationManager.State.DEFAULT;
  }

  public final String getKey() {
    return key;
  }

  public final void disable() {
    state = IntegrationManager.State.DISABLED;
  }

  public final void enable() {
    state = IntegrationManager.State.ENABLED;
  }

  public final IntegrationManager.State state() {
    return state;
  }

  public abstract void initialize(JsonMap integrationSettings) throws InvalidConfigurationException;

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

  Context getContext() {
    return context;
  }
}
