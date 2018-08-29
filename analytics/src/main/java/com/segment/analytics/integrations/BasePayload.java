/**
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
package com.segment.analytics.integrations;

import static com.segment.analytics.internal.Utils.assertNotNull;
import static com.segment.analytics.internal.Utils.assertNotNullOrEmpty;
import static com.segment.analytics.internal.Utils.immutableCopyOf;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.parseISO8601Date;
import static com.segment.analytics.internal.Utils.toISO8601String;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.ValueMap;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A payload object that will be sent to the server. Clients will not decode instances of this
 * directly, but through one if it's subclasses.
 */
// This ignores projectId, receivedAt and version that are set by the server.
// sentAt is set on SegmentClient#BatchPayload
public abstract class BasePayload extends ValueMap {

  static final String TYPE_KEY = "type";
  static final String ANONYMOUS_ID_KEY = "anonymousId";
  static final String CHANNEL_KEY = "channel";
  static final String MESSAGE_ID = "messageId";
  static final String CONTEXT_KEY = "context";
  static final String INTEGRATIONS_KEY = "integrations";
  static final String TIMESTAMP_KEY = "timestamp";
  static final String USER_ID_KEY = "userId";

  BasePayload(
      @NonNull Type type,
      @NonNull String messageId,
      @NonNull Date timestamp,
      @NonNull Map<String, Object> context,
      @NonNull Map<String, Object> integrations,
      @Nullable String userId,
      @NonNull String anonymousId) {
    put(CHANNEL_KEY, Channel.mobile);
    put(TYPE_KEY, type);
    put(MESSAGE_ID, messageId);
    put(TIMESTAMP_KEY, toISO8601String(timestamp));
    put(CONTEXT_KEY, context);
    put(INTEGRATIONS_KEY, integrations);
    if (!isNullOrEmpty(userId)) {
      put(USER_ID_KEY, userId);
    }
    put(ANONYMOUS_ID_KEY, anonymousId);
  }

  /** The type of message. */
  @NonNull
  public Type type() {
    return getEnum(Type.class, TYPE_KEY);
  }

  /**
   * The user ID is an identifier that unique identifies the user in your database. Ideally it
   * should not be an email address, because emails can change, whereas a database ID can't.
   */
  @Nullable
  public String userId() {
    return getString(USER_ID_KEY);
  }

  /**
   * The anonymous ID is an identifier that uniquely (or close enough) identifies the user, but
   * isn't from your database. This is useful in cases where you are able to uniquely identifier the
   * user between visits before they sign up thanks to a cookie, or session ID or device ID. In our
   * mobile and browser libraries we will automatically handle sending the anonymous ID.
   */
  @NonNull
  public String anonymousId() {
    return getString(ANONYMOUS_ID_KEY);
  }

  /** A randomly generated unique id for this message. */
  @NonNull
  public String messageId() {
    return getString(MESSAGE_ID);
  }

  /**
   * Set a timestamp the event occurred.
   *
   * <p>This library will automatically create and attach a timestamp to all events.
   *
   * @see <a href="https://segment.com/docs/spec/common/#timestamps">Timestamp</a>
   */
  @Nullable
  public Date timestamp() {
    // It's unclear if this will ever be null. So we're being safe.
    String timestamp = getString(TIMESTAMP_KEY);
    if (isNullOrEmpty(timestamp)) {
      return null;
    }
    return parseISO8601Date(timestamp);
  }

  /**
   * A dictionary of integration names that the message should be proxied to. 'All' is a special
   * name that applies when no key for a specific integration is found, and is case-insensitive.
   */
  public ValueMap integrations() {
    return getValueMap(INTEGRATIONS_KEY);
  }

  /**
   * The context is a dictionary of extra information that provides useful context about a message,
   * for example ip address or locale.
   *
   * @see <a href="https://segment.com/docs/spec/common/#context">Context fields</a>
   */
  public AnalyticsContext context() {
    return getValueMap(CONTEXT_KEY, AnalyticsContext.class);
  }

  @Override
  public BasePayload putValue(String key, Object value) {
    super.putValue(key, value);
    return this;
  }

  @NonNull
  public abstract Builder toBuilder();

  /** @see #TYPE_KEY */
  public enum Type {
    alias,
    group,
    identify,
    screen,
    track
  }

  /**
   * The channel where the request originated from: server, browser or mobile. In the future we may
   * add additional channels as we add libraries, for example console.
   *
   * <p>This is always {@link Channel#mobile} for us.
   */
  public enum Channel {
    browser,
    mobile,
    server
  }

  public abstract static class Builder<P extends BasePayload, B extends Builder> {

    private String messageId;
    private Date timestamp;
    private Map<String, Object> context;
    private Map<String, Object> integrationsBuilder;
    private String userId;
    private String anonymousId;

    Builder() {
      // Empty constructor.
    }

    Builder(BasePayload payload) {
      messageId = payload.messageId();
      timestamp = payload.timestamp();
      context = payload.context();
      integrationsBuilder = new LinkedHashMap<>(payload.integrations());
      userId = payload.userId();
      anonymousId = payload.anonymousId();
    }

    /**
     * The Message ID is a unique identifier for each message. If not provided, one will be
     * generated for you. This ID is typically used for deduping - messages with the same IDs as
     * previous events may be dropped.
     *
     * @see <a href="https://segment.com/docs/spec/common/">Common Fields</a>
     */
    @NonNull
    public B messageId(@NonNull String messageId) {
      assertNotNullOrEmpty(messageId, "messageId");
      this.messageId = messageId;
      return self();
    }

    /**
     * Set a timestamp for the event. By default, the current timestamp is used, but you may
     * override it for historical import.
     *
     * <p>This library will automatically create and attach a timestamp to all events.
     *
     * @see <a href="https://segment.com/docs/spec/common/#timestamps">Timestamp</a>
     */
    @NonNull
    public B timestamp(@NonNull Date timestamp) {
      assertNotNull(timestamp, "timestamp");
      this.timestamp = timestamp;
      return self();
    }

    /**
     * Set a map of information about the state of the device. You can add any custom data to the
     * context dictionary that you'd like to have access to in the raw logs.
     *
     * <p>Some keys in the context dictionary have semantic meaning and will be collected for you
     * automatically, depending on the library you send data from. Some keys, such as location and
     * speed need to be manually entered.
     *
     * @see <a href="https://segment.com/docs/spec/common/#context">Context</a>
     */
    @NonNull
    public B context(@NonNull Map<String, ?> context) {
      assertNotNull(context, "context");
      this.context = Collections.unmodifiableMap(new LinkedHashMap<>(context));
      return self();
    }

    /**
     * Set whether this message is sent to the specified integration or not. 'All' is a special key
     * that applies when no key for a specific integration is found.
     *
     * @see <a href="https://segment.com/docs/spec/common/#integrations">Integrations</a>
     */
    @NonNull
    public B integration(@NonNull String key, boolean enable) {
      assertNotNullOrEmpty(key, "key");
      if (integrationsBuilder == null) {
        integrationsBuilder = new LinkedHashMap<>();
      }
      integrationsBuilder.put(key, enable);
      return self();
    }

    /**
     * Pass in some options that will only be used by the target integration. This will implicitly
     * mark the integration as enabled.
     *
     * @see <a href="https://segment.com/docs/spec/common/#integrations">Integrations</a>
     */
    @NonNull
    public B integration(@NonNull String key, @NonNull Map<String, Object> options) {
      assertNotNullOrEmpty(key, "key");
      assertNotNullOrEmpty(options, "options");
      if (integrationsBuilder == null) {
        integrationsBuilder = new LinkedHashMap<>();
      }
      integrationsBuilder.put(key, immutableCopyOf(options));
      return self();
    }

    /**
     * Specify a dictionary of options for integrations.
     *
     * @see <a href="https://segment.com/docs/spec/common/#integrations">Integrations</a>
     */
    @NonNull
    public B integrations(@Nullable Map<String, ?> integrations) {
      if (isNullOrEmpty(integrations)) {
        return self();
      }
      if (integrationsBuilder == null) {
        integrationsBuilder = new LinkedHashMap<>();
      }
      integrationsBuilder.putAll(integrations);
      return self();
    }

    /**
     * The Anonymous ID is a pseudo-unique substitute for a User ID, for cases when you don't have
     * an absolutely unique identifier.
     *
     * @see <a href="https://segment.com/docs/spec/identify/#identities">Identities</a>
     * @see <a href="https://segment.com/docs/spec/identify/#anonymous-id">Anonymous ID</a>
     */
    @NonNull
    public B anonymousId(@NonNull String anonymousId) {
      this.anonymousId = assertNotNullOrEmpty(anonymousId, "anonymousId");
      return self();
    }

    /**
     * The User ID is a persistent unique identifier for a user (such as a database ID).
     *
     * @see <a href="https://segment.com/docs/spec/identify/#identities">Identities</a>
     * @see <a href="https://segment.com/docs/spec/identify/#user-id">User ID</a>
     */
    @NonNull
    public B userId(@NonNull String userId) {
      this.userId = assertNotNullOrEmpty(userId, "userId");
      return self();
    }

    abstract P realBuild(
        @NonNull String messageId,
        @NonNull Date timestamp,
        @NonNull Map<String, Object> context,
        @NonNull Map<String, Object> integrations,
        @Nullable String userId,
        @NonNull String anonymousId);

    abstract B self();

    /** Create a {@link BasePayload} instance. */
    @CheckResult
    @NonNull
    public P build() {
      if (isNullOrEmpty(userId) && isNullOrEmpty(anonymousId)) {
        throw new NullPointerException("either userId or anonymousId is required");
      }

      Map<String, Object> integrations =
          isNullOrEmpty(integrationsBuilder)
              ? Collections.<String, Object>emptyMap()
              : immutableCopyOf(integrationsBuilder);

      if (isNullOrEmpty(messageId)) {
        messageId = UUID.randomUUID().toString();
      }

      if (timestamp == null) {
        timestamp = new Date();
      }

      if (isNullOrEmpty(context)) {
        context = Collections.emptyMap();
      }

      return realBuild(messageId, timestamp, context, integrations, userId, anonymousId);
    }
  }
}
