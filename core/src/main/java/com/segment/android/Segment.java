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
import com.segment.android.internal.SegmentHTTPApi;
import com.segment.android.internal.payload.AliasPayload;
import com.segment.android.internal.payload.BasePayload;
import com.segment.android.internal.payload.GroupPayload;
import com.segment.android.internal.payload.IdentifyPayload;
import com.segment.android.internal.payload.ScreenPayload;
import com.segment.android.internal.payload.TrackPayload;
import com.segment.android.internal.util.Logger;

import static com.segment.android.internal.Asserts.assertOnMainThread;
import static com.segment.android.internal.util.ResourceUtils.getBooleanOrThrow;
import static com.segment.android.internal.util.ResourceUtils.getIntegerOrThrow;
import static com.segment.android.internal.util.ResourceUtils.getString;
import static com.segment.android.internal.util.Utils.getDeviceId;
import static com.segment.android.internal.util.Utils.hasPermission;
import static com.segment.android.internal.util.Utils.isNullOrEmpty;

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
   * If these settings do not meet the requirements of your application you can construct your own
   * instance with full control over the configuration by using {@link Builder}.
   */
  public static Segment with(Context context) {
    if (singleton == null) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      synchronized (Segment.class) {
        if (singleton == null) {
          Builder builder = new Builder(context);

          String writeKey = getString(context, WRITE_KEY_RESOURCE_IDENTIFIER);
          builder.writeKey(writeKey);

          try {
            // We need the exception to be able to tell if this was not defined, or if it was
            // incorrectly defined - something we shouldn't ignore
            int maxQueueSize = getIntegerOrThrow(context, QUEUE_SIZE_RESOURCE_IDENTIFIER);
            if (maxQueueSize <= 0) {
              throw new IllegalStateException(
                  "maxQueueSize(" + maxQueueSize + ") may not be zero or negative.");
            }
            builder.maxQueueSize(maxQueueSize);
          } catch (Resources.NotFoundException e) {
            // when maxQueueSize is not defined in xml, we'll use a default option in the builder
          }

          try {
            boolean debugging = getBooleanOrThrow(context, DEBUGGING_RESOURCE_IDENTIFIER);
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
    // Defaults
    public static final int DEFAULT_QUEUE_SIZE = 20;
    public static final boolean DEFAULT_DEBUGGING = false;

    private final Application application;
    private String writeKey;
    private int maxQueueSize = -1;
    private boolean debugging = DEFAULT_DEBUGGING;

    /** Start building a new {@link Segment} instance. */
    public Builder(Context context) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      if (!hasPermission(context, Manifest.permission.INTERNET)) {
        throw new IllegalArgumentException("INTERNET permission is required.");
      }

      application = (Application) context.getApplicationContext();
    }

    /** Set the write api key for Segment.io */
    public Builder writeKey(String writeKey) {
      if (isNullOrEmpty(writeKey)) {
        throw new IllegalArgumentException("writeKey must not be null.");
      }
      if (this.writeKey != null) {
        throw new IllegalStateException("writeKey is already set.");
      }
      this.writeKey = writeKey;
      return this;
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

    /** Whether debugging is enabled or not. */
    public Builder debugging(boolean debugging) {
      this.debugging = debugging;
      return this;
    }

    /** Create Segment {@link Segment} instance. */
    public Segment build() {
      if (isNullOrEmpty(writeKey)) {
        throw new IllegalStateException("apiKey must be provided defined in analytics.xml");
      }

      if (maxQueueSize == -1) {
        maxQueueSize = DEFAULT_QUEUE_SIZE;
      }

      SegmentHTTPApi segmentHTTPApi = SegmentHTTPApi.create(writeKey);
      Dispatcher dispatcher = Dispatcher.create(application, HANDLER, maxQueueSize, segmentHTTPApi);
      IntegrationManager integrationManager =
          IntegrationManager.create(application, HANDLER, segmentHTTPApi);
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

    submit(new IdentifyPayload(anonymousId,
        AnalyticsContext.with(application).putTraits(Traits.with(application)), userId,
        Traits.with(application), options));
  }

  public void group(String groupId, Options options) {
    assertOnMainThread();

    if (options == null) {
      options = new Options();
    }

    if (isNullOrEmpty(groupId)) {
      throw new IllegalArgumentException("groupId must be null or empty.");
    }
    submit(new GroupPayload(anonymousId,
        AnalyticsContext.with(application).putTraits(Traits.with(application)),
        Traits.with(application).getId(), groupId, Traits.with(application), options));
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

    submit(new TrackPayload(anonymousId,
        AnalyticsContext.with(application).putTraits(Traits.with(application)),
        Traits.with(application).getId(), event, properties, options));
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

    submit(new ScreenPayload(anonymousId,
        AnalyticsContext.with(application).putTraits(Traits.with(application)),
        Traits.with(application).getId(), category, name, properties, options));
  }

  public void alias(String newId, String previousId, Options options) {
    assertOnMainThread();

    if (isNullOrEmpty(newId)) {
      throw new IllegalArgumentException("newId must not be null or empty.");
    }

    if (isNullOrEmpty(previousId)) {
      previousId = Traits.with(application).getId(); // copy the previousId
    }
    if (options == null) {
      options = new Options();
    }

    Traits.with(application).putId(newId); // update the new id

    submit(new AliasPayload(anonymousId,
        AnalyticsContext.with(application).putTraits(Traits.with(application)),
        Traits.with(application).getId(), previousId, options));
  }

  public void flush() {
    dispatcher.dispatchFlush();
    // todo: flush integration manager
  }

  void submit(BasePayload payload) {
    dispatcher.dispatchEnqueue(payload);
  }
}
