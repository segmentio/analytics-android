package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

/**
 * A base class for Integrations. An integration will only be created if the server sends us
 * settings for it.
 *
 * @param <T> The type of the backing instance. This isn't strictly necessary, but serves as
 * documentation for what type to expect.
 */
abstract class AbstractIntegrationAdapter<T> {
  static final String VIEWED_EVENT_FORMAT = "Viewed %s Screen";

  final boolean debuggingEnabled;

  AbstractIntegrationAdapter(boolean debuggingEnabled) {
    this.debuggingEnabled = debuggingEnabled;
  }

  /**
   * Initialize the integration. Implementations should wrap any errors, including missing settings
   * and permission in {@link InvalidConfigurationException}. If this method call completes without
   * an error, the integration is assumed to be initialize and ready.
   */
  abstract void initialize(Context context, JsonMap settings) throws InvalidConfigurationException;

  /**
   * The underlying instance for this provider - used for integration specific actions. This will
   * be null for SDK's that maintain a shared instance (e.g. Amplitude).
   */
  T getUnderlyingInstance() {
    // Only Mixpanel and GoogleAnalytics don't have shared instances so no need to make all
    // integrations include this.
    return null;
  }

  abstract String key();

  // Application Callbacks, same as Application$ActivityLifecycleCallbacks
  void onActivityCreated(Activity activity, Bundle savedInstanceState) {

  }

  void onActivityStarted(Activity activity) {

  }

  void onActivityResumed(Activity activity) {
  }

  void onActivityPaused(Activity activity) {
  }

  void onActivityStopped(Activity activity) {
  }

  void onActivitySaveInstanceState(Activity activity, Bundle outState) {
  }

  void onActivityDestroyed(Activity activity) {
  }

  // Analytics Actions
  void identify(IdentifyPayload identify) {
  }

  void group(GroupPayload group) {
  }

  void track(TrackPayload track) {
  }

  void alias(AliasPayload alias) {
  }

  void screen(ScreenPayload screen) {
  }

  void flush() {

  }

  /**
   * Called to indicate that the user has optedOut. If called with {@code true}true, this
   * integration won't receive any more events until this method is called with {@code false}.
   * todo: this isn't exposed to the public API yet
   */
  void optOut(boolean optOut) {

  }
}
