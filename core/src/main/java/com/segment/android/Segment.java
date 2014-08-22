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

package com.segment.android;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.segment.android.internal.Dispatcher;
import com.segment.android.internal.IntegrationManager;
import com.segment.android.internal.Logger;
import com.segment.android.internal.SegmentHTTPApi;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.BasePayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;

import static com.segment.android.internal.Utils.assertOnMainThread;
import static com.segment.android.internal.Utils.getDeviceId;
import static com.segment.android.internal.Utils.getResourceBooleanOrThrow;
import static com.segment.android.internal.Utils.getResourceIntegerOrThrow;
import static com.segment.android.internal.Utils.getResourceString;
import static com.segment.android.internal.Utils.hasPermission;
import static com.segment.android.internal.Utils.isNullOrEmpty;

/**
 * The idea is simple: one pipeline for all your data.
 *
 * <p>
 * Use {@link #with(android.content.Context)} for the global singleton instance or construct your
 * own instance with {@link Builder}.
 *
 * @see {@link https://segment.io/}
 */
public class Segment {
  // Resource identifiers to define options in xml
  public static final String WRITE_KEY_RESOURCE_IDENTIFIER = "analytics_write_key";
  public static final String QUEUE_SIZE_RESOURCE_IDENTIFIER = "analytics_queue_size";
  public static final String DEBUGGING_RESOURCE_IDENTIFIER = "analytics_debug";

  static Segment singleton = null;

  /**
   * The global default {@link Segment} instance.
   * <p/>
   * This instance is automatically initialized with defaults that are suitable to most
   * implementations.
   * <p/>
   * If these settings do not meet the requirements of your application, you can provide
   * properties in {@code analytics.xml} or you can construct your own instance with full control
   * over the configuration by using {@link Builder}.
   */
  public static Segment with(Context context) {
    if (singleton == null) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      synchronized (Segment.class) {
        if (singleton == null) {
          String writeKey = getResourceString(context, WRITE_KEY_RESOURCE_IDENTIFIER);
          Builder builder = new Builder(context, writeKey);
          try {
            // We need the exception to be able to tell if this was not defined, or if it was
            // incorrectly defined - something we shouldn't ignore
            int maxQueueSize = getResourceIntegerOrThrow(context, QUEUE_SIZE_RESOURCE_IDENTIFIER);
            if (maxQueueSize <= 0) {
              throw new IllegalStateException(
                  "analytics_queue_size(" + maxQueueSize + ") may not be zero or negative.");
            }
            builder.maxQueueSize(maxQueueSize);
          } catch (Resources.NotFoundException e) {
            // when maxQueueSize is not defined in xml, we'll use a default option in the builder
          }
          try {
            boolean debugging = getResourceBooleanOrThrow(context, DEBUGGING_RESOURCE_IDENTIFIER);
            builder.debugging(debugging);
          } catch (Resources.NotFoundException e) {
            // when debugging is not defined in xml, we'll use a default value from the builder
          }
          singleton = builder.build();
        }
      }
    }
    return singleton;
  }

  /** Fluent API for creating {@link Segment} instances. */
  @SuppressWarnings("UnusedDeclaration") // Public API.
  public static class Builder {
    public static final int DEFAULT_QUEUE_SIZE = 20;
    public static final boolean DEFAULT_DEBUGGING = false;

    private final Application application;
    private String writeKey;
    private int maxQueueSize = -1;

    private boolean debugging = DEFAULT_DEBUGGING;

    /** Start building a new {@link Segment} instance. */
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

    /** Set the size of the queue to batch events. */
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

    /** Set whether debugging is enabled or not. */
    public Builder debugging(boolean debugging) {
      this.debugging = debugging;
      return this;
    }

    /** Create Segment {@link Segment} instance. */
    public Segment build() {
      if (maxQueueSize == -1) {
        maxQueueSize = DEFAULT_QUEUE_SIZE;
      }
      SegmentHTTPApi segmentHTTPApi = new SegmentHTTPApi(writeKey);
      Dispatcher dispatcher = Dispatcher.create(application, HANDLER, maxQueueSize, segmentHTTPApi);
      IntegrationManager integrationManager =
          new IntegrationManager(application, HANDLER, segmentHTTPApi);
      return new Segment(application, dispatcher, integrationManager, debugging);
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
  volatile boolean debugging;
  final String anonymousId;

  Segment(Application application, Dispatcher dispatcher, IntegrationManager integrationManager,
      boolean debugging) {
    this.application = application;
    this.dispatcher = dispatcher;
    this.integrationManager = integrationManager;
    setDebugging(debugging);
    anonymousId = getDeviceId(application);
    AnalyticsContext.with(application);
    Traits.with(application);
  }

  /**
   * Toggle whether debugging is enabled.
   * <p/>
   * <b>WARNING:</b>This should be only be used for debugging behavior. Do NOT pass {@code
   * BuildConfig.DEBUG}.
   */
  public void setDebugging(boolean enabled) {
    debugging = enabled;
    Logger.setLog(enabled);
  }

  /** {@code true} if debugging is enabled. */
  public boolean isDebugging() {
    return debugging;
  }

  public void identify(String userId, Options options) {
    assertOnMainThread();

    if (isNullOrEmpty(userId)) {
      throw new IllegalArgumentException("userId must be null or empty.");
    }

    if (options == null) {
      options = new Options();
    }

    Traits.with(application).putId(userId);

    submit(new IdentifyPayload(anonymousId, AnalyticsContext.with(application), userId,
        Traits.with(application), options, integrationManager.bundledIntegrations()));
  }

  public void group(String groupId, Options options) {
    assertOnMainThread();

    if (isNullOrEmpty(groupId)) {
      throw new IllegalArgumentException("groupId must be null or empty.");
    }

    if (options == null) {
      options = new Options();
    }

    submit(new GroupPayload(anonymousId, AnalyticsContext.with(application),
        Traits.with(application).id(), groupId, Traits.with(application), options,
        integrationManager.bundledIntegrations()));
  }

  public void track(String event, Properties properties, Options options) {
    assertOnMainThread();

    if (isNullOrEmpty(event)) {
      throw new IllegalArgumentException("event must be null or empty.");
    }

    if (properties == null) {
      properties = new Properties();
    }
    if (options == null) {
      options = new Options();
    }

    TrackPayload payload = new TrackPayload(anonymousId, AnalyticsContext.with(application),
        Traits.with(application).id(), event, properties, options,
        integrationManager.bundledIntegrations());

    submit(payload);
    integrationManager.track(payload);
  }

  public void screen(String category, String name, Properties properties, Options options) {
    assertOnMainThread();

    if (isNullOrEmpty(category) && isNullOrEmpty(name)) {
      throw new IllegalArgumentException(
          "either one of category or name must not be null or empty.");
    }

    if (properties == null) {
      properties = new Properties();
    }
    if (options == null) {
      options = new Options();
    }

    submit(new ScreenPayload(anonymousId, AnalyticsContext.with(application),
        Traits.with(application).id(), category, name, properties, options,
        integrationManager.bundledIntegrations()));
  }

  public void alias(String newId, String previousId, Options options) {
    assertOnMainThread();

    if (isNullOrEmpty(newId)) {
      throw new IllegalArgumentException("newId must not be null or empty.");
    }

    if (isNullOrEmpty(previousId)) {
      previousId = Traits.with(application).id(); // copy the previousId
    }
    if (options == null) {
      options = new Options();
    }

    Traits.with(application).putId(newId); // update the new id

    submit(new AliasPayload(anonymousId, AnalyticsContext.with(application),
        Traits.with(application).id(), previousId, options,
        integrationManager.bundledIntegrations()));
  }

  public void flush() {
    dispatcher.dispatchFlush();
    // todo: flush integration manager
  }

  void submit(BasePayload payload) {
    dispatcher.dispatchEnqueue(payload);
  }
}
