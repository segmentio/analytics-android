package com.segment.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.segment.android.internal.integrations.AmplitudeIntegration;
import com.segment.android.internal.integrations.Integration;
import com.segment.android.internal.integrations.InvalidConfigurationException;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.util.Logger;
import com.segment.android.internal.util.Utils;
import com.segment.android.json.JsonMap;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class IntegrationManager {
  static final int REQUEST_FETCH = 1;

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

  private static final Map<String, BundledProvider> providers;

  static {
    // A Map of all providers that can be bundled.
    providers = new LinkedHashMap<String, BundledProvider>();
    providers.put("AMPLITUDE", BundledProvider.AMPLITUDE);
    providers.put("BUGSNAG", BundledProvider.BUGSNAG);
    providers.put("COUNTLY", BundledProvider.COUNTLY);
    providers.put("CRITTERCISM", BundledProvider.CRITTERCISM);
    providers.put("FLURRY", BundledProvider.FLURRY);
    providers.put("GOOGLE_ANALYTICS", BundledProvider.GOOGLE_ANALYTICS);
    providers.put("LOCALYTICS", BundledProvider.LOCALYTICS);
    providers.put("MIXPANEL", BundledProvider.MIXPANEL);
    providers.put("QUANTCAST", BundledProvider.QUANTCAST);
    providers.put("TAPSTREAM", BundledProvider.TAPSTREAM);
  }

  private final List<Integration> integrations = new LinkedList<Integration>();
  final Context context;
  final SegmentHTTPApi segmentHTTPApi;
  final Handler mainThreadHandler;
  final FetcherThread fetcherThread;
  final FetcherHandler fetcherHandler;

  static IntegrationManager create(Context context, Handler mainThreadHandler,
      SegmentHTTPApi segmentHTTPApi) {
    return new IntegrationManager(context, mainThreadHandler, segmentHTTPApi);
  }

  IntegrationManager(Context context, Handler mainThreadHandler, SegmentHTTPApi segmentHTTPApi) {
    this.context = context;
    this.segmentHTTPApi = segmentHTTPApi;
    this.mainThreadHandler = mainThreadHandler;
    this.fetcherThread = new FetcherThread();
    this.fetcherThread.start();
    this.fetcherHandler = new FetcherHandler(fetcherThread.getLooper(), this);

    dispatchFetch();
  }

  void dispatchFetch() {
    fetcherHandler.sendMessage(fetcherHandler.obtainMessage(REQUEST_FETCH));
  }

  void performFetch() {
    try {
      final JsonMap jsonMap = segmentHTTPApi.fetchSettings();
      Segment.HANDLER.post(new Runnable() {
        @Override public void run() {
          Logger.d("Fetched %s", jsonMap.toString());
        }
      });
    } catch (IOException e) {
      Logger.e(e, "Failed to fetch settings");
      Segment.HANDLER.post(new Runnable() {
        @Override public void run() {
          // retry
          dispatchFetch();
        }
      });
    }
  }

  private static final String FETCHER_THREAD_NAME = "Fetcher";

  static class FetcherThread extends HandlerThread {
    FetcherThread() {
      super(Utils.THREAD_PREFIX + FETCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    }
  }

  static class FetcherHandler extends Handler {
    private final IntegrationManager integrationManager;

    FetcherHandler(Looper looper, IntegrationManager integrationManager) {
      super(looper);
      this.integrationManager = integrationManager;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_FETCH: {
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

  private void initialize(JsonMap projectSettings) {
    // todo: SLOWWWWWW. Probably not, but profile this and optimise it if needed
    for (String key : projectSettings.keySet()) {
      BundledProvider provider = providers.get(key);
      if (provider == null) continue;

      JsonMap integrationSettings = projectSettings.getJsonMap(key);
      switch (provider) {
        case AMPLITUDE:
          try {
            Class.forName("com.amplitude.api.Amplitude");
            Integration integration = new AmplitudeIntegration(context, integrationSettings);
            integrations.add(integration);
          } catch (ClassNotFoundException e) {
            Logger.w("Amplitude is not bundled in the app.");
          } catch (InvalidConfigurationException e) {
            Logger.e(e, "Could not initialize Amplitude's SDK.");
          }
          break;
        case BUGSNAG:
        case COUNTLY:
        case CRITTERCISM:
        case FLURRY:
        case GOOGLE_ANALYTICS:
        case LOCALYTICS:
        case MIXPANEL:
        case QUANTCAST:
        case TAPSTREAM:
        default:
          throw new IllegalArgumentException("provider is not available as a bundled integration");
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
