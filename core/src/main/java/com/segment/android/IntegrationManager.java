package com.segment.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import java.util.LinkedList;
import java.util.List;

class IntegrationManager {
  private final List<Integration> integrations = new LinkedList<Integration>();
  private final List<String> keys = new LinkedList<String>();

  IntegrationManager() {
    try {
      Class.forName("com.amplitude.api.Amplitude");
      Integration integration = new AmplitudeIntegration();
      integrations.add(integration);
      keys.add(integration.getKey());
    } catch (ClassNotFoundException e) {
    }
  }

  List<String> keys() {
    return keys;
  }

  void initialize(Context context, ProjectSettings projectSettings) {
    Logger.v("Initializing integrations: ");
    for (Integration integration : integrations) {
      Logger.v("Initializing integration: ", integration.getKey());
      Json settings = projectSettings.getSettingsForIntegration(integration);
      Logger.v("Initializing integration %s with settings %s ", integration.getKey(), settings);
      integration.start(context, settings);
    }
  }

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

  /**
   * Called when the user identifies a user.
   *
   * @param identify An identify action
   */
  void identify(IdentifyPayload identify) {
    for (Integration integration : integrations) {
      if (integration.shouldPerformOperation(identify.getOptions())) integration.identify(identify);
    }
  }

  /**
   * Called when the user identifies a group.
   *
   * @param group A group action
   */
  void group(GroupPayload group) {

  }

  /**
   * Called when the user tracks an action.
   *
   * @param track : A track action
   */
  void track(TrackPayload track) {
    for (Integration integration : integrations) {
      if (integration.shouldPerformOperation(track.getOptions())) integration.track(track);
    }
  }

  /**
   * Called when a user aliases an action.
   *
   * @param alias An alias action
   */
  void alias(AliasPayload alias) {

  }

  /**
   * Called when a user opens up a new screen
   *
   * @param screen Screen action
   */
  void screen(ScreenPayload screen) {

  }

  /**
   * Resets the identified user in the library. Can be used when the user logs out.
   */
  void reset() {

  }

  /**
   * Opt out of analytics.
   */
  void optOut(boolean optedOut) {

  }

  /**
   * If possible, will flush all the messages from this provider
   * to their respective server endpoints.
   */
  void flush() {

  }
}
