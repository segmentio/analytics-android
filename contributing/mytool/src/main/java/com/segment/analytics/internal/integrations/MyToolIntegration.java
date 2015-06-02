package com.segment.analytics.internal.integrations;

import android.app.Activity;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;

/**
 * MyTool is a ...
 *
 * @see <a href="MyTool Website URL">MyTool</a>
 * @see <a href="MyTool Segment Integration URL">MyTool Integration</a>
 * @see <a href="MyTool Android SDK URL">MyTool Android SDK</a>
 */
public class MyToolIntegration extends AbstractIntegration<Void> {

  static final String MYTOOL_KEY = "MyTool";

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    // todo: Initialize the MyTool SDK here
  }

  @Override public String key() {
    return MYTOOL_KEY;
  }

  @Override public Void getUnderlyingInstance() {
    // TODO: If your tool doesn't use a singleton, return the SDK instance here and change the
    // return type of this method and the generic type of this class. See the Mixpanel integration
    // for an example.
    // If you tool uses a singleton with static class methods, remove this method entirely.
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);

    // TODO: Record the given user and their traits.
    // If your tool doesn't need this method, remove it entirely.
  }

  @Override public void group(GroupPayload group) {
    super.group(group);

    // TODO: Record the given group and it's traits.
    // If your tool doesn't need this method, remove it entirely.
  }

  @Override public void track(TrackPayload track) {
    super.track(track);

    // TODO: Record an event and related properties.
    // If your tool doesn't need this method, remove it entirely.
  }

  @Override public void alias(AliasPayload alias) {
    super.alias(alias);

    // TODO: Merge two user identities.
    // If your tool doesn't need this method, remove it entirely.
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);

    // TODO: Record a screen event and properties.
    // If your tool doesn't need this method, remove it entirely.
  }

  @Override public void flush() {
    super.flush();

    // TODO: Trigger an upload of queued events to your server.
    // If your tool doesn't need this method, remove it entirely.
  }

  @Override public void reset() {
    super.reset();

    // TODO: Clear any data saved about the user, such as cached user ids, etc.
    // If your tool doesn't need this method, remove it entirely.
  }

  /**
   * The following methods are same as Application.ActivityLifecycleCallbacks in
   * http://developer.android.com/reference/android/app/Application.ActivityLifecycleCallbacks.html.
   * If your tool needs to listen to the application lifecycle, you can do so here. If not, then
   * don't implement these methods and remove them from this file.
   */

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    super.onActivityCreated(activity, savedInstanceState);
    // TODO: Call your SDKs onActivityCreated method
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    // TODO: Call your SDKs onActivityStarted method
  }

  @Override public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    // TODO: Call your SDKs onActivityStarted method
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    // TODO: Call your SDKs onActivityPaused method
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    // TODO: Call your SDKs onActivityStopped method
  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    super.onActivitySaveInstanceState(activity, outState);
    // TODO: Call your SDKs onActivitySaveInstanceState method
  }

  @Override public void onActivityDestroyed(Activity activity) {
    super.onActivityDestroyed(activity);
    // TODO: Call your SDKs onActivityDestroyed method
  }
}
