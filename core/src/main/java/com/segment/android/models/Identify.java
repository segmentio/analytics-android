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

public class Identify extends BasePayload {

  public final static String TYPE = "identify";

  private final static String USER_ID_KEY = "userId";
  private final static String TRAITS_KEY = "traits";

  public Identify(JSONObject obj) {
    super(obj);
  }

  public Identify(String userId, Traits traits, EasyJSONObject bundledIntegrations,
      Options options) {
    super(TYPE, bundledIntegrations, options);
    setUserId(userId);
    setTraits(traits);
  }

  public String getUserId() {
    return this.optString(USER_ID_KEY, null);
  }

  public void setUserId(String userId) {
    this.put(USER_ID_KEY, userId);
  }

  public Traits getTraits() {
    JSONObject object = getObject(TRAITS_KEY);
    if (object == null) {
      return null;
    } else {
      return new Traits(object);
    }
  }

  public void setTraits(Traits traits) {
    this.put(TRAITS_KEY, traits);
  }
}
