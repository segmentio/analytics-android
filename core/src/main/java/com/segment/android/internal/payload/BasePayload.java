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

package com.segment.android.internal.payload;

import com.segment.android.AnalyticsContext;
import com.segment.android.Options;
import com.segment.android.internal.ISO8601Time;
import com.segment.android.json.JsonMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A payload object that will be sent to the server. Clients will not decode instances of this
 * directly, but through one if it's subclasses.
 */
/* This ignores projectId, receivedAt, messageId, sentAt, version that are set by the server. */
public class BasePayload extends JsonMap {
  enum Type {
    alias, group, identify, page, screen, track
  }

  private enum Channel {
    browser, mobile, server
  }

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

  /**
   * The sent timestamp is an ISO-8601-formatted string that, if present on a message, can be used
   * to correct the original timestamp in situations where the local clock cannot be trusted, for
   * example in our mobile libraries. The sentAt and receivedAt timestamps will be assumed to have
   * occurred at the same time, and therefore the difference is the local clock skew.
   */
  private static final String SENT_AT_KEY = "sentAt";

  /** The timestamp when the message took place. This should be an ISO-8601-formatted string. */
  private static final String TIMESTAMP_KEY = "timestamp";

  /**
   * The user ID is an identifier that unique identifies the user in your database. Ideally it
   * should not be an email address, because emails can change, whereas a database ID can't.
   */
  private static final String USER_ID_KEY = "userId";

  public BasePayload(Type type, String anonymousId, AnalyticsContext context, String userId,
      Options options, Map<String, Boolean> bundledIntegrations) {
    put(TYPE_KEY, type.toString());
    put(CHANNEL_KEY, Channel.mobile.toString());
    put(ANONYMOUS_ID_KEY, anonymousId);

    // Top level integrations are used by servers, this is a combination of disabled bundled
    // integrations, and anything the user may have passed in
    HashMap<String, Boolean> serverIntegrations = new LinkedHashMap<String, Boolean>();
    serverIntegrations.putAll(options.getIntegrations());
    serverIntegrations.putAll(bundledIntegrations);
    put(INTEGRATIONS_KEY, serverIntegrations);
    // Context level integrations are used by IntegrationManger, this is simply what the user may
    // have passed in, used to disable integrations for specific events. Could be a bundled one,
    // which we'll skip locally, or a server one, which we'll pass on to the server
    context.putIntegrations(options.getIntegrations());

    put(CONTEXT_KEY, context);
    put(USER_ID_KEY, userId);
    put(TIMESTAMP_KEY, options.getTimestamp() == null ? ISO8601Time.now().toString()
        : ISO8601Time.from(options.getTimestamp()).toString());
  }

  public BasePayload(String json) {
    super(json);
  }

  public String type() {
    return getString(TYPE_KEY);
  }

  public String userId() {
    return getString(USER_ID_KEY);
  }

  public AnalyticsContext context() {
    return new AnalyticsContext(getJsonMap(CONTEXT_KEY));
  }

  public void setSentAt(ISO8601Time time) {
    put(SENT_AT_KEY, time.toString());
  }

  @Override public BasePayload putValue(String key, Object value) {
    super.putValue(key, value);
    return this;
  }
}