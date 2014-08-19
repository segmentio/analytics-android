package com.segment.android.internal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import com.segment.android.Segment;
import com.segment.android.internal.integrations.AbstractIntegration;
import com.segment.android.internal.integrations.AmplitudeIntegration;
import com.segment.android.internal.integrations.InvalidConfigurationException;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.segment.android.internal.Utils.defaultSingleThreadedExecutor;

/**
 * Manages bundled integrations. This class will maintain it's own queue for events to account for
 * the latency between receiving the first event, fetching remote settings and enabling the
 * integrations. Once we enable all integrations - we'll replay any events on disk.
 * This should only affect the first app install, subsequent launches will be use a cached value
 * from disk.
 * Note that none of the activity lifecycle events are queued to disk.
 */
public class IntegrationManager {
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
  }

  void performFetch() {
    try {
      final ProjectSettings projectSettings = segmentHTTPApi.fetchSettings();
      Segment.HANDLER.post(new Runnable() {
        @Override public void run() {
          // todo : does this need to be on the main thread?
          initialize(projectSettings);
        }
      });
    } catch (IOException e) {
      Logger.e(e, "Failed to fetch settings");
      performFetch(); // todo: terminate retry
    }
  }

  private void initialize(ProjectSettings projectSettings) {
    if (projectSettings.Amplitude != null) {
      try {
        Class.forName("com.amplitude.api.Amplitude");
        AbstractIntegration integration =
            new AmplitudeIntegration(context, projectSettings.Amplitude);
        integrations.add(integration);
      } catch (ClassNotFoundException e) {
        Logger.w("Amplitude is not bundled in the app.");
      } catch (InvalidConfigurationException e) {
        Logger.e(e, "Amplitude is not bundled in the app.");
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

  public void track(TrackPayload track) {
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
