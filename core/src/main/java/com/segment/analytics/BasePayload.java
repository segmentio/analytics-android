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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.UUID;

import static com.segment.analytics.Options.ALL_INTEGRATIONS_KEY;
import static com.segment.analytics.Utils.toISO8601Date;

/**
 * A payload object that will be sent to the server. Clients will not decode instances of this
 * directly, but through one if it's subclasses.
 */
// This ignores projectId, receivedAt and version that are set by the server.
// sentAt is set on SegmentHTTPApi#BatchPayload
abstract class BasePayload extends ValueMap implements IntegrationManager.IntegrationOperation {
  /** The type of message. */
  private static final String TYPE_KEY = "type";
  /**
   * The anonymous ID is an identifier that uniquely (or close enough) identifies the user, but
   * isn't from your database. This is useful in cases where you are able to uniquely identifier
   * the
   * user between visits before they signup thanks to a cookie, or session ID or device ID. In our
   * mobile and browser libraries we will automatically handle sending the anonymous ID.
   */
  private static final String ANONYMOUS_ID_KEY = "anonymousId";
  /**
   * The channel where the request originated from: server, browser or mobile. In the future we may
   * add additional channels as we add libraries, for example console.
   * <p/>
   * This is always {@link Channel#mobile} for us.
   */
  private static final String CHANNEL_KEY = "channel";
  /**
   * A randomly generated unique id for this message. Used to track payload lifecycle.
   */
  private static final String MESSAGE_ID = "messageId";
  /**
   * The context is a dictionary of extra information that provides useful context about a message,
   * for example ip address or locale. This dictionary is loosely speced, but you can also add your
   * own context, for example app.name or app.version. Check out the existing spec'ed properties in
   * the context before adding your own.
   */
  private static final String CONTEXT_KEY = "context";
  /**
   * A dictionary of integration names that the message should be proxied to. 'All' is a special
   * name that applies when no key for a specific integration is found, and is case-insensitive.
   */
  private static final String INTEGRATIONS_KEY = "integrations";
  /** The timestamp when the message took place. This should be an ISO-8601-formatted string. */
  private static final String TIMESTAMP_KEY = "timestamp";
  /**
   * The user ID is an identifier that unique identifies the user in your database. Ideally it
   * should not be an email address, because emails can change, whereas a database ID can't.
   */
  private static final String USER_ID_KEY = "userId";

  BasePayload(Type type, String anonymousId, AnalyticsContext context, String userId,
      Options options) {
    put(MESSAGE_ID, UUID.randomUUID().toString());
    put(TYPE_KEY, type);
    put(CHANNEL_KEY, Channel.mobile);
    put(ANONYMOUS_ID_KEY, anonymousId);
    put(CONTEXT_KEY, context.unmodifiableCopy());
    put(USER_ID_KEY, userId);
    put(TIMESTAMP_KEY, options.timestamp() == null ? toISO8601Date(new Date())
        : toISO8601Date(options.timestamp()));
    put(INTEGRATIONS_KEY, new LinkedHashMap<String, Boolean>(options.integrations())); // copy
  }

  private ValueMap integrations() {
    return getValueMap(INTEGRATIONS_KEY);
  }

  Type type() {
    return getEnum(Type.class, TYPE_KEY);
  }

  String userId() {
    return getString(USER_ID_KEY);
  }

  AnalyticsContext context() {
    return getValueMap(CONTEXT_KEY, AnalyticsContext.class);
  }

  @Override BasePayload putValue(String key, Object value) {
    super.putValue(key, value);
    return this;
  }

  @Override public String id() {
    return getString(MESSAGE_ID);
  }

  boolean isIntegrationEnabledInPayload(AbstractIntegration integration) {
    boolean enabled = true;
    ValueMap integrations = integrations();
    String key = integration.key();
    if (integrations.containsKey(key)) {
      enabled = integrations.getBoolean(key, true);
    } else if (integrations.containsKey(ALL_INTEGRATIONS_KEY)) {
      enabled = integrations.getBoolean(ALL_INTEGRATIONS_KEY, true);
    }
    return enabled;
  }

  /** @see #TYPE_KEY */
  enum Type {
    alias, group, identify, screen, track
  }

  /** @see #CHANNEL_KEY */
  private enum Channel {
    browser, mobile, server
  }
}
