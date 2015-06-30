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

package com.segment.analytics.internal.model.payloads;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Properties;

public class TrackPayload extends BasePayload {

  /**
   * The name of the event. We recommend using title case and past tense for event names, like
   * "Signed Up".
   */
  private static final String EVENT_KEY = "event";

  /**
   * A dictionary of properties that give more information about the event. We have a collection of
   * special properties that we recognize with semantic meaning. You can also add your own custom
   * properties.
   */
  private static final String PROPERTIES_KEY = "properties";

  public TrackPayload(AnalyticsContext context, Options options, String event,
      Properties properties) {
    super(TYPE_TRACK, context, options);
    put(EVENT_KEY, event);
    put(PROPERTIES_KEY, properties);
  }

  public String event() {
    return getString(EVENT_KEY);
  }

  public Properties properties() {
    return getValueMap(PROPERTIES_KEY, Properties.class);
  }

  @Override public String toString() {
    return "TrackPayload{\"" + event() + '}';
  }
}
