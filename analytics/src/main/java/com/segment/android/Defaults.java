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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Defaults {
  public static final boolean DEBUG = false;

  public static final String HOST = "https://api.segment.io";

  public static final int FLUSH_AT = 20;
  public static final int FLUSH_AFTER = (int) TimeUnit.SECONDS.toMillis(10);

  @SuppressWarnings("serial")
  public static final Map<String, String> ENDPOINTS = new HashMap<String, String>() {
    {
      this.put("identify", "/v1/identify");
      this.put("alias", "/v1/alias");
      this.put("track", "/v1/track");
      this.put("import", "/v1/import");
    }
  };

  public static String getSettingsEndpoint(String writeKey) {
    return "/project/" + writeKey + "/settings";
  }

  public static final int MAX_QUEUE_SIZE = 10000;

  // cache the settings for 1 hour before reloading
  public static final int SETTINGS_CACHE_EXPIRY = 1000 * 60 * 60;

  // try to send the location by default
  public static final boolean SEND_LOCATION = true;
}