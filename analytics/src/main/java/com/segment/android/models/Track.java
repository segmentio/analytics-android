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

package com.segment.android.models;

import org.json.JSONObject;

public class Track extends BasePayload {

  public final static String TYPE = "track";

  private final static String USER_ID_KEY = "userId";
  private static final String EVENT_KEY = "event";
  private static final String PROPERTIES_KEY = "properties";

  public Track(JSONObject obj) {
    super(obj);
  }

  public Track(String userId, String event, Props properties, Options options) {

    super(TYPE, options);
    setUserId(userId);
    setEvent(event);
    setProperties(properties);
  }

  public String getUserId() {
    return this.optString(USER_ID_KEY, null);
  }

  public void setUserId(String userId) {
    this.put(USER_ID_KEY, userId);
  }

  public String getEvent() {
    return this.optString(EVENT_KEY, null);
  }

  public void setEvent(String event) {
    this.put(EVENT_KEY, event);
  }

  public Props getProperties() {
    JSONObject object = getObject(PROPERTIES_KEY);
    if (object == null) {
      return null;
    } else {
      return new Props(object);
    }
  }

  public void setProperties(Props properties) {
    this.put(PROPERTIES_KEY, properties);
  }
}
