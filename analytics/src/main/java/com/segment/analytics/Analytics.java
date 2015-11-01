/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment, Inc.
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
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import com.segment.analytics.integrations.AliasPayload;
import com.segment.analytics.integrations.BasePayload;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Log;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import com.segment.analytics.internal.Utils;
import com.segment.analytics.internal.Utils.AnalyticsExecutorService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.internal.Utils.THREAD_PREFIX;
import static com.segment.analytics.internal.Utils.buffer;
import static com.segment.analytics.internal.Utils.closeQuietly;
import static com.segment.analytics.internal.Utils.getResourceString;
import static com.segment.analytics.internal.Utils.hasPermission;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * The entry point into the Segment for Android SDK.
 * <p/>
 * The idea is simple: one pipeline for all your data. Segment is the single hub to collect,
 * translate and route your data with the flip of a switch.
 * <p/>
 * Analytics for Android will automatically batch events, queue them to disk, and upload it
 * periodically to Segment for you. It will also look up your project's settings (that you've
 * configured in the web interface), specifically looking up settings for bundled integrations, and
 * then initialize them for you on the user's phone, and mapping our standardized events to formats
 * they can all understand. You only need to instrument Segment once, then flip a switch to install
 * new tools.
 * <p/>
 * This class is the main entry point into the client API. Use {@link
 * #with(android.content.Context)} for the global singleton instance or construct your own instance
 * with {@link Builder}.
 *
 * @see <a href="https://Segment/">Segment</a>
 */
public class Analytics {
  private static final String ANALYTICS_THREAD_NAME = THREAD_PREFIX + "Analytics";
  static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
    @Override public void handleMessage(Message msg) {
      throw new AssertionError("Unknown handler message received: " + msg.what);
    }
  };
  static final String WRITE_KEY_RESOURCE_IDENTIFIER = "analytics_write_key";
  static final List<String> INSTANCES = new ArrayList<>(1);
  volatile static Analytics singleton = null;
  private static final Properties EMPTY_PROPERTIES = new Properties();

  private final Application application;
  final ExecutorService networkExecutor;
  final Stats stats;
  private final Options defaultOptions;
  private final Traits.Cache traitsCache;
  private final AnalyticsContext analyticsContext;
  private final Log log;
  final String tag;
  final Client client;
  final Cartographer cartographer;
  private final ProjectSettings.Cache projectSettingsCache;
  private final String writeKey;
  final int flushQueueSize;
  final long flushIntervalInMillis;
  private final HandlerThread analyticsThread;
  private final AnalyticsHandler analyticsHandler;

  final Map<String, Boolean> bundledIntegrations = new ConcurrentHashMap<>();
  // todo: use lightweight map implementations.
  private Map<String, Integration.Factory> factories;
  private Map<String, Integration<?>> integrations;
  volatile boolean shutdown;

  /**
   * Return a reference to the global default {@link Analytics} instance.
   * <p/>
   * This instance is automatically initialized with defaults that are suitable to most
   * implementations.
   * <p/>
   * If these settings do not meet the requirements of your application, you can override defaults
   * in {@code analytics.xml}, or you can construct your own instance with full control over the
   * configuration by using {@link Builder}.
   * <p/>
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
            String packageName = context.getPackageName();
            int flags = context.getPackageManager().getApplicationInfo(packageName, 0).flags;
            boolean debugging = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (debugging) {
              builder.logLevel(LogLevel.INFO);
            }
          } catch (PackageManager.NameNotFoundException ignored) {
          }

          singleton = builder.build();
        }
      }
    }
    return singleton;
  }

  /**
   * Set the global instance returned from {@link #with}.
   * <p/>
   * This method must be called before any calls to {@link #with} and may only be called once.
   */
  public static void setSingletonInstance(Analytics analytics) {
    synchronized (Analytics.class) {
      if (singleton != null) {
        throw new IllegalStateException("Singleton instance already exists.");
      }
      singleton = analytics;
    }
  }

  Analytics(Application application, ExecutorService networkExecutor, Stats stats,
      Traits.Cache traitsCache, AnalyticsContext analyticsContext, Options defaultOptions, Log log,
      String tag, Map<String, Integration.Factory> factories, Client client,
      Cartographer cartographer, ProjectSettings.Cache projectSettingsCache, String writeKey,
      int flushQueueSize, long flushIntervalInMillis) {
    this.application = application;
    this.networkExecutor = networkExecutor;
    this.stats = stats;
    this.traitsCache = traitsCache;
    this.analyticsContext = analyticsContext;
    this.defaultOptions = defaultOptions;
    this.log = log;
    this.tag = tag;
    this.client = client;
    this.cartographer = cartographer;
    this.projectSettingsCache = projectSettingsCache;
    this.writeKey = writeKey;
    this.flushQueueSize = flushQueueSize;
    this.flushIntervalInMillis = flushIntervalInMillis;
    this.factories = Collections.unmodifiableMap(factories);

    analyticsThread = new HandlerThread(ANALYTICS_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    analyticsThread.start();
    analyticsHandler = new AnalyticsHandler(analyticsThread.getLooper(), this);

    analyticsHandler.sendEmptyMessage(AnalyticsHandler.INITIALIZE);

    log.debug("Created analytics client for project with tag:%s.", tag);
  }

  // Analytics API

  /**
   * Identify lets you tie one of your users and their actions to a recognizable {@code userId}. It
   * also lets you record {@code traits} about the user, like their email, name, account type, etc.
   * This method will simply set the userId for the current user.
   *
   * @see #identify(String, Traits, Options)
   */
  public void identify(String userId) {
    identify(userId, null, null);
  }

  /**
   * Identify lets you tie one of your users and their actions to a recognizable {@code userId}. It
   * also lets you record {@code traits} about the user, like their email, name, account type, etc.
   * This method will simply add the given traits to the user profile.
   *
   * @see #identify(String, Traits, Options)
   */
  public void identify(Traits traits) {
    identify(null, traits, null);
  }

  /**
   * Identify lets you tie one of your users and their actions to a recognizable {@code userId}. It
   * also lets you record {@code traits} about the user, like their email, name, account type, etc.
   * <p/>
   * Traits and userId will be automatically cached and available on future sessions for the same
   * user. To update a trait on the server, call identify with the same user id (or null). You can
   * also use {@link #identify(Traits)} for this purpose.
   *
   * @param userId Unique identifier which you recognize a user by in your own database. If this
   * is null or empty, any previous id we have (could be the anonymous id) will be
   * used.
   * @param newTraits Traits about the user
   * @param options To configure the call
   * @throws IllegalArgumentException if both {@code userId} and {@code newTraits} are not provided
   * @see <a href="https://segment.com/docs/tracking-api/identify/">Identify Documentation</a>
   */
  public void identify(String userId, Traits newTraits, Options options) {
    if (isNullOrEmpty(userId) && isNullOrEmpty(newTraits)) {
      throw new IllegalArgumentException("Either userId or some traits must be provided.");
    }

    Traits traits = traitsCache.get();
    if (!isNullOrEmpty(userId)) {
      traits.putUserId(userId);
    }
    if (!isNullOrEmpty(newTraits)) {
      traits.putAll(newTraits);
    }

    traitsCache.set(traits); // Save the new traits
    analyticsContext.setTraits(traits); // Update the references

    if (options == null) {
      options = defaultOptions;
    }

    IdentifyPayload payload = new IdentifyPayload(analyticsContext, options, traitsCache.get());
    submit(payload);
  }

  /**
   * The group method lets you associate a user with a group. It also lets you record custom traits
   * about the group, like industry or number of employees.
   *
   * @see #group(String, Traits, Options)
   */
  public void group(String groupId) {
    group(groupId, null, null);
  }

  /**
   * The group method lets you associate a user with a group. It also lets you record custom traits
   * about the group, like industry or number of employees.
   * <p/>
   * If you've called {@link #identify(String, Traits, Options)} before, this will automatically
   * remember the userId. If not, it will fall back to use the anonymousId instead.
   *
   * @param groupId Unique identifier which you recognize a group by in your own database. Must not
   * be null or empty.
   * @param options To configure the call
   * @throws IllegalArgumentException if groupId is null or an empty string
   * @see <a href="https://segment.com/docs/tracking-api/group/">Group Documentation</a>
   */
  public void group(String groupId, Traits groupTraits, Options options) {
    if (isNullOrEmpty(groupId)) {
      throw new IllegalArgumentException("groupId must not be null or empty.");
    }
    if (groupTraits == null) {
      groupTraits = new Traits();
    }
    if (options == null) {
      options = defaultOptions;
    }

    GroupPayload payload = new GroupPayload(analyticsContext, options, groupId, groupTraits);
    submit(payload);
  }

  /**
   * The track method is how you record any actions your users perform. Each action is known by a
   * name, like 'Purchased a T-Shirt'. You can also record properties specific to those actions.
   * For example a 'Purchased a Shirt' event might have properties like revenue or size.
   *
   * @see #track(String, Properties, Options)
   */
  public void track(String event) {
    track(event, null, null);
  }

  /**
   * The track method is how you record any actions your users perform. Each action is known by a
   * name, like 'Purchased a T-Shirt'. You can also record properties specific to those actions.
   * For example a 'Purchased a Shirt' event might have properties like revenue or size.
   *
   * @see #track(String, Properties, Options)
   */
  public void track(String event, Properties properties) {
    track(event, properties, null);
  }

  /**
   * The track method is how you record any actions your users perform. Each action is known by a
   * name, like 'Purchased a T-Shirt'. You can also record properties specific to those actions.
   * For example a 'Purchased a Shirt' event might have properties like revenue or size.
   *
   * @param event Name of the event. Must not be null or empty.
   * @param properties {@link Properties} to add extra information to this call
   * @param options To configure the call
   * @throws IllegalArgumentException if event name is null or an empty string
   * @see <a href="https://segment.com/docs/tracking-api/track/">Track Documentation</a>
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

    TrackPayload payload = new TrackPayload(analyticsContext, options, event, properties);
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
   * <p/>
   * Either category or name must be provided.
   *
   * @param category A category to describe the screen
   * @param name A name for the screen
   * @param properties {@link Properties} to add extra information to this call
   * @param options To configure the call
   * @see <a href="http://segment.com/docs/tracking-api/page-and-screen/">Screen Documentation</a>
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

    ScreenPayload payload =
        new ScreenPayload(analyticsContext, options, category, name, properties);
    submit(payload);
  }

  /**
   * The alias method is used to merge two user identities, effectively connecting two sets of user
   * data as one. This is an advanced method, but it is required to manage user identities
   * successfully in some of our integrations.
   *
   * @see #alias(String, Options)
   */
  public void alias(String newId) {
    alias(newId, null);
  }

  /**
   * The alias method is used to merge two user identities, effectively connecting two sets of user
   * data as one. This is an advanced method, but it is required to manage user identities
   * successfully in some of our integrations.
   * <p>
   *
   * Usage:
   * <pre> <code>
   *   analytics.track("user did something");
   *   analytics.alias(newId);
   *   analytics.identify(newId);
   * </code> </pre>
   *
   * @param newId The new ID you want to alias the existing ID to. The existing ID will be either
   * the previousId if you have called identify, or the anonymous ID.
   * @param options To configure the call
   * @throws IllegalArgumentException if newId is null or empty
   * @see <a href="https://segment.com/docs/tracking-api/alias/">Alias Documentation</a>
   */
  public void alias(String newId, Options options) {
    if (isNullOrEmpty(newId)) {
      throw new IllegalArgumentException("newId must not be null or empty.");
    }
    if (options == null) {
      options = defaultOptions;
    }

    AliasPayload payload = new AliasPayload(analyticsContext, options, newId);
    submit(payload);
  }

  void submit(BasePayload payload) {
    if (shutdown) {
      throw new IllegalStateException("Cannot enqueue messages after client is shutdown.");
    }
    log.verbose("Created payload %s.", payload);
    IntegrationOperation operation;
    switch (payload.type()) {
      case identify:
        operation = IntegrationOperation.identify((IdentifyPayload) payload);
        break;
      case alias:
        operation = IntegrationOperation.alias((AliasPayload) payload);
        break;
      case group:
        operation = IntegrationOperation.group((GroupPayload) payload);
        break;
      case track:
        operation = IntegrationOperation.track((TrackPayload) payload);
        break;
      case screen:
        operation = IntegrationOperation.screen((ScreenPayload) payload);
        break;
      default:
        throw new AssertionError("unknown type " + payload.type());
    }
    Message message = analyticsHandler.obtainMessage(AnalyticsHandler.ENQUEUE, operation);
    analyticsHandler.sendMessage(message);
  }

  /**
   * Asynchronously flushes all messages in the queue to the server, and tells bundled integrations
   * to do the same.
   */
  public void flush() {
    if (shutdown) {
      throw new IllegalStateException("Cannot enqueue messages after client is shutdown.");
    }
    IntegrationOperation operation = IntegrationOperation.FLUSH;
    Message message = analyticsHandler.obtainMessage(AnalyticsHandler.ENQUEUE, operation);
    analyticsHandler.sendMessage(message);
  }

  /** Get the {@link AnalyticsContext} used by this instance. */
  @SuppressWarnings("UnusedDeclaration") public AnalyticsContext getAnalyticsContext() {
    return analyticsContext;
  }

  /** Creates a {@link StatsSnapshot} of the current stats for this instance. */
  public StatsSnapshot getSnapshot() {
    return stats.createSnapshot();
  }

  /** Return the {@link Application} used to create this instance. */
  public Application getApplication() {
    return application;
  }

  /**
   * Return the {@link LogLevel} for this instance.
   *
   * @deprecated This will be removed in a future release.
   */
  @Deprecated public LogLevel getLogLevel() {
    return log.logLevel;
  }

  /** Return the {@link Log} instance used by this client. */
  public Log getLogger() {
    return log;
  }

  /**
   * Logs out the current user by clearing any information, including traits and user id.
   *
   * @deprecated Use {@link #reset()} instead
   */
  @Deprecated public void logout() {
    reset();
  }

  /**
   * Resets the analytics client by clearing any stored information about the user. Events queued
   * on disk are not cleared, and will be uploaded at a later time.
   */
  public void reset() {
    traitsCache.delete();
    traitsCache.set(Traits.create());
    analyticsContext.setTraits(traitsCache.get());
    IntegrationOperation operation = IntegrationOperation.RESET;
    Message message = analyticsHandler.obtainMessage(AnalyticsHandler.ENQUEUE, operation);
    analyticsHandler.sendMessage(message);
  }

  /**
   * Register to be notified when a bundled integration is ready.
   * <p/>
   * In most cases, integrations would have already been initialized, and the callback will be
   * invoked fairly quickly. However there may be a latency the first time the app is launched, and
   * we don't have settings for bundled integrations yet. This is compounded if the user is offline
   * on the first run.
   * <p/>
   * You can only register for one callback per integration at a time, and passing in a {@code
   * callback} will remove the previous callback for that integration.
   * </p>
   * The callback is invoked on the same thread we interact with integrations. Ff you want to
   * update
   * the UI, make sure you move off to the main thread.
   * <p/>
   * Usage:
   * <pre> <code>
   *   analytics.onIntegrationReady("Amplitude", new Callback() {
   *     {@literal @}Override public void onIntegrationReady(Object instance) {
   *       Amplitude.enableLocationListening();
   *     }
   *   });
   *   analytics.onIntegrationReady("Mixpanel", new Callback() {
   *     {@literal @}Override public void onIntegrationReady(Object instance) {
   *       ((MixpanelAPI) instance).clearSuperProperties();
   *     }
   *   })*
   * </code> </pre>
   */
  public <T> void onIntegrationReady(String key, Callback<T> callback) {
    if (isNullOrEmpty(key)) {
      throw new IllegalArgumentException("key cannot be null or empty.");
    }

    Pair<String, Callback<T>> pair = new Pair<>(key, callback);
    analyticsHandler.sendMessage(analyticsHandler.obtainMessage(AnalyticsHandler.CALLBACK, pair));
  }

  /** @deprecated Use {@link #onIntegrationReady(String, Callback)} instead. */
  public void onIntegrationReady(BundledIntegration integration, Callback callback) {
    if (integration == null) {
      throw new IllegalArgumentException("integration cannot be null or empty.");
    }

    Pair<String, Callback> pair = new Pair<>(integration.key, callback);
    analyticsHandler.sendMessage(analyticsHandler.obtainMessage(AnalyticsHandler.CALLBACK, pair));
  }

  /** @deprecated  */
  public enum BundledIntegration {
    AMPLITUDE("Amplitude"),
    APPS_FLYER("AppsFlyer"),
    APPTIMIZE("Apptimize"),
    BUGSNAG("Bugsnag"),
    COUNTLY("Countly"),
    CRITTERCISM("Crittercism"),
    FLURRY("Flurry"),
    GOOGLE_ANALYTICS("Google Analytics"),
    KAHUNA("Kahuna"),
    LEANPLUM("Leanplum"),
    LOCALYTICS("Localytics"),
    MIXPANEL("Mixpanel"),
    QUANTCAST("Quantcast"),
    TAPLYTICS("Taplytics"),
    TAPSTREAM("Tapstream"),
    UXCAM("UXCam");

    /** The key that identifies this integration in our API. */
    final String key;

    BundledIntegration(String key) {
      this.key = key;
    }
  }

  /** Stops this instance from accepting further requests. */
  public void shutdown() {
    if (this == singleton) {
      throw new UnsupportedOperationException("Default singleton instance cannot be shutdown.");
    }
    if (shutdown) {
      return;
    }
    analyticsThread.quit();
    if (networkExecutor instanceof AnalyticsExecutorService) {
      networkExecutor.shutdown();
    }
    stats.shutdown();
    shutdown = true;
    synchronized (INSTANCES) {
      INSTANCES.remove(tag);
    }
  }

  /** Controls the level of logging. */
  public enum LogLevel {
    /** No logging. */
    NONE,
    /** Log exceptions only. */
    INFO,
    /** Log exceptions and print debug output. */
    DEBUG,
    /**
     * Log exceptions and print debug output.
     *
     * @deprecated Use {@link LogLevel#DEBUG} instead.
     */
    @Deprecated BASIC,
    /** Same as {@link LogLevel#DEBUG}, and log transformations in bundled integrations. */
    VERBOSE;

    public boolean log() {
      return this != NONE;
    }
  }

  /**
   * A callback interface that is invoked when the Analytics client initializes bundled
   * integrations.
   */
  public interface Callback<T> {
    /**
     * This method will be invoked once for each callback.
     *
     * @param instance The underlying instance that has been initialized with the settings from
     * Segment.
     */
    void onReady(T instance);
  }

  /** Fluent API for creating {@link Analytics} instances. */
  public static class Builder {
    private final Application application;
    private String writeKey;
    private boolean collectDeviceID = Utils.DEFAULT_COLLECT_DEVICE_ID;
    private int flushQueueSize = Utils.DEFAULT_FLUSH_QUEUE_SIZE;
    private long flushIntervalInMillis = Utils.DEFAULT_FLUSH_INTERVAL;
    private Options defaultOptions;
    private String tag;
    private LogLevel logLevel;
    private ExecutorService networkExecutor;
    private ConnectionFactory connectionFactory;
    private Map<String, Integration.Factory> factories;

    /** Start building a new {@link Analytics} instance. */
    public Builder(Context context, String writeKey) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      if (!hasPermission(context, Manifest.permission.INTERNET)) {
        throw new IllegalArgumentException("INTERNET permission is required.");
      }
      application = (Application) context.getApplicationContext();
      if (application == null) {
        throw new IllegalArgumentException("Application context must not be null.");
      }

      if (isNullOrEmpty(writeKey)) {
        throw new IllegalArgumentException("writeKey must not be null or empty.");
      }
      this.writeKey = writeKey;

      factories = new LinkedHashMap<>();
    }

    /**
     * Set the queue size at which the client should flush events. The client will automatically
     * flush events to Segment when the queue reaches {@code flushQueueSize}.
     *
     * @throws IllegalArgumentException if the flushQueueSize is less than or equal to zero.
     */
    public Builder flushQueueSize(int flushQueueSize) {
      if (flushQueueSize <= 0) {
        throw new IllegalArgumentException("flushQueueSize must be greater than or equal to zero.");
      }
      // 250 is a reasonably high number to trigger queue size flushes.
      // The queue may go over this size (as much as 1000), but you should flush much before then.
      if (flushQueueSize > 250) {
        throw new IllegalArgumentException("flushQueueSize must be less than or equal to 250.");
      }
      this.flushQueueSize = flushQueueSize;
      return this;
    }

    /**
     * Set the interval at which the client should flush events. The client will automatically
     * flush events to Segment every {@code flushInterval} duration, regardless of {@code
     * flushQueueSize}.
     *
     * @throws IllegalArgumentException if the flushInterval is less than or equal to zero.
     */
    public Builder flushInterval(long flushInterval, TimeUnit timeUnit) {
      if (timeUnit == null) {
        throw new IllegalArgumentException("timeUnit must not be null.");
      }
      if (flushInterval <= 0) {
        throw new IllegalArgumentException("flushInterval must be greater than zero.");
      }
      this.flushIntervalInMillis = timeUnit.toMillis(flushInterval);
      return this;
    }

    /**
     * Enable or disable collection of {@link android.provider.Settings.Secure#ANDROID_ID},
     * {@link android.os.Build#SERIAL} or the Telephony Identifier retreived via
     * TelephonyManager as available. Collection of the device identifier is enabled by default.
     */
    public Builder collectDeviceId(boolean collect) {
      this.collectDeviceID = collect;
      return this;
    }

    /**
     * Set some default options for all calls. This will only be used to figure out which
     * integrations should be enabled or not for actions by default.
     *
     * @see {@link Options}
     */
    public Builder defaultOptions(Options defaultOptions) {
      if (defaultOptions == null) {
        throw new IllegalArgumentException("defaultOptions must not be null.");
      }
      // Make a defensive copy
      this.defaultOptions = new Options();
      for (Map.Entry<String, Object> entry : defaultOptions.integrations().entrySet()) {
        if (entry.getValue() instanceof Boolean) {
          this.defaultOptions.setIntegration(entry.getKey(), (Boolean) entry.getValue());
        } else {
          // A value is provided for an integration, and it is not a boolean. Assume it is enabled.
          this.defaultOptions.setIntegration(entry.getKey(), true);
        }
      }
      return this;
    }

    /**
     * Set a tag for this instance. The tag is used to generate keys for caching.
     * </p>
     * By default the writeKey is used. You may want to specify an alternative one, if you want
     * the instances with the same writeKey to share different caches (you probably do).
     *
     * @throws IllegalArgumentException if the tag is null or empty.
     */
    public Builder tag(String tag) {
      if (isNullOrEmpty(tag)) {
        throw new IllegalArgumentException("tag must not be null or empty.");
      }
      this.tag = tag;
      return this;
    }

    /** Set a {@link LogLevel} for this instance. */
    public Builder logLevel(LogLevel logLevel) {
      if (logLevel == null) {
        throw new IllegalArgumentException("LogLevel must not be null.");
      }
      this.logLevel = logLevel;
      return this;
    }

    /** @deprecated As of {@code 3.0.1}, this method does nothing. */
    @Deprecated public Builder disableBundledIntegrations() {
      return this;
    }

    /**
     * Specify the executor service for making network calls in the background.
     * <p/>
     * Note: Calling {@link Analytics#shutdown()} will not shutdown supplied executors.
     * <p/>
     * Use it with care! http://bit.ly/1JVlA2e
     */
    public Builder networkExecutor(ExecutorService networkExecutor) {
      if (networkExecutor == null) {
        throw new IllegalArgumentException("Executor service must not be null.");
      }
      this.networkExecutor = networkExecutor;
      return this;
    }

    /**
     * Specify the connection factory for customizing how connections are created.
     * <p/>
     * This is a beta API, and might be changed in the future.
     * Use it with care! http://bit.ly/1JVlA2e
     */
    public Builder connectionFactory(ConnectionFactory connectionFactory) {
      if (connectionFactory == null) {
        throw new IllegalArgumentException("ConnectionFactory must not be null.");
      }
      this.connectionFactory = connectionFactory;
      return this;
    }

    /** TODO: docs */
    public Builder use(String key, Integration.Factory factory) {
      if (factory == null) {
        throw new IllegalArgumentException("Factory must not be null.");
      }
      factories.put(key, factory);
      return this;
    }

    /** Create a {@link Analytics} client. */
    public Analytics build() {
      if (isNullOrEmpty(tag)) {
        tag = writeKey;
      }
      synchronized (INSTANCES) {
        if (INSTANCES.contains(tag)) {
          throw new IllegalStateException("Duplicate analytics client created with tag: "
              + tag
              + ". If you want to use multiple Analytics clients, use a different writeKey "
              + "or set a tag via the builder during construction.");
        }
        INSTANCES.add(tag);
      }

      if (defaultOptions == null) {
        defaultOptions = new Options();
      }
      if (logLevel == null) {
        logLevel = LogLevel.NONE;
      }
      if (networkExecutor == null) {
        networkExecutor = new AnalyticsExecutorService();
      }
      if (connectionFactory == null) {
        connectionFactory = new ConnectionFactory();
      }

      final Stats stats = new Stats();
      final Cartographer cartographer = Cartographer.INSTANCE;
      final Client client = new Client(application, writeKey, connectionFactory);

      ProjectSettings.Cache projectSettingsCache =
          new ProjectSettings.Cache(application, cartographer, tag);

      Traits.Cache traitsCache = new Traits.Cache(application, cartographer, tag);
      if (!traitsCache.isSet() || traitsCache.get() == null) {
        Traits traits = Traits.create();
        traitsCache.set(traits);
      }
      AnalyticsContext analyticsContext =
          AnalyticsContext.create(application, traitsCache.get(), collectDeviceID);
      analyticsContext.attachAdvertisingId(application);

      Map<String, Integration.Factory> factories = new LinkedHashMap<>(1 + this.factories.size());
      factories.put(SegmentIntegration.SEGMENT_KEY, SegmentIntegration.FACTORY);
      factories.putAll(this.factories);

      return new Analytics(application, networkExecutor, stats, traitsCache, analyticsContext,
          defaultOptions, new Log(logLevel), tag, factories, client, cartographer,
          projectSettingsCache, writeKey, flushQueueSize, flushIntervalInMillis);
    }
  }

  // Handler Logic.

  static class AnalyticsHandler extends Handler {
    static final int INITIALIZE = 1;
    static final int ENQUEUE = 2;
    static final int CALLBACK = 3;

    private final Analytics analytics;

    AnalyticsHandler(Looper looper, Analytics analytics) {
      super(looper);
      this.analytics = analytics;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case INITIALIZE:
          analytics.performInitializeIntegrations();
          break;
        case ENQUEUE:
          analytics.performIntegrationOperation((IntegrationOperation) msg.obj);
          break;
        case CALLBACK:
          Pair<String, Analytics.Callback<?>> pair = (Pair<String, Analytics.Callback<?>>) msg.obj;
          analytics.performCallback(pair.first, pair.second);
          break;
        default:
          throw new AssertionError("Unknown Integration Manager handler message: " + msg);
      }
    }
  }

  private static final long SETTINGS_REFRESH_INTERVAL = 1000 * 60 * 60 * 24; // 24 hours
  private static final long SETTINGS_RETRY_INTERVAL = 1000 * 60; // 1 minute

  private ProjectSettings fetch() {
    try {
      ProjectSettings projectSettings = networkExecutor.submit(new Callable<ProjectSettings>() {
        @Override public ProjectSettings call() throws Exception {
          Client.Connection connection = null;
          try {
            connection = client.fetchSettings();
            Map<String, Object> map = cartographer.fromJson(buffer(connection.is));
            return ProjectSettings.create(map);
          } finally {
            closeQuietly(connection);
          }
        }
      }).get();
      projectSettingsCache.set(projectSettings);
      return projectSettings;
    } catch (InterruptedException e) {
      log.error(e, "Thread interrupted while fetching settings.");
    } catch (ExecutionException e) {
      log.error(e, "Unable to fetch settings. Retrying in %s ms.", SETTINGS_RETRY_INTERVAL);
    }
    return null;
  }

  private void performInitializeIntegrations() {
    ProjectSettings projectSettings = projectSettingsCache.get();

    if (isNullOrEmpty(projectSettings)) {
      if (isConnected(application)) {
        projectSettings = fetch();
      }

      if (isNullOrEmpty(projectSettings)) {
        // We don't have any cached settings, and we can't connect to the internet. Enable just the
        // Segment integration:
        // {
        //   integrations: {
        //     Segment.io: {
        //       apiKey: "{writeKey}"
        //     }
        //   }
        // }
        ValueMap settings = new ValueMap().putValue("integrations",
            new ValueMap().putValue("Segment.io", new ValueMap().putValue("apiKey", writeKey)));
        projectSettings = ProjectSettings.create(settings);
      }
    } else {
      if (projectSettings.timestamp() + SETTINGS_REFRESH_INTERVAL < System.currentTimeMillis()) {
        if (isConnected(application)) {
          projectSettings = fetch();
        }
      }
    }

    ValueMap integrationSettings = projectSettings.integrations();
    integrations = new LinkedHashMap<>(factories.size());
    for (Map.Entry<String, Integration.Factory> entry : factories.entrySet()) {
      String key = entry.getKey();
      ValueMap settings = integrationSettings.getValueMap(key);
      if (isNullOrEmpty(settings)) {
        log.debug("Integration %s is not enabled.", key);
        continue;
      }
      Integration.Factory factory = entry.getValue();
      Integration integration = factory.create(settings, this);
      if (integration == null) {
        log.info("Factory %s couldn't create integration.", factory);
      } else {
        integrations.put(key, integration);
        bundledIntegrations.put(key, true);
      }
    }
    factories = null;
  }

  /** Runs the given operation on all integrations. */
  void performIntegrationOperation(IntegrationOperation operation) {
    for (Map.Entry<String, Integration<?>> entry : integrations.entrySet()) {
      String key = entry.getKey();
      long startTime = System.nanoTime();
      operation.run(key, entry.getValue(), projectSettingsCache.get());
      long endTime = System.nanoTime();
      long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
      stats.dispatchIntegrationOperation(key, duration);
    }
  }

  private <T> void performCallback(String key, Callback<T> callback) {
    for (Map.Entry<String, Integration<?>> entry : integrations.entrySet()) {
      if (key.equals(entry.getKey())) {
        callback.onReady((T) entry.getValue().getUnderlyingInstance());
        return;
      }
    }
  }
}
