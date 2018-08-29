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
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.segment.analytics.Properties;
import com.segment.analytics.internal.Private;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class TrackPayload extends BasePayload {

  static final String EVENT_KEY = "event";
  static final String PROPERTIES_KEY = "properties";

  @Private
  TrackPayload(
      @NonNull String messageId,
      @NonNull Date timestamp,
      @NonNull Map<String, Object> context,
      @NonNull Map<String, Object> integrations,
      @Nullable String userId,
      @NonNull String anonymousId,
      @NonNull String event,
      @NonNull Map<String, Object> properties) {
    super(Type.track, messageId, timestamp, context, integrations, userId, anonymousId);
    put(EVENT_KEY, event);
    put(PROPERTIES_KEY, properties);
  }

  /**
   * The name of the event. We recommend using title case and past tense for event names, like
   * "Signed Up".
   */
  @NonNull
  public String event() {
    return getString(EVENT_KEY);
  }

  /**
   * A dictionary of properties that give more information about the event. We have a collection of
   * special properties that we recognize with semantic meaning. You can also add your own custom
   * properties.
   */
  @NonNull
  public Properties properties() {
    return getValueMap(PROPERTIES_KEY, Properties.class);
  }

  @Override
  public String toString() {
    return "TrackPayload{event=\"" + event() + "\"}";
  }

  @NonNull
  @Override
  public TrackPayload.Builder toBuilder() {
    return new Builder(this);
  }

  /** Fluent API for creating {@link TrackPayload} instances. */
  public static class Builder extends BasePayload.Builder<TrackPayload, Builder> {

    private String event;
    private Map<String, Object> properties;

    public Builder() {
      // Empty constructor.
    }

    @Private
    Builder(TrackPayload track) {
      super(track);
      event = track.event();
      properties = track.properties();
    }

    @NonNull
    public Builder event(@NonNull String event) {
      this.event = assertNotNullOrEmpty(event, "event");
      return this;
    }

    @NonNull
    public Builder properties(@NonNull Map<String, ?> properties) {
      assertNotNull(properties, "properties");
      this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
      return this;
    }

    @Override
    protected TrackPayload realBuild(
        @NonNull String messageId,
        @NonNull Date timestamp,
        @NonNull Map<String, Object> context,
        @NonNull Map<String, Object> integrations,
        String userId,
        @NonNull String anonymousId) {
      assertNotNullOrEmpty(event, "event");

      Map<String, Object> properties = this.properties;
      if (isNullOrEmpty(properties)) {
        properties = Collections.emptyMap();
      }

      return new TrackPayload(
          messageId, timestamp, context, integrations, userId, anonymousId, event, properties);
    }

    @Override
    Builder self() {
      return this;
    }
  }
}
