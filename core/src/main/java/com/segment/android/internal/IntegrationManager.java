package com.segment.android.internal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import com.google.gson.Gson;
import com.segment.android.Segment;
import com.segment.android.internal.integrations.AbstractIntegration;
import com.segment.android.internal.integrations.AmplitudeIntegration;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.util.Logger;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.segment.android.internal.util.Utils.defaultSingleThreadedExecutor;

public class IntegrationManager {
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

  private final List<AbstractIntegration> integrations = new LinkedList<AbstractIntegration>();
  final Context context;
  final SegmentHTTPApi segmentHTTPApi;
  final Handler mainThreadHandler;
  final ExecutorService service;

  public static IntegrationManager create(Context context, Handler mainThreadHandler,
      SegmentHTTPApi segmentHTTPApi) {
    ExecutorService service = defaultSingleThreadedExecutor();
    return new IntegrationManager(context, mainThreadHandler, segmentHTTPApi, service);
  }

  IntegrationManager(Context context, Handler mainThreadHandler, SegmentHTTPApi segmentHTTPApi,
      ExecutorService service) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.mainThreadHandler = mainThreadHandler;
    this.service = service;

    service.submit(new Runnable() {
      @Override public void run() {
        performFetch();
      }
    });

    try {
      Class.forName("com.amplitude.api.Amplitude");
      AbstractIntegration integration = new AmplitudeIntegration(context);
      integrations.add(integration);
    } catch (ClassNotFoundException e) {
      Logger.w("Amplitude is not bundled in the app.");
    }
  }

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
      performFetch(); // todo: terminate retry
    }
  }

  private void initialize(ProjectSettings projectSettings) {
    Logger.d("Initializing with settings %s", new Gson().toJson(projectSettings));
    for (AbstractIntegration integration : integrations) {
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
    for (AbstractIntegration integration : integrations) {
      integration.onActivityCreated(activity, savedInstanceState);
    }
  }

  void onActivityStarted(Activity activity) {
    for (AbstractIntegration integration : integrations) {
      integration.onActivityStarted(activity);
    }
  }

  void onActivityResumed(Activity activity) {
    for (AbstractIntegration integration : integrations) {
      integration.onActivityResumed(activity);
    }
  }

  void onActivityPaused(Activity activity) {
    for (AbstractIntegration integration : integrations) {
      integration.onActivityPaused(activity);
    }
  }

  void onActivityStopped(Activity activity) {
    for (AbstractIntegration integration : integrations) {
      integration.onActivityStopped(activity);
    }
  }

  void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    for (AbstractIntegration integration : integrations) {
      integration.onActivitySaveInstanceState(activity, outState);
    }
  }

  void onActivityDestroyed(Activity activity) {
    for (AbstractIntegration integration : integrations) {
      integration.onActivityDestroyed(activity);
    }
  }

  // Analytics Actions
  void identify(IdentifyPayload identify) {
    for (AbstractIntegration integration : integrations) {
      integration.identify(identify);
    }
  }

  void group(GroupPayload group) {
    for (AbstractIntegration integration : integrations) {
      integration.group(group);
    }
  }

  void track(TrackPayload track) {
    for (AbstractIntegration integration : integrations) {
      integration.track(track);
    }
  }

  void alias(AliasPayload alias) {
    for (AbstractIntegration integration : integrations) {
      integration.alias(alias);
    }
  }

  void screen(ScreenPayload screen) {
    for (AbstractIntegration integration : integrations) {
      integration.screen(screen);
    }
  }
}
