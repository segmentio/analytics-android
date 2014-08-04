package com.segment.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import java.util.Map;

import static com.segment.android.Options.ALL_INTEGRATIONS_KEY;

abstract class Integration {

  enum State {
    /** Starting state, when we haven't fetched settings from the server. */
    DEFAULT,
    /** Integration was disabled on the server. */
    DISABLED,
    /** Integration was enabled on the server but failed to initialize. */
    FAILED,
    /** Integration was successfully initialized on the device. */
    INITIALIZED;
  }

  public class InvalidConfigurationException extends Exception {
    InvalidConfigurationException(String detailMessage) {
      super(detailMessage);
    }

    InvalidConfigurationException(String detailMessage, Throwable throwable) {
      super(detailMessage, throwable);
    }
  }

  private State state = State.DEFAULT;

  final State getState() {
    return state;
  }

  /** Returns whether the integration has been enabled. */
  final boolean shouldPerformOperation(Options options) {
    if (state != State.INITIALIZED) {
      Logger.d("Integration (%s) not yet initialized.", getKey());
      return false;
    }

    Map<String, Boolean> integrations = options.getIntegrations();

    if (integrations.containsKey(getKey())) {
      // user has specified an option for this integration, respect this value
      return options.getIntegrations().get(getKey());
    } else {
      // user has not specified an option for this setting, so let's see what is defined for 'all'
      return options.getIntegrations().get(ALL_INTEGRATIONS_KEY);
    }
  }

  abstract String getKey();

  // Integrations Lifecycle

  /** Start the integration with the given settings. */
  final void start(Context context, Json settings) {
    if (settings == null) {
      Logger.d("No settings for bundled integration (%s). Disabled on device.", getKey());
      state = State.DISABLED;
      return;
    }
    try {
      initialize(context, settings);
      state = State.INITIALIZED;
      Logger.d("Successfully initialized bundled integration (%s).", getKey());
    } catch (InvalidConfigurationException e) {
      state = State.FAILED;
      Logger.e(e, "Failed to initialize bundled integration (%s).", getKey());
    }
  }

  /**
   * Validate the context and settings. Check for any specific permissions or features that your
   * integration needs. Users are only guaranteed to have the
   * {@link android.Manifest.permission#INTERNET}
   * permission. Also check for any required values in your settings.
   */
  abstract void initialize(Context context, Json settings) throws InvalidConfigurationException;

  // Application Callbacks, same as Application$ActivityLifecycleCallbacks

  abstract void onActivityCreated(Activity activity, Bundle savedInstanceState);

  abstract void onActivityStarted(Activity activity);

  abstract void onActivityResumed(Activity activity);

  abstract void onActivityPaused(Activity activity);

  abstract void onActivityStopped(Activity activity);

  abstract void onActivitySaveInstanceState(Activity activity, Bundle outState);

  abstract void onActivityDestroyed(Activity activity);

  // Analytics Actions

  /**
   * Called when the user identifies a user.
   *
   * @param identify An identify action
   */
  abstract void identify(IdentifyPayload identify);

  /**
   * Called when the user identifies a group.
   *
   * @param group A group action
   */
  abstract void group(GroupPayload group);

  /**
   * Called when the user tracks an action.
   *
   * @param track : A track action
   */
  abstract void track(TrackPayload track);

  /**
   * Called when a user aliases an action.
   *
   * @param alias An alias action
   */
  abstract void alias(AliasPayload alias);

  /**
   * Called when a user opens up a new screen
   *
   * @param screen Screen action
   */
  abstract void screen(ScreenPayload screen);

  /**
   * Resets the identified user in the library. Can be used when the user logs out.
   */
  abstract void reset();

  /**
   * Opt out of analytics.
   */
  abstract void optOut(boolean optedOut);

  /**
   * If possible, will flush all the messages from this provider
   * to their respective server endpoints.
   */
  abstract void flush();
}
