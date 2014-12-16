/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.analytics;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.Map;

import static com.segment.analytics.IntegrationManager.ActivityLifecyclePayload;
import static com.segment.analytics.IntegrationManager.ActivityLifecyclePayload.Type.CREATED;
import static com.segment.analytics.IntegrationManager.ActivityLifecyclePayload.Type.DESTROYED;
import static com.segment.analytics.IntegrationManager.ActivityLifecyclePayload.Type.PAUSED;
import static com.segment.analytics.IntegrationManager.ActivityLifecyclePayload.Type.RESUMED;
import static com.segment.analytics.IntegrationManager.ActivityLifecyclePayload.Type.SAVE_INSTANCE;
import static com.segment.analytics.IntegrationManager.ActivityLifecyclePayload.Type.STARTED;
import static com.segment.analytics.IntegrationManager.ActivityLifecyclePayload.Type.STOPPED;
import static com.segment.analytics.Logger.OWNER_MAIN;
import static com.segment.analytics.Logger.VERB_CREATE;
import static com.segment.analytics.Utils.checkMain;
import static com.segment.analytics.Utils.getResourceBooleanOrThrow;
import static com.segment.analytics.Utils.getResourceIntegerOrThrow;
import static com.segment.analytics.Utils.getResourceString;
import static com.segment.analytics.Utils.hasPermission;
import static com.segment.analytics.Utils.isNullOrEmpty;

/**
 * The entry point into the Analytics for Android SDK.
 * <p>
 * The idea is simple: one pipeline for all your data. Segment is the single hub to collect,
 * translate and route your data with the flip of a switch.
 * <p>
 * Analytics for Android will automatically batch events, queue them to disk, and upload it
 * periodically to Segment for you. It will also look up your project's settings (that you've
 * configured in the web interface), specifically looking up settings for bundled integrations, and
 * then initialize them for you on the user's phone, and mapping our standardized events to formats
 * they can all understand. You only need to instrument Segment once, then flip a switch to install
 * new tools.
 * <p>
 * This class is the main entry point into the client API. Use {@link
 * #with(android.content.Context)} for the global singleton instance or construct your own instance
 * with {@link Builder}.
 *
 * @see <a href="https://segment.io/">Segment.io</a>
 */
public class Analytics {
  // Resource identifiers to define options in xml
  static final String WRITE_KEY_RESOURCE_IDENTIFIER = "analytics_write_key";
  static final String QUEUE_SIZE_RESOURCE_IDENTIFIER = "analytics_queue_size";
  static final String FLUSH_INTERVAL_IDENTIFIER = "analytics_flush_interval";
  static final String DEBUGGING_RESOURCE_IDENTIFIER = "analytics_debugging";
  static final Properties EMPTY_PROPERTIES = new Properties();
  static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
        default:
          throw new AssertionError("Unknown handler message received: " + msg.what);
      }
    }
  };
  static Analytics singleton = null;
  final Application application;
  final IntegrationManager integrationManager;
  final Segment segment;
  final Stats stats;
  final Traits.Cache traitsCache;
  final AnalyticsContext analyticsContext;
  final Options defaultOptions;
  final Logger logger;
  final boolean debuggingEnabled;
  boolean shutdown;

  /**
   * The global default {@link Analytics} instance.
   * <p>
   * This instance is automatically initialized with defaults that are suitable to most
   * implementations.
   * <p>
   * If these settings do not meet the requirements of your application, you can override defaults
   * in {@code analytics.xml}, or you can construct your own instance with full control over the
   * configuration by using {@link Builder}.
   * <p>
   * By default, events are uploaded every 30 seconds, or every 20 events (whichever occurs first),
   * and debugging is disabled.
   */
  public static Analytics with(Context context) {
    if (singleton == null) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      synchronized (Analytics.class) {
        if (singleton == null) {
          String writeKey = getResourceString(context, WRITE_KEY_RESOURCE_IDENTIFIER);
          Builder builder = new Builder(context, writeKey);
          try {
            // We need the exception to be able to tell if this was not defined, or if it was
            // incorrectly defined - something we shouldn't ignore
            int queueSize = getResourceIntegerOrThrow(context, QUEUE_SIZE_RESOURCE_IDENTIFIER);
            if (queueSize <= 0) {
              throw new IllegalStateException(QUEUE_SIZE_RESOURCE_IDENTIFIER
                  + "("
                  + queueSize
                  + ") may not be zero or negative.");
            }
            builder.queueSize(queueSize);
          } catch (Resources.NotFoundException e) {
            // when queueSize is not defined in xml, we'll use a default option in the builder
          }
          try {
            // We need the exception to be able to tell if this was not defined, or if it was
            // incorrectly defined - something we shouldn't ignore
            int flushInterval = getResourceIntegerOrThrow(context, FLUSH_INTERVAL_IDENTIFIER);
            if (flushInterval < 1) {
              throw new IllegalStateException(FLUSH_INTERVAL_IDENTIFIER
                  + "("
                  + flushInterval
                  + ") may not be zero or negative.");
            }
            builder.flushInterval(flushInterval);
          } catch (Resources.NotFoundException e) {
            // when flushInterval is not defined in xml, we'll use a default option in the builder
          }
          try {
            boolean debugging = getResourceBooleanOrThrow(context, DEBUGGING_RESOURCE_IDENTIFIER);
            builder.debugging(debugging);
          } catch (Resources.NotFoundException notFoundException) {
            // when debugging is not defined in xml, we'll try to figure it out from package flags
            String packageName = context.getPackageName();
            try {
              final int flags =
                  context.getPackageManager().getApplicationInfo(packageName, 0).flags;
              boolean debugging = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
              builder.debugging(debugging);
            } catch (PackageManager.NameNotFoundException nameNotFoundException) {
              // if we still can't figure it out, we'll use the default options in the builder
            }
          }
          singleton = builder.build();
        }
      }
    }
    return singleton;
  }

  /**
   * Set the global instance returned from {@link #with}.
   * <p>
   * This method must be called before any calls to {@link #with} and may only be called once.
   *
   * @since 2.3
   */
  public static void setSingletonInstance(Analytics analytics) {
    synchronized (Analytics.class) {
      if (singleton != null) {
        throw new IllegalStateException("Singleton instance already exists.");
      }
      singleton = analytics;
    }
  }

  Analytics(Application application, IntegrationManager integrationManager, Segment segment,
      Stats stats, Traits.Cache traitsCache, AnalyticsContext analyticsContext,
      Options defaultOptions, Logger logger, boolean debuggingEnabled) {
    this.application = application;
    this.integrationManager = integrationManager;
    this.segment = segment;
    this.stats = stats;
    this.traitsCache = traitsCache;
    this.analyticsContext = analyticsContext;
    this.defaultOptions = defaultOptions;
    this.debuggingEnabled = debuggingEnabled;
    this.logger = logger;

    application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
      @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        submit(new ActivityLifecyclePayload(CREATED, activity, savedInstanceState));
      }

      @Override public void onActivityStarted(Activity activity) {
        submit(new ActivityLifecyclePayload(STARTED, activity, null));
      }

      @Override public void onActivityResumed(Activity activity) {
        submit(new ActivityLifecyclePayload(RESUMED, activity, null));
      }

      @Override public void onActivityPaused(Activity activity) {
        submit(new ActivityLifecyclePayload(PAUSED, activity, null));
      }

      @Override public void onActivityStopped(Activity activity) {
        submit(new ActivityLifecyclePayload(STOPPED, activity, null));
      }

      @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        submit(new ActivityLifecyclePayload(SAVE_INSTANCE, activity, outState));
      }

      @Override public void onActivityDestroyed(Activity activity) {
        submit(new ActivityLifecyclePayload(DESTROYED, activity, null));
      }
    });
  }

  /**
   * Returns {@code true} if debugging is enabled.
   *
   * @deprecated Use {@link #isDebugging()} instead.
   */
  public boolean isLogging() {
    return debuggingEnabled;
  }

  /**
   * Returns {@code true} if debugging is enabled.
   *
   * @since 2.3
   */
  public boolean isDebugging() {
    return debuggingEnabled;
  }

  /** @see #identify(String, Traits, Options) */
  public void identify(String userId) {
    identify(userId, null, null);
  }

  /** @see #identify(String, Traits, Options) */
  public void identify(Traits traits) {
    identify(null, traits, null);
  }

  /**
   * Identify lets you tie one of your users and their actions to a recognizable {@code userId}. It
   * also lets you record {@code traits} about the user, like their email, name, account type, etc.
   * <p>
   * Traits and userId will be automatically cached and available on future sessions for the same
   * user. To update a trait on the server, simply call identify with the same user id (or null).
   * You can also use {@link #identify(Traits)} for this purpose.
   *
   * @param userId Unique identifier which you recognize a user by in your own database. If this is
   * null or empty, any previous id we have (could be the anonymous id) will be
   * used.
   * @param newTraits Traits about the user
   * @param options To configure the call
   * @throws IllegalArgumentException if userId is null or an empty string
   * @see <a href="https://segment.io/docs/tracking-api/identify/">Identify Documentation</a>
   */
  public void identify(String userId, Traits newTraits, Options options) {
    if (!isNullOrEmpty(userId)) {
      traitsCache.get().putUserId(userId);
    }
    if (options == null) {
      options = defaultOptions;
    }
    if (!isNullOrEmpty(newTraits)) {
      Traits traits = traitsCache.get();
      traits.putAll(newTraits);
      traitsCache.set(traits);
      analyticsContext.setTraits(traits);
    }

    BasePayload payload = new IdentifyPayload(analyticsContext, options, traitsCache.get());
    submit(payload);
  }

  /** @see #group(String, String, Traits, Options) */
  public void group(String groupId) {
    group(null, groupId, null, null);
  }

  /**
   * The group method lets you associate a user with a group. It also lets you record custom traits
   * about the group, like industry or number of employees.
   * <p>
   * If you've called {@link #identify(String, Traits, Options)} before, this will automatically
   * remember the userId. If not, it will fall back to use the anonymousId instead.
   *
   * @param userId To match up a user with their associated group.
   * @param groupId Unique identifier which you recognize a group by in your own database. Must not
   * be null or empty.
   * @param options To configure the call
   * @throws IllegalArgumentException if groupId is null or an empty string
   * @see <a href="https://segment.io/docs/tracking-api/group/">Group Documentation</a>
   */
  public void group(String userId, String groupId, Traits groupTraits, Options options) {
    if (isNullOrEmpty(groupId)) {
      throw new IllegalArgumentException("groupId must not be null or empty.");
    }
    if (isNullOrEmpty(userId)) {
      userId = traitsCache.get().userId();
    }
    if (!isNullOrEmpty(groupTraits)) {
      Traits traits = traitsCache.get();
      traits.putAll(groupTraits);
      traitsCache.set(traits);
      analyticsContext.setTraits(traits);
    }
    if (options == null) {
      options = defaultOptions;
    }

    BasePayload payload = new GroupPayload(analyticsContext, options, groupId, groupTraits);
    submit(payload);
  }

  /** @see #track(String, Properties, Options) */
  public void track(String event) {
    track(event, null, null);
  }

  /** @see #track(String, Properties, Options) */
  public void track(String event, Properties properties) {
    track(event, properties, null);
  }

  /**
   * The track method is how you record any actions your users perform. Each action is known by a
   * name, like 'Purchased a T-Shirt'. You can also record properties specific to those actions.
   * For
   * example a 'Purchased a Shirt' event might have properties like revenue or size.
   *
   * @param event Name of the event. Must not be null or empty.
   * @param properties {@link Properties} to add extra information to this call
   * @param options To configure the call
   * @throws IllegalArgumentException if event name is null or an empty string
   * @see <a href="https://segment.io/docs/tracking-api/track/">Track Documentation</a>
   */
  public void track(String event, Properties properties, Options options) {
    if (isNullOrEmpty(event)) {
      throw new IllegalArgumentException("event must not be null or empty.");
    }
    if (properties == null) {
      properties = EMPTY_PROPERTIES;
    }
    if (options == null) {
      options = defaultOptions;
    }

    BasePayload payload = new TrackPayload(analyticsContext, options, event, properties);
    submit(payload);
  }

  /** @see #screen(String, String, Properties, Options) */
  public void screen(String category, String name) {
    screen(category, name, null, null);
  }

  /** @see #screen(String, String, Properties, Options) */
  public void screen(String category, String name, Properties properties) {
    screen(category, name, properties, null);
  }

  /**
   * The screen methods let your record whenever a user sees a screen of your mobile app, and
   * attach
   * a name, category or properties to the screen.
   * <p>
   * Either category or name must be provided.
   *
   * @param category A category to describe the screen
   * @param name A name for the screen
   * @param properties {@link Properties} to add extra information to this call
   * @param options To configure the call
   * @see <a href="http://segment.io/docs/tracking-api/page-and-screen/">Screen Documentation</a>
   */
  public void screen(String category, String name, Properties properties, Options options) {
    if (isNullOrEmpty(category) && isNullOrEmpty(name)) {
      throw new IllegalArgumentException("either category or name must be provided.");
    }

    if (properties == null) {
      properties = EMPTY_PROPERTIES;
    }
    if (options == null) {
      options = defaultOptions;
    }

    BasePayload payload = new ScreenPayload(analyticsContext, options, category, name, properties);
    submit(payload);
  }

  /** @see #alias(String, String, Options) */
  public void alias(String previousId, String newId) {
    alias(previousId, newId, null);
  }

  /**
   * The alias method is used to merge two user identities, effectively connecting two sets of user
   * data as one. This is an advanced method, but it is required to manage user identities
   * successfully in some of our integrations. You should still call {@link #identify(String,
   * Traits, Options)} with {@code newId} if you want to use it as the default id.
   *
   * @param previousId The old id we want to map. If it is null, the userId we've cached will
   * automatically used.
   * @param newId The newId to map the old id to. Must not be null to empty.
   * @param options To configure the call
   * @throws IllegalArgumentException if newId is null or empty
   * @see <a href="https://segment.io/docs/tracking-api/alias/">Alias Documentation</a>
   */
  public void alias(String previousId, String newId, Options options) {
    if (isNullOrEmpty(newId)) {
      throw new IllegalArgumentException("newId must not be null or empty.");
    }
    if (isNullOrEmpty(previousId)) {
      previousId = traitsCache.get().userId();
    }
    if (options == null) {
      options = defaultOptions;
    }

    BasePayload payload = new AliasPayload(analyticsContext, options, previousId);
    submit(payload);
  }

  /**
   * Asynchronously flushes all messages in the queue to the server, and tell integrations to do
   * the same.
   * <p>
   * Note that this will do nothing for bundled integrations that don't provide an explicit flush
   * method.
   */
  public void flush() {
    segment.dispatchFlush(0);
    integrationManager.dispatchFlush();
  }

  /** Get the {@link AnalyticsContext} used by this instance. */
  public AnalyticsContext getAnalyticsContext() {
    return analyticsContext;
  }

  /** Creates a {@link StatsSnapshot} of the current stats for this instance. */
  public StatsSnapshot getSnapshot() {
    return stats.createSnapshot();
  }

  /** Clear any information, including traits and user id about the current user. */
  public void logout() {
    traitsCache.delete();
    traitsCache.set(Traits.create(application));
    analyticsContext.setTraits(traitsCache.get());
  }

  /** Stops this instance from accepting further requests. */
  public void shutdown() {
    if (this == singleton) {
      throw new UnsupportedOperationException("Default singleton instance cannot be shutdown.");
    }
    if (shutdown) {
      return;
    }
    integrationManager.shutdown();
    segment.shutdown();
    stats.shutdown();
    shutdown = true;
  }

  /**
   * Register to be notified when a bundled integration is ready. See {@link
   * OnIntegrationReadyListener} for more information.
   * <p>
   * This method must be called from the main thread.
   *
   * @since 2.1
   * @deprecated Use {@link #registerOnIntegrationReady(OnIntegrationReadyListener)} instead.
   */
  public void onIntegrationReady(OnIntegrationReadyListener onIntegrationReadyListener) {
    registerOnIntegrationReady(onIntegrationReadyListener);
  }

  /**
   * Register to be notified when a bundled integration is ready. See {@link
   * OnIntegrationReadyListener} for more information.
   * <p>
   * This method must be called from the main thread.
   * <p>
   * Usage:
   * {@code
   * analytics.registerOnIntegrationReady((key, integration) ->
   * if("Mixpanel".equals(key)) {
   * ((MixpanelAPI) integration).clearSuperProperties();
   * }
   * }
   *
   * @since 2.3
   */
  public void registerOnIntegrationReady(OnIntegrationReadyListener onIntegrationReadyListener) {
    checkMain();
    integrationManager.dispatchRegisterIntegrationInitializedListener(onIntegrationReadyListener);
  }

  void submit(BasePayload payload) {
    logger.debug(OWNER_MAIN, VERB_CREATE, payload.id(), "%s", payload);
    segment.dispatchEnqueue(payload);
    integrationManager.dispatchOperation(payload);
  }

  void submit(ActivityLifecyclePayload payload) {
    logger.debug(OWNER_MAIN, VERB_CREATE, payload.id(), "%s", payload);
    integrationManager.dispatchOperation(payload);
  }

  /**
   * A callback interface that is invoked when the Analytics client initializes bundled
   * integrations.
   * <p>
   * In most cases, integrations would have already been initialized, and the callback will be
   * invoked fairly quickly. However there may be a latency the first time the app is launched, and
   * we don't have settings for bundled integrations yet. This is compounded if the user is
   * offline on the first run.
   */
  public interface OnIntegrationReadyListener {
    /**
     * This method will be invoked once for each integration. The first argument is a
     * key to uniquely identify each integration (which will the same as the one in our public HTTP
     * API). The second argument will be the integration object itself, so you can call methods not
     * exposed as a part of our spec. This is useful if you're doing things like A/B testing.
     *
     * @param key A unique string to identify an integration.
     * @param integration The underlying instance that has been initialized with the settings from
     * Segment
     */
    void onIntegrationReady(String key, Object integration);
  }

  /** Fluent API for creating {@link Analytics} instances. */
  public static class Builder {
    private final Application application;
    private String writeKey;
    private String tag;
    private int queueSize = 20;
    private int flushInterval = 30;
    private Options defaultOptions;
    private boolean debuggingEnabled = false;

    /** Start building a new {@link Analytics} instance. */
    public Builder(Context context, String writeKey) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      if (!hasPermission(context, Manifest.permission.INTERNET)) {
        throw new IllegalArgumentException("INTERNET permission is required.");
      }
      if (isNullOrEmpty(writeKey)) {
        throw new IllegalArgumentException("writeKey must not be null or empty.");
      }
      application = (Application) context.getApplicationContext();
      this.writeKey = writeKey;
    }

    /**
     * Set the queue size at which the client should flush events.
     *
     * @deprecated Use {@link #queueSize(int)} instead.
     */
    public Builder maxQueueSize(int maxQueueSize) {
      return queueSize(maxQueueSize);
    }

    /**
     * Set the queue size at which the client should flush events.
     * The client will automatically flush events every {@code flushInterval} seconds, or when the
     * queue reaches {@code queueSize}, whichever occurs first.
     */
    public Builder queueSize(int queueSize) {
      if (queueSize <= 0) {
        throw new IllegalArgumentException("queueSize must be greater than or equal to zero.");
      }
      this.queueSize = queueSize;
      return this;
    }

    /**
     * Set the interval (in seconds) at which the client should flush events.
     * The client will automatically flush events every {@code flushInterval} seconds, or when the
     * queue reaches {@code queueSize}, whichever occurs first.
     */
    public Builder flushInterval(int flushInterval) {
      if (flushInterval < 1) {
        throw new IllegalArgumentException("flushInterval must be greater than or equal to 1.");
      }
      this.flushInterval = flushInterval;
      return this;
    }

    /**
     * Set some default options for all calls. This options should not contain a timestamp. You
     * won't be able to change the integrations specified in this options object.
     */
    public Builder defaultOptions(Options defaultOptions) {
      if (defaultOptions == null) {
        throw new IllegalArgumentException("defaultOptions must not be null.");
      }
      if (defaultOptions.timestamp() != null) {
        throw new IllegalArgumentException("default option must not contain timestamp.");
      }
      if (this.defaultOptions != null) {
        throw new IllegalStateException("defaultOptions is already set.");
      }
      // Make a defensive copy
      this.defaultOptions = new Options();
      for (Map.Entry<String, Boolean> entry : defaultOptions.integrations().entrySet()) {
        this.defaultOptions.setIntegration(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * Set a tag for this instance. The tag is used to generate keys for caching. By default the
     * writeKey is used, but you may want to specify an alternative one, if you want the instances
     * to share different caches. For example, without this tag, all instances with the same
     * writeKey, will share the same traits. By specifying a custom tag for each instance of the
     * client, the instances will have a different traits instance.
     */
    public Builder tag(String tag) {
      if (isNullOrEmpty(tag)) {
        throw new IllegalArgumentException("tag must not be null or empty.");
      }
      if (this.tag != null) {
        throw new IllegalStateException("tag is already set.");
      }
      this.tag = tag;
      return this;
    }

    /**
     * Set whether debugging is enabled or not.
     *
     * @deprecated Use {@link #debugging(boolean)} instead.
     */
    public Builder logging(boolean debuggingEnabled) {
      return debugging(debuggingEnabled);
    }

    /**
     * Set whether debugging is enabled or not.
     *
     * @since 2.3
     */
    public Builder debugging(boolean debuggingEnabled) {
      this.debuggingEnabled = debuggingEnabled;
      return this;
    }

    /** Create a {@link Analytics} client. */
    public Analytics build() {
      if (defaultOptions == null) {
        defaultOptions = new Options();
      }
      if (isNullOrEmpty(tag)) tag = writeKey;

      Logger logger = new Logger(debuggingEnabled);
      Stats stats = new Stats();
      SegmentHTTPApi segmentHTTPApi = new SegmentHTTPApi(writeKey);
      IntegrationManager integrationManager =
          IntegrationManager.create(application, segmentHTTPApi, stats, logger, tag,
              debuggingEnabled);
      Segment segment = Segment.create(application, queueSize, flushInterval, segmentHTTPApi,
          integrationManager.bundledIntegrations, tag, stats, logger);

      Traits.Cache traitsCache = new Traits.Cache(application, tag, Traits.class);
      if (!traitsCache.isSet() || traitsCache.get() == null) {
        traitsCache.set(Traits.create(application));
      }
      AnalyticsContext analyticsContext = new AnalyticsContext(application, traitsCache.get());

      return new Analytics(application, integrationManager, segment, stats, traitsCache,
          analyticsContext, defaultOptions, logger, debuggingEnabled);
    }
  }
}
