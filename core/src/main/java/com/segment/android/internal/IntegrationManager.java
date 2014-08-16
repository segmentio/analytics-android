package com.segment.android.internal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.segment.android.Segment;
import com.segment.android.internal.integrations.AmplitudeIntegration;
import com.segment.android.internal.integrations.Integration;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.util.Logger;
import com.segment.android.internal.util.Utils;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class IntegrationManager {
  static final int REQUEST_FETCH_SETTINGS = 1;

  public enum State {
    // Default state, has not been initialized from the server
    DEFAULT,
    // Integration is disabled - either the user has not enabled it, or it could not be enabled due
    // to missing settings/permissions
    DISABLED,
    // Integration is ready to receive events
    ENABLED
  }

  @SuppressWarnings("SpellCheckingInspection")
  public enum BundledProvider {
    AMPLITUDE,
    BUGSNAG,
    COUNTLY,
    CRITTERCISM,
    FLURRY,
    GOOGLE_ANALYTICS,
    LOCALYTICS,
    MIXPANEL,
    QUANTCAST,
    TAPSTREAM
  }

  private final List<Integration> integrations = new LinkedList<Integration>();
  final Context context;
  final SegmentHTTPApi segmentHTTPApi;
  final Handler mainThreadHandler;
  final IntegrationManagerThread integrationManagerThread;
  final IntegrationManagerHandler integrationManagerThreadHandler;

  public static IntegrationManager create(Context context, Handler mainThreadHandler,
      SegmentHTTPApi segmentHTTPApi) {
    return new IntegrationManager(context, mainThreadHandler, segmentHTTPApi);
  }

  IntegrationManager(Context context, Handler mainThreadHandler, SegmentHTTPApi segmentHTTPApi) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.mainThreadHandler = mainThreadHandler;
    this.integrationManagerThread = new IntegrationManagerThread();
    this.integrationManagerThread.start();
    this.integrationManagerThreadHandler =
        new IntegrationManagerHandler(integrationManagerThread.getLooper(), this);

    // todo: check cache value
    dispatchFetch();

    try {
      Class.forName("com.amplitude.api.Amplitude");
      Integration integration = new AmplitudeIntegration(context);
      integrations.add(integration);
    } catch (ClassNotFoundException e) {
      Logger.w("Amplitude is not bundled in the app.");
    }
  }

  // Dispatch Actions - These are called from the main thread and dispatched to a background thread
  void dispatchFetch() {
    integrationManagerThreadHandler.sendMessage(
        integrationManagerThreadHandler.obtainMessage(REQUEST_FETCH_SETTINGS));
  }

  // Perform Actions - These are called on a bakcground thread

  void performFetch() {
    try {
      final ProjectSettings projectSettings = segmentHTTPApi.fetchSettings();
      Segment.HANDLER.post(new Runnable() {
        @Override public void run() {
          initialize(projectSettings);
        }
      });
    } catch (IOException e) {
      Logger.e(e, "Failed to fetch settings");
      Segment.HANDLER.post(new Runnable() {
        @Override public void run() {
          dispatchFetch(); // retry
        }
      });
    }
  }

  private static final String INTEGRATION_MANAGER_THREAD_NAME = "IntegrationManager";

  static class IntegrationManagerThread extends HandlerThread {
    IntegrationManagerThread() {
      super(Utils.THREAD_PREFIX + INTEGRATION_MANAGER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    }
  }

  static class IntegrationManagerHandler extends Handler {
    private final IntegrationManager integrationManager;

    IntegrationManagerHandler(Looper looper, IntegrationManager integrationManager) {
      super(looper);
      this.integrationManager = integrationManager;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_FETCH_SETTINGS: {
          integrationManager.performFetch();
          break;
        }
        default:
          Segment.HANDLER.post(new Runnable() {
            @Override public void run() {
              throw new AssertionError("Unknown handler message received: " + msg.what);
            }
          });
      }
    }
  }

  private void initialize(ProjectSettings projectSettings) {
    for (Integration integration : integrations) {
      try {
        integration.initialize(projectSettings);
        integration.enable();
      } catch (Exception e) {
        Logger.e(e, "Could not initialize integration %s", integration.getKey());
        integration.disable();
      }
    }
  }

  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    for (Integration integration : integrations) {
      integration.onActivityCreated(activity, savedInstanceState);
    }
  }

  void onActivityStarted(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityStarted(activity);
    }
  }

  void onActivityResumed(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityResumed(activity);
    }
  }

  void onActivityPaused(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityPaused(activity);
    }
  }

  void onActivityStopped(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityStopped(activity);
    }
  }

  void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    for (Integration integration : integrations) {
      integration.onActivitySaveInstanceState(activity, outState);
    }
  }

  void onActivityDestroyed(Activity activity) {
    for (Integration integration : integrations) {
      integration.onActivityDestroyed(activity);
    }
  }

  // Analytics Actions
  void identify(IdentifyPayload identify) {
    for (Integration integration : integrations) {
      integration.identify(identify);
    }
  }

  void group(GroupPayload group) {
    for (Integration integration : integrations) {
      integration.group(group);
    }
  }

  void track(TrackPayload track) {
    for (Integration integration : integrations) {
      integration.track(track);
    }
  }

  void alias(AliasPayload alias) {
    for (Integration integration : integrations) {
      integration.alias(alias);
    }
  }

  void screen(ScreenPayload screen) {
    for (Integration integration : integrations) {
      integration.screen(screen);
    }
  }
}
