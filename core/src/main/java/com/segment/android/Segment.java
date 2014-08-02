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
import java.util.LinkedHashMap;
import java.util.Map;

import static com.segment.android.Asserts.assertOnMainThread;
import static com.segment.android.ResourceUtils.getBooleanOrThrow;
import static com.segment.android.ResourceUtils.getIntegerOrThrow;
import static com.segment.android.ResourceUtils.getString;
import static com.segment.android.Utils.hasPermission;
import static com.segment.android.Utils.isNullOrEmpty;

public class Segment {
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
          singleton = new Builder(context).build();
        }
      }
    }
    return singleton;
  }

  /** Fluent API for creating {@link Segment} instances. */
  @SuppressWarnings("UnusedDeclaration") // Public API.
  public static class Builder {
    private final Application application;
    private String writeKey;
    // Use Boxed types to be able to see what has been defined and what has not
    private Integer queueSize;
    private Boolean debugging;

    // Defaults
    public static final int DEFAULT_QUEUE_SIZE = 20;
    public static final boolean DEFAULT_DEBUGGING = false;

    // Resource identifier to define options in xml
    public static final String WRITE_KEY_RESOURCE_IDENTIFIER = "analytics_write_key";
    public static final String QUEUE_SIZE_RESOURCE_IDENTIFIER = "analytics_queue_size";
    public static final String DEBUGGING_RESOURCE_IDENTIFIER = "analytics_debug";

    /** Start building a new {@link Segment} instance. */
    public Builder(Context context) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      if (!hasPermission(context, Manifest.permission.INTERNET)) {
        throw new IllegalArgumentException("INTERNET permission is required.");
      }
      if (!hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
        // todo: do we really need this permission?
        throw new IllegalArgumentException("ACCESS_NETWORK_STATE permission is required.");
      }

      application = (Application) context.getApplicationContext();
    }

    /**
     * Set the write api key for Segment.io.
     */
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

    /**
     * Set the size of the queue to batch events.
     */
    public Builder queueSize(int queueSize) {
      if (queueSize <= 0) {
        throw new IllegalArgumentException("queueSize must be greater than or equal to zero.");
      }
      if (this.queueSize != null) {
        throw new IllegalStateException("queueSize is already set.");
      }
      this.queueSize = queueSize;
      return this;
    }

    /**
     * Whether debugging is enabled or not.
     */
    public Builder debugging(boolean debugging) {
      this.debugging = debugging;
      return this;
    }

    /** Create Segment {@link Segment} instance. */
    public Segment build() {
      if (writeKey == null) {
        writeKey = getString(application, WRITE_KEY_RESOURCE_IDENTIFIER);
        if (isNullOrEmpty(writeKey)) {
          throw new IllegalStateException("apiKey must be provided defined in analytics.xml");
        }
      }

      if (queueSize == null) {
        try {
          queueSize = getIntegerOrThrow(application, QUEUE_SIZE_RESOURCE_IDENTIFIER);
          if (queueSize <= 0) {
            throw new IllegalStateException(
                "queueSize(" + queueSize + ") may not be zero or negative.");
          }
        } catch (Resources.NotFoundException e) {
          // when queueSize is not defined in xml, we'll use a default option
          queueSize = DEFAULT_QUEUE_SIZE;
        }
      }

      if (debugging == null) {
        try {
          debugging = getBooleanOrThrow(application, DEBUGGING_RESOURCE_IDENTIFIER);
        } catch (Resources.NotFoundException e) {
          // when debugging is not defined in xml, we'll use a default option
          debugging = DEFAULT_DEBUGGING;
        }
      }

      SegmentHTTPApi segmentHTTPApi = SegmentHTTPApi.create(writeKey);
      Dispatcher dispatcher = Dispatcher.create(application, HANDLER, queueSize, segmentHTTPApi);

      return new Segment(application, dispatcher, debugging);
    }
  }

  static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
        default:
          throw new AssertionError("Unknown handler message received: " + msg.what);
      }
    }
  };

  final Application application;
  final Dispatcher dispatcher;
  volatile boolean debugging;
  final String anonymousId;

  Segment(Application application, Dispatcher dispatcher, boolean debugging) {
    this.application = application;
    this.dispatcher = dispatcher;
    this.debugging = debugging;
    this.anonymousId = Utils.getDeviceId(application);

    // Initialize the AnalyticsContext, which will initialize Traits automatically
    AnalyticsContext.with(application);
    Logger.setLog(debugging);
  }

  /**
   * Toggle whether debugging is enabled.
   * <p/>
   * <b>WARNING:</b>This should be only be used for debugging behavior. Do NOT pass {@code
   * BuildConfig.DEBUG}.
   */
  public void setDebugging(boolean enabled) {
    debugging = enabled;
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

    Traits.with(application).putId(userId);

    submit(new IdentifyPayload(anonymousId, AnalyticsContext.with(application),
        generateServerIntegrations(), userId, Traits.with(application)));
  }

  public void group(String groupId) {
    assertOnMainThread();

    if (isNullOrEmpty(groupId)) {
      throw new IllegalArgumentException("groupId must be null or empty.");
    }

    submit(new GroupPayload(anonymousId, AnalyticsContext.with(application),
        generateServerIntegrations(), Traits.with(application).getId(), groupId,
        Traits.with(application)));
  }

  public void track(String event, Properties properties, Options options) {
    assertOnMainThread();

    if (isNullOrEmpty(event)) {
      throw new IllegalArgumentException("event must be null or empty.");
    }
    if (properties == null) {
      properties = new Properties();
    }

    submit(new TrackPayload(anonymousId, AnalyticsContext.with(application),
        generateServerIntegrations(), Traits.with(application).getId(), event, properties));
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

    submit(new ScreenPayload(anonymousId, AnalyticsContext.with(application),
        generateServerIntegrations(), Traits.with(application).getId(), category, name,
        properties));
  }

  public void alias(String newId, Options options) {
    assertOnMainThread();

    if (isNullOrEmpty(newId)) {
      throw new IllegalArgumentException("newId must not be null or empty.");
    }

    String previousId = Traits.with(application).getId(); // copy the previousId
    Traits.with(application).putId(newId); // update the new id

    submit(new AliasPayload(anonymousId, AnalyticsContext.with(application),
        generateServerIntegrations(), Traits.with(application).getId(), previousId));
  }

  Map<String, Boolean> generateServerIntegrations() {
    // todo: implement
    return new LinkedHashMap<String, Boolean>();
  }

  void submit(Payload payload) {
    dispatcher.dispatchEnqueue(payload);
  }
}
