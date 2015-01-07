package com.segment.analytics.internal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.GroupPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

/**
 * A base class for Integrations. An integration will only be created if the server sends us
 * settings for it. <p></p> This could exist as an interface, but we'll want an abstract class
 * anyway since not all integrations require all methods to be implemented.
 *
 * @param <T> The type of the backing instance. This isn't strictly necessary, but serves as
 *            documentation for what type to expect with {@link #getUnderlyingInstance()}
 */
public abstract class AbstractIntegration<T> {
  public static final String VIEWED_EVENT_FORMAT = "Viewed %s Screen";

  /**
   * Initialize the integration. Implementations should wrap any errors, including missing settings
   * and permission in {@link IllegalStateException}. If this method call completes without an
   * error, the integration is assumed to be initialize and ready.
   */
  public abstract void initialize(Context context, ValueMap settings, boolean debuggingEnabled)
      throws IllegalStateException;

  /**
   * The underlying instance for this provider - used for integration specific actions. This will be
   * null for SDK's that maintain a shared instance (e.g. Amplitude).
   */
  public T getUnderlyingInstance() {
    // Only Mixpanel and GoogleAnalytics don't have shared instances so no need to make all
    // integrations include this.
    return null;
  }

  public abstract String key();

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
}
