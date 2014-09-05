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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.segment.analytics.internal.AnalyticsActivityLifecycleCallbacksAdapter;
import com.segment.analytics.internal.Dispatcher;
import com.segment.analytics.internal.IntegrationManager;
import com.segment.analytics.internal.Logger;
import com.segment.analytics.internal.SegmentHTTPApi;
import com.segment.analytics.internal.Stats;
import com.segment.analytics.internal.payload.AliasPayload;
import com.segment.analytics.internal.payload.BasePayload;
import com.segment.analytics.internal.payload.GroupPayload;
import com.segment.analytics.internal.payload.IdentifyPayload;
import com.segment.analytics.internal.payload.ScreenPayload;
import com.segment.analytics.internal.payload.TrackPayload;

import static com.segment.analytics.internal.Utils.getResourceBooleanOrThrow;
import static com.segment.analytics.internal.Utils.getResourceIntegerOrThrow;
import static com.segment.analytics.internal.Utils.getResourceString;
import static com.segment.analytics.internal.Utils.hasPermission;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * The idea is simple: one pipeline for all your data.
 * <p/>
 * Use {@link #with(android.content.Context)} for the global singleton instance or construct your
 * own instance with {@link Builder}.
 *
 * @see <a href="https://segment.io/">Segment.io</a>
 */
public class Analytics {
  // Resource identifiers to define options in xml
  public static final String WRITE_KEY_RESOURCE_IDENTIFIER = "analytics_write_key";
  public static final String QUEUE_SIZE_RESOURCE_IDENTIFIER = "analytics_queue_size";
  public static final String DEBUGGING_RESOURCE_IDENTIFIER = "analytics_debug";

  static Analytics singleton = null;

  /**
   * The global default {@link Analytics} instance.
   * <p/>
   * This instance is automatically initialized with defaults that are suitable to most
   * implementations.
   * <p/>
   * If these settings do not meet the requirements of your application, you can provide properties
   * in {@code analytics.xml} or you can construct your own instance with full control over the
   * configuration by using {@link Builder}.
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
            int maxQueueSize = getResourceIntegerOrThrow(context, QUEUE_SIZE_RESOURCE_IDENTIFIER);
            if (maxQueueSize <= 0) {
              throw new IllegalStateException(QUEUE_SIZE_RESOURCE_IDENTIFIER
                  + "("
                  + maxQueueSize
                  + ") may not be zero or negative.");
            }
            builder.maxQueueSize(maxQueueSize);
          } catch (Resources.NotFoundException e) {
            Logger.d("%s not defined in xml. Using default value.", QUEUE_SIZE_RESOURCE_IDENTIFIER);
            // when maxQueueSize is not defined in xml, we'll use a default option in the builder
          }
          try {
            boolean debugging = getResourceBooleanOrThrow(context, DEBUGGING_RESOURCE_IDENTIFIER);
            builder.debugging(debugging);
          } catch (Resources.NotFoundException notFoundException) {
            Logger.d("%s not defined in xml.", DEBUGGING_RESOURCE_IDENTIFIER);
            String packageName = context.getPackageName();
            try {
              final int flags =
                  context.getPackageManager().getApplicationInfo(packageName, 0).flags;
              boolean debugging = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
              builder.debugging(debugging);
            } catch (PackageManager.NameNotFoundException nameNotFoundException) {
              Logger.e(nameNotFoundException,
                  "Could not look up package flags. Disabling debugging.");
            }
          }
          singleton = builder.build();
        }
      }
    }
    return singleton;
  }

  /** Fluent API for creating {@link Analytics} instances. */
  @SuppressWarnings("UnusedDeclaration") // Public API.
  public static class Builder {
    public static final int DEFAULT_QUEUE_SIZE = 20;
    public static final boolean DEFAULT_DEBUGGING = false;

    private final Application application;
    private String writeKey;
    private String tag;
    private int maxQueueSize = -1;
    private Options defaultOptions;

    private boolean debugging = DEFAULT_DEBUGGING;

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

    /** Set the queue size at which we should flush events. */
    public Builder maxQueueSize(int maxQueueSize) {
      if (maxQueueSize <= 0) {
        throw new IllegalArgumentException("maxQueueSize must be greater than or equal to zero.");
      }
      if (this.maxQueueSize != -1) {
        throw new IllegalStateException("maxQueueSize is already set.");
      }
      this.maxQueueSize = maxQueueSize;
      return this;
    }

    /** Set some default options for all calls. */
    public Builder defaultOptions(Options options) {
      if (options == null) {
        throw new IllegalArgumentException("options must not be null.");
      }
      if (this.defaultOptions != null) {
        throw new IllegalStateException("options is already set.");
      }
      this.defaultOptions = options;
      return this;
    }

    /**
     * Set a tag for this instance. The tag is used to generate keys for caching. By default the
     * writeKey is used, but you may want to specify an alternative one, if you want the instances
     * to share different caches. For example, without this tag, all instances will share the same
     * traits. By specifying a custom tag for each instance of the client, all instance will have a
     * different traits instance.
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

    /** Set whether debugging is enabled or not. */
    public Builder debugging(boolean debugging) {
      this.debugging = debugging;
      return this;
    }

    /** Create Segment {@link Analytics} instance. */
    public Analytics build() {
      if (maxQueueSize == -1) {
        maxQueueSize = DEFAULT_QUEUE_SIZE;
      }
      if (defaultOptions == null) {
        defaultOptions = new Options();
      }
      if (isNullOrEmpty(tag)) tag = writeKey;
      Stats stats = new Stats();
      SegmentHTTPApi segmentHTTPApi = new SegmentHTTPApi(writeKey);
      Dispatcher dispatcher = Dispatcher.create(application, maxQueueSize, segmentHTTPApi, stats);
      IntegrationManager integrationManager =
          IntegrationManager.create(application, segmentHTTPApi, stats);
      Traits traits = Traits.forContext(application, tag);
      AnalyticsContext analyticsContext = new AnalyticsContext(application, traits);
      return new Analytics(application, dispatcher, integrationManager, stats, traits,
          analyticsContext, defaultOptions, debugging);
    }
  }

  public static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
        default:
          throw new AssertionError("Unknown handler message received: " + msg.what);
      }
    }
  };

  final Application application;
  final Dispatcher dispatcher;
  final IntegrationManager integrationManager;
  final Stats stats;
  final Traits traits;
  final AnalyticsContext analyticsContext;
  final Options defaultOptions;
  volatile boolean debugging;

  Analytics(Application application, Dispatcher dispatcher, IntegrationManager integrationManager,
      Stats stats, Traits traits, AnalyticsContext analyticsContext, Options defaultOptions,
      boolean debugging) {
    this.application = application;
    this.dispatcher = dispatcher;
    this.integrationManager = integrationManager;
    this.stats = stats;
    this.traits = traits;
    this.analyticsContext = analyticsContext;
    this.defaultOptions = defaultOptions;
    setDebugging(debugging);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      AnalyticsActivityLifecycleCallbacksAdapter.registerActivityLifecycleCallbacks(application,
          this);
    }
  }

  /**
   * Toggle whether debugging is enabled.
   */
  public void setDebugging(boolean enabled) {
    debugging = enabled;
    Logger.setLog(enabled);
  }

  /** {@code true} if debugging is enabled. */
  public boolean isDebugging() {
    return debugging;
  }

  // Activity Lifecycle
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    integrationManager.dispatchOnActivityCreated(activity, savedInstanceState);
  }

  public void onActivityStarted(Activity activity) {
    integrationManager.dispatchOnActivityStarted(activity);
  }

  public void onActivityResumed(Activity activity) {
    integrationManager.dispatchOnActivityResumed(activity);
  }

  public void onActivityPaused(Activity activity) {
    integrationManager.dispatchOnActivityPaused(activity);
  }

  public void onActivityStopped(Activity activity) {
    integrationManager.dispatchOnActivityStopped(activity);
  }

  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    integrationManager.dispatchOnActivitySaveInstanceState(activity, outState);
  }

  public void onActivityDestroyed(Activity activity) {
    integrationManager.dispatchOnActivityDestroyed(activity);
  }

  /**
   * @see {@link #identify(String, Options)}
   */
  public void identify() {
    identify(traits.userId(), defaultOptions);
  }

  /**
   * @see {@link #identify(String, Options)}
   */
  public void identify(String userId) {
    identify(userId, defaultOptions);
  }

  /**
   * Identify lets you tie one of your users and their actions to a recognizable {@code userId}. It
   * also lets you record {@code traits} about the user, like their email, name, account type, etc.
   *
   * @param userId Unique identifier which you recognize a user by in your own database. Must not
   * be null or empty.
   * @param options To configure the call
   * @throws IllegalArgumentException if userId is null or an empty string
   * @see <a href="https://segment.io/docs/tracking-api/identify/">Identify Documentation</a>
   */
  public void identify(String userId, Options options) {
    if (isNullOrEmpty(userId)) {
      throw new IllegalArgumentException("userId must not be null or empty.");
    }
    if (options == null) {
      throw new IllegalArgumentException("options must not be null.");
    }

    traits.putUserId(userId);
    BasePayload payload =
        new IdentifyPayload(traits.anonymousId(), analyticsContext, traits.userId(), traits,
            options, integrationManager.bundledIntegrations());
    submit(payload);
    stats.dispatchIdentify();
  }

  /**
   * @see {@link #group(String, String, Options)}
   */
  public void group(String userId, String groupId) {
    group(userId, groupId, defaultOptions);
  }

  /**
   * The group method lets you associate a user with a group. It also lets you record custom traits
   * about the group, like industry or number of employees.
   * <p/>
   * If you've called {@link #identify(String, Options)} before, this will automatically remember
   * the userId. If not, it will fall back to use the anonymousId instead.
   *
   * @param userId To match up a user with their associated group.
   * @param groupId Unique identifier which you recognize a group by in your own database. Must not
   * be null or empty.
   * @param options To configure the call
   * @throws IllegalArgumentException if groupId is null or an empty string
   * @see <a href=" https://segment.io/docs/tracking-api/group/">Group Documentation</a>
   */
  public void group(String userId, String groupId, Options options) {
    if (isNullOrEmpty(groupId)) {
      throw new IllegalArgumentException("groupId must be null or empty.");
    }
    if (isNullOrEmpty(userId)) {
      userId = traits.userId();
    }
    if (options == null) {
      throw new IllegalArgumentException("options must not be null.");
    }

    BasePayload payload =
        new GroupPayload(traits.anonymousId(), analyticsContext, userId, groupId, traits, options,
            integrationManager.bundledIntegrations());

    submit(payload);
    stats.dispatchGroup();
  }

  /**
   * @see {@link #track(String, Properties, Options)}
   */
  public void track(String event) {
    track(event, new Properties(), defaultOptions);
  }

  /**
   * @see {@link #track(String, Properties, Options)}
   */
  public void track(String event, Properties properties) {
    track(event, properties, defaultOptions);
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
      throw new IllegalArgumentException("event must be null or empty.");
    }
    if (properties == null) {
      throw new IllegalArgumentException("properties must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("options must not be null.");
    }

    BasePayload payload =
        new TrackPayload(traits.anonymousId(), analyticsContext, traits.userId(), event, properties,
            options, integrationManager.bundledIntegrations());
    submit(payload);
    stats.dispatchTrack();
  }

  /**
   * @see {@link #screen(String, String, Properties, Options)}
   */
  public void screen(String category, String name) {
    screen(category, name, new Properties(), defaultOptions);
  }

  /**
   * @see {@link #screen(String, String, Properties, Options)}
   */
  public void screen(String category, String name, Properties properties) {
    screen(category, name, properties, defaultOptions);
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
   * @see <a href="http://segment.io/docs/tracking-api/page-and-screen/">Screen Documentation</a>
   */
  public void screen(String category, String name, Properties properties, Options options) {
    if (isNullOrEmpty(category) && isNullOrEmpty(name)) {
      throw new IllegalArgumentException("either category or name must be provided.");
    }
    if (properties == null) {
      throw new IllegalArgumentException("properties must not be null.");
    }
    if (options == null) {
      throw new IllegalArgumentException("options must not be null.");
    }

    BasePayload payload =
        new ScreenPayload(traits.anonymousId(), analyticsContext, traits.userId(), category, name,
            properties, options, integrationManager.bundledIntegrations());
    submit(payload);
    stats.dispatchScreen();
  }

  /**
   * @see {@link #alias(String, String, Options)}
   */
  public void alias(String newId, String previousId) {
    alias(newId, previousId, defaultOptions);
  }

  /**
   * The alias method is used to merge two user identities, effectively connecting two sets of user
   * data as one. This is an advanced method, but it is required to manage user identities
   * successfully in some of our integrations. You should still call {@link #identify(String,
   * Options)} with {@code newId} if you want to use it as the default id.
   *
   * @param newId The newId to map the old id to. Must not be null to empty.
   * @param previousId The old id we want to map. If it is null, the userId we've cached will
   * automatically used.
   * @param options To configure the call
   * @throws IllegalArgumentException if newId is null or empty
   * @see <a href="https://segment.io/docs/tracking-api/alias/">Alias Documentation</a>
   */
  public void alias(String newId, String previousId, Options options) {
    if (isNullOrEmpty(newId)) {
      throw new IllegalArgumentException("newId must not be null or empty.");
    }
    if (isNullOrEmpty(previousId)) {
      previousId = traits.userId();
    }
    if (options == null) {
      throw new IllegalArgumentException("options must not be null.");
    }

    BasePayload payload =
        new AliasPayload(traits.anonymousId(), analyticsContext, traits.userId(), previousId,
            options, integrationManager.bundledIntegrations());
    submit(payload);
    stats.dispatchAlias();
  }

  /**
   * Tries to send all messages from our queue to our server and indicates our bundled integrations
   * to do the same. Note that not all bundled integrations support this, for which we just no-op.
   */
  public void flush() {
    dispatcher.dispatchFlush();
    integrationManager.dispatchFlush();
  }

  /** Get the {@link Traits} used by this instance. */
  public Traits getTraits() {
    return traits;
  }

  /** Get the {@link AnalyticsContext} used by this instance. */
  public AnalyticsContext getAnalyticsContext() {
    return analyticsContext;
  }

  /**
   * Creates a {@link StatsSnapshot} of the current stats for this instance.
   * <p/>
   * <b>NOTE:</b> The snapshot may not always be completely up-to-date if requests are still in
   * progress.
   */
  @SuppressWarnings("UnusedDeclaration") public StatsSnapshot getSnapshot() {
    return stats.createSnapshot();
  }

  void submit(BasePayload payload) {
    dispatcher.dispatchEnqueue(payload);
    integrationManager.dispatchAnalyticsEvent(payload);
  }
}
