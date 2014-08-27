package com.segment.android.internal.integrations;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.segment.android.internal.Integration;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.json.JsonMap;

/**
 * A base class for Integrations. An integration will only be created if the server sends us
 * settings for it.
 *
 * @param <T> The type of the backing instance. This isn't strictly necessary, but serves as
 * documentation for what type to expect.
 */
public abstract class AbstractIntegrationAdapter<T> {
  static final String VIEWED_EVENT_FORMAT = "Viewed %s Screen";

  /**
   * Initialize the integration. Implementations should wrap any errors, including missing settings
   * and permission in {@link InvalidConfigurationException}. If this method call completes without
   * an error, the integration is assumed to be initialize and ready.
   */
  public abstract void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException;

  /**
   * The underlying instance for this provider - used for integration specific actions. This will
   * be null for SDK's that maintain a shared instance (e.g. Amplitude).
   */
  public T getUnderlyingInstance() {
    // Only Mixpanel and GoogleAnalytics don't have shared instances so no need to make all
    // integrations include this.
    return null;
  }

  public abstract Integration provider();

  // Application Callbacks, same as Application$ActivityLifecycleCallbacks
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

  }

  public void onActivityStarted(Activity activity) {

  }

  public void onActivityResumed(Activity activity) {
  }

  public void onActivityPaused(Activity activity) {
  }

  public void onActivityStopped(Activity activity) {
  }

  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
  }

  public void onActivityDestroyed(Activity activity) {
  }

  // Analytics Actions
  public void identify(IdentifyPayload identify) {
  }

  public void group(GroupPayload group) {
  }

  public void track(TrackPayload track) {
  }

  public void alias(AliasPayload alias) {
  }

  public void screen(ScreenPayload screen) {
  }

  public void flush() {

  }

  /**
   * Called to indicate that the user has optedOut. If called with {@code true}true, this
   * integration won't receive any more events until this method is called with {@code false}.
   */
  public void optOut(boolean optOut) {

  }
}
