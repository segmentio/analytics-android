package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.Cartographer;
import com.segment.analytics.internal.IntegrationOperation;
import com.segment.analytics.internal.ProjectSettings;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Analytics.OnIntegrationReadyListener;
import static com.segment.analytics.internal.Utils.OWNER_INTEGRATION_MANAGER;
import static com.segment.analytics.internal.Utils.THREAD_PREFIX;
import static com.segment.analytics.internal.Utils.VERB_DISPATCH;
import static com.segment.analytics.internal.Utils.VERB_DOWNLOAD;
import static com.segment.analytics.internal.Utils.VERB_ENQUEUE;
import static com.segment.analytics.internal.Utils.VERB_INITIALIZE;
import static com.segment.analytics.internal.Utils.VERB_SKIP;
import static com.segment.analytics.internal.Utils.buffer;
import static com.segment.analytics.internal.Utils.createDirectory;
import static com.segment.analytics.internal.Utils.debug;
import static com.segment.analytics.internal.Utils.error;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.panic;
import static com.segment.analytics.internal.Utils.print;
import static com.segment.analytics.internal.Utils.quitThread;

/**
 * The class that forwards operations from the client to integrations, including Segment. It
 * maintains it's own in-memory queue to account for the latency between receiving the first event,
 * fetching settings from the server and enabling the integrations. Once it enables all
 * integrations,it replays any events in the queue. Subsequent launches will be use the cached
 * settings on disk.
 */
class IntegrationManager {
  private static final String PROJECT_SETTINGS_CACHE_KEY_PREFIX = "project-settings-";
  private static final String INTEGRATION_MANAGER_THREAD_NAME =
      THREAD_PREFIX + OWNER_INTEGRATION_MANAGER;
  private static final long SETTINGS_REFRESH_INTERVAL = 1000 * 60 * 60 * 24; // 24 hours
  private static final long SETTINGS_RETRY_INTERVAL = 1000 * 60; // 1 minute

  final Map<String, Boolean> bundledIntegrations = new ConcurrentHashMap<>();
  final List<AbstractIntegration> integrations = new ArrayList<>();

  final Context context;
  final Client client;
  final Cartographer cartographer;
  final Stats stats;
  final ValueMap.Cache<ProjectSettings> projectSettingsCache;
  final boolean debuggingEnabled;
  final HandlerThread integrationManagerThread;
  final Handler integrationManagerHandler;

  Queue<IntegrationOperation> operationQueue;
  OnIntegrationReadyListener listener;
  boolean initialized;

  static synchronized IntegrationManager create(Context context, Cartographer cartographer,
      Client client, Stats stats, String tag, boolean debuggingEnabled) {
    ValueMap.Cache<ProjectSettings> projectSettingsCache =
        new ValueMap.Cache<>(context, cartographer, PROJECT_SETTINGS_CACHE_KEY_PREFIX + tag,
            ProjectSettings.class);
    return new IntegrationManager(context, client, cartographer, stats, projectSettingsCache,
        debuggingEnabled);
  }

  private static String getSanitizedKeyForIntegration(String integrationKey) {
    return integrationKey.replace(' ', '-').toLowerCase();
  }

  private static String getFileNameForIntegration(String integrationKey) {
    return getSanitizedKeyForIntegration(integrationKey)
        + "-3.0.0-SNAPSHOT-jar-with-dependencies.jar";
  }

  private static File getJarDownloadDirectory(Context context) {
    return context.getDir("jars", Context.MODE_PRIVATE);
  }

  IntegrationManager(Context context, Client client, Cartographer cartographer, Stats stats,
      ValueMap.Cache<ProjectSettings> projectSettingsCache, boolean debuggingEnabled) {
    this.context = context;
    this.client = client;
    this.cartographer = cartographer;
    this.stats = stats;
    this.projectSettingsCache = projectSettingsCache;
    this.debuggingEnabled = debuggingEnabled;

    integrationManagerThread =
        new HandlerThread(INTEGRATION_MANAGER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    integrationManagerThread.start();
    integrationManagerHandler =
        new IntegrationManagerHandler(integrationManagerThread.getLooper(), this);

    checkBundledIntegration("com.segment.analytics.internal.integrations.AmplitudeIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.AppsFlyerIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.BugsnagIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.CountlyIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.CrittercismIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.FlurryIntegration");
    checkBundledIntegration(
        "com.segment.analytics.internal.integrations.GoogleAnalyticsIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.KahunaIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.LeanplumIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.LocalyticsIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.MixpanelIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.QuantcastIntegration");
    checkBundledIntegration("com.segment.analytics.internal.integrations.TapstreamIntegration");

    if (projectSettingsCache.isSet() && projectSettingsCache.get() != null) {
      dispatchInitialize(projectSettingsCache.get());
      if (projectSettingsCache.get().timestamp() + SETTINGS_REFRESH_INTERVAL
          < System.currentTimeMillis()) {
        dispatchFetch();
      }
    } else {
      dispatchFetch();
    }
  }

  private void checkBundledIntegration(String className) {
    try {
      Class clazz = Class.forName(className);
      loadIntegration(clazz);
    } catch (ClassNotFoundException e) {
      if (debuggingEnabled) {
        debug(OWNER_INTEGRATION_MANAGER, VERB_SKIP, className);
      }
    }
  }

  private void loadIntegration(Class clazz) {
    try {
      Constructor<AbstractIntegration> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      AbstractIntegration integration = constructor.newInstance();
      integrations.add(integration);
      bundledIntegrations.put(integration.key(), false);
    } catch (Exception e) {
      throw panic(e, "Could not instantiate class.");
    }
  }

  void dispatchFetch() {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_FETCH_SETTINGS));
  }

  void performFetch() {
    if (!isConnected(context)) {
      retryFetch();
      return;
    }

    try {
      Client.Response response = client.fetchSettings();
      ProjectSettings projectSettings;
      try {
        projectSettings = ProjectSettings.create(cartographer.fromJson(buffer(response.is)),
            System.currentTimeMillis());
        projectSettingsCache.set(projectSettings);
      } finally {
        response.close();
      }

      File downloadedJarsDirectory = getJarDownloadDirectory(context);
      try {
        createDirectory(downloadedJarsDirectory);
      } catch (IOException e) {
        if (debuggingEnabled) {
          error(OWNER_INTEGRATION_MANAGER, VERB_DOWNLOAD, null, e,
              "Unable to download integrations into " + downloadedJarsDirectory);
        }
        return;
      }

      Set<String> bundledIntegrationsKeys = bundledIntegrations.keySet();
      HashSet<String> skippedIntegrations = new HashSet<>(bundledIntegrationsKeys.size() + 2);
      skippedIntegrations.addAll(bundledIntegrationsKeys);
      skippedIntegrations.add(ProjectSettings.SEGMENT_KEY);
      skippedIntegrations.add(ProjectSettings.TIMESTAMP_KEY);

      for (String key : projectSettings.keySet()) {
        if (skippedIntegrations.contains(key)) continue;

        String fileName = getFileNameForIntegration(key);
        String url = "https://dl.dropboxusercontent.com/u/11371156/integrations/" + fileName;
        client.downloadFile(url, new File(downloadedJarsDirectory, fileName));
      }

      if (!initialized) dispatchInitialize(projectSettings);
    } catch (IOException e) {
      retryFetch();
    }
  }

  void retryFetch() {
    integrationManagerHandler.sendMessageDelayed(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_FETCH_SETTINGS), SETTINGS_RETRY_INTERVAL);
  }

  void dispatchInitialize(ProjectSettings projectSettings) {
    integrationManagerHandler.sendMessageAtFrontOfQueue(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_INITIALIZE, projectSettings));
  }

  private void loadDownloadedIntegrations(ProjectSettings settings) {
    File downloadedJarsDirectory = getJarDownloadDirectory(context);
    File optimizedDexDirectory = context.getDir("optimized-dex", Context.MODE_PRIVATE);
    try {
      createDirectory(optimizedDexDirectory);
    } catch (IOException e) {
      if (debuggingEnabled) {
        print(e, "Unable to dex downloaded integrations");
      }
      return;
    }
    ClassLoader defaultClassLoader = context.getClassLoader();

    Set<String> bundledIntegrationsKeys = bundledIntegrations.keySet();
    HashSet<String> skippedIntegrations = new HashSet<>(bundledIntegrationsKeys.size() + 2);
    skippedIntegrations.addAll(bundledIntegrationsKeys);
    skippedIntegrations.add(ProjectSettings.SEGMENT_KEY);
    skippedIntegrations.add(ProjectSettings.TIMESTAMP_KEY);

    for (String key : settings.keySet()) {
      if (skippedIntegrations.contains(key)) continue; // skip bundled integrations

      File jarFile = new File(downloadedJarsDirectory, getFileNameForIntegration(key));

      DexClassLoader dexClassLoader =
          new DexClassLoader(jarFile.getAbsolutePath(), optimizedDexDirectory.getAbsolutePath(),
              null, defaultClassLoader);
      String className =
          "com.segment.analytics.internal.integrations." + key.replace(" ", "") + "Integration";
      try {
        Class integrationClass = dexClassLoader.loadClass(className);
        loadIntegration(integrationClass);
      } catch (ClassNotFoundException e) {
        if (debuggingEnabled) {
          error(OWNER_INTEGRATION_MANAGER, VERB_SKIP, className, e);
        }
      }
    }
  }

  void performInitialize(ProjectSettings projectSettings) {
    if (initialized) return;

    loadDownloadedIntegrations(projectSettings);

    Iterator<AbstractIntegration> iterator = integrations.iterator();
    while (iterator.hasNext()) {
      AbstractIntegration integration = iterator.next();
      String key = integration.key();
      if (projectSettings.containsKey(key)) {
        ValueMap settings = projectSettings.getValueMap(key);
        try {
          if (debuggingEnabled) {
            debug(OWNER_INTEGRATION_MANAGER, VERB_INITIALIZE, key, settings);
          }
          integration.initialize(context, settings, debuggingEnabled);
          if (listener != null) {
            listener.onIntegrationReady(key, integration.getUnderlyingInstance());
            listener = null; // clear the reference
          }
        } catch (IllegalStateException e) {
          if (debuggingEnabled) {
            error(OWNER_INTEGRATION_MANAGER, VERB_SKIP, key, e, settings);
          }
          iterator.remove();
          bundledIntegrations.remove(key);
        }
      } else {
        iterator.remove();
      }
    }

    if (!isNullOrEmpty(operationQueue)) {
      Iterator<IntegrationOperation> operationIterator = operationQueue.iterator();
      while (operationIterator.hasNext()) {
        IntegrationOperation operation = operationIterator.next();
        run(operation);
        operationIterator.remove();
      }
    }
    operationQueue = null;
    initialized = true;
  }

  void dispatchFlush() {
    dispatchOperation(new FlushOperation());
  }

  void dispatchOperation(IntegrationOperation operation) {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_DISPATCH_OPERATION, operation));
  }

  void performOperation(IntegrationOperation operation) {
    if (initialized) {
      run(operation);
    } else {
      if (operationQueue == null) {
        operationQueue = new ArrayDeque<>();
      }
      if (debuggingEnabled) {
        debug(OWNER_INTEGRATION_MANAGER, VERB_ENQUEUE, operation.id());
      }
      operationQueue.add(operation);
    }
  }

  /** Runs the given operation on all Bundled integrations. */
  private void run(IntegrationOperation operation) {
    for (int i = 0; i < integrations.size(); i++) {
      AbstractIntegration integration = integrations.get(i);
      long startTime = System.nanoTime();
      operation.run(integration);
      long endTime = System.nanoTime();
      long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
      if (debuggingEnabled) {
        debug(OWNER_INTEGRATION_MANAGER, VERB_DISPATCH, operation.id(), integration.key(),
            TimeUnit.NANOSECONDS.toMillis(duration) + "ms");
      }
      stats.dispatchIntegrationOperation(integration.key(), duration);
    }
  }

  void dispatchRegisterIntegrationInitializedListener(OnIntegrationReadyListener listener) {
    integrationManagerHandler.sendMessage(integrationManagerHandler //
        .obtainMessage(IntegrationManagerHandler.REQUEST_REGISTER_LISTENER, listener));
  }

  void performRegisterIntegrationInitializedListener(OnIntegrationReadyListener listener) {
    if (initialized && listener != null) {
      // Integrations are already ready, notify the listener right away
      for (AbstractIntegration abstractIntegration : integrations) {
        listener.onIntegrationReady(abstractIntegration.key(),
            abstractIntegration.getUnderlyingInstance());
      }
    } else {
      this.listener = listener;
    }
  }

  void shutdown() {
    quitThread(integrationManagerThread);
    if (operationQueue != null) {
      operationQueue.clear();
      operationQueue = null;
    }
  }

  static class ActivityLifecyclePayload implements IntegrationOperation {
    final Type type;
    final Bundle bundle;
    final Activity activity;
    final String id;

    ActivityLifecyclePayload(Type type, Activity activity, Bundle bundle) {
      this.type = type;
      this.bundle = bundle;
      this.id = UUID.randomUUID().toString();
      // Ideally we would store a weak reference, but it doesn't work for stop/destroy events
      this.activity = activity;
    }

    Activity getActivity() {
      return activity;
    }

    @Override public void run(AbstractIntegration integration) {
      switch (type) {
        case CREATED:
          integration.onActivityCreated(activity, bundle);
          break;
        case STARTED:
          integration.onActivityStarted(activity);
          break;
        case RESUMED:
          integration.onActivityResumed(activity);
          break;
        case PAUSED:
          integration.onActivityPaused(activity);
          break;
        case STOPPED:
          integration.onActivityStopped(activity);
          break;
        case SAVE_INSTANCE:
          integration.onActivitySaveInstanceState(activity, bundle);
          break;
        case DESTROYED:
          integration.onActivityDestroyed(activity);
          break;
        default:
          panic("Unknown lifecycle event type: " + type);
      }
    }

    @Override public String id() {
      return id;
    }

    @Override public String toString() {
      return "ActivityLifecycle{" + type + '}';
    }

    enum Type {
      CREATED, STARTED, RESUMED, PAUSED, STOPPED, SAVE_INSTANCE, DESTROYED
    }
  }

  static class FlushOperation implements IntegrationOperation {
    String id;

    @Override public void run(AbstractIntegration integration) {
      integration.flush();
    }

    @Override public synchronized String id() {
      if (id == null) {
        id = UUID.randomUUID().toString();
      }
      return id;
    }

    @Override public String toString() {
      return getClass().getCanonicalName();
    }
  }

  private static class IntegrationManagerHandler extends Handler {
    private static final int REQUEST_FETCH_SETTINGS = 1;
    private static final int REQUEST_INITIALIZE = 2;
    private static final int REQUEST_DISPATCH_OPERATION = 3;
    private static final int REQUEST_REGISTER_LISTENER = 4;
    private final IntegrationManager integrationManager;

    IntegrationManagerHandler(Looper looper, IntegrationManager integrationManager) {
      super(looper);
      this.integrationManager = integrationManager;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_FETCH_SETTINGS:
          integrationManager.performFetch();
          break;
        case REQUEST_INITIALIZE:
          integrationManager.performInitialize((ProjectSettings) msg.obj);
          break;
        case REQUEST_DISPATCH_OPERATION:
          integrationManager.performOperation((IntegrationOperation) msg.obj);
          break;
        case REQUEST_REGISTER_LISTENER:
          integrationManager //
              .performRegisterIntegrationInitializedListener((OnIntegrationReadyListener) msg.obj);
          break;
        default:
          panic("Unhandled dispatcher message: " + msg.what);
      }
    }
  }
}
