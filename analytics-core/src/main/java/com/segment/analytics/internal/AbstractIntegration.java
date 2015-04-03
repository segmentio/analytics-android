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

import static com.segment.analytics.Analytics.LogLevel;

/**
 * A base class for all bundled integrations.
 *
 * @param <T> The type of the backing instance. This isn't strictly necessary (since we return an
 * object), but serves as documentation for what type to expect with
 * {@link #getUnderlyingInstance()}.
 */
public abstract class AbstractIntegration<T> {
  protected static final String VIEWED_EVENT_FORMAT = "Viewed %s Screen";

  /**
   * Initialize the integration. Implementations should wrap any errors, including missing settings
   * and permission in {@link IllegalStateException}. If this method call completes without an
   * error, the integration is assumed to be initialized and ready to except events.
   */
  public abstract void initialize(Context context, ValueMap settings, LogLevel logLevel)
      throws IllegalStateException;

  /**
   * The underlying instance for this provider - used for integration specific actions. This will
   * return {@code null} for SDK's that maintain a shared instance (e.g. Amplitude).
   */
  public T getUnderlyingInstance() {
    return null;
  }

  /** A key to identify this integration, matching the one in the Segment Public API. */
  public abstract String key();

  // Application Callbacks, same as Application$ActivityLifecycleCallbacks
  public boolean onActivityCreated(Activity activity, Bundle savedInstanceState) {
    return false;
  }

  public boolean onActivityStarted(Activity activity) {
    return false;
  }

  public boolean onActivityResumed(Activity activity) {
    return false;
  }

  public boolean onActivityPaused(Activity activity) {
    return false;
  }

  public boolean onActivityStopped(Activity activity) {
    return false;
  }

  public boolean onActivitySaveInstanceState(Activity activity, Bundle outState) {
    return false;
  }

  public boolean onActivityDestroyed(Activity activity) {
    return false;
  }

  // Analytics Actions
  public boolean identify(IdentifyPayload identify) {
    return false;
  }

  public boolean group(GroupPayload group) {
    return false;
  }

  public boolean track(TrackPayload track) {
    return false;
  }

  public boolean alias(AliasPayload alias) {
    return false;
  }

  public boolean screen(ScreenPayload screen) {
    return false;
  }

  public boolean flush() {
    return false;
  }
}
