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

import java.util.Map;

public class TrackPayload extends Payload {

  /**
   * The name of the event. We recommend using title case and past tense for event names, like
   * Signed Up.
   */
  private final String event;

  /**
   * A dictionary of properties that give more information about the event. We have a collection of
   * special properties that we recognize with semantic meaning. You can also add your own custom
   * properties.
   */
  private final Properties properties;

  public TrackPayload(String anonymousId, AnalyticsContext context,
      Map<String, Boolean> integrations, String userId, String event, Properties properties) {
    super(Type.TRACK, anonymousId, context, integrations, userId);
    this.event = event;
    this.properties = properties;
  }

}
