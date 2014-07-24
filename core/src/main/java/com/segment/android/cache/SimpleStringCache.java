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

package com.segment.android.cache;

import android.content.Context;
import android.content.SharedPreferences;
import com.segment.android.Constants;

/**
 * Simple String cache built over the {@link android.content.SharedPreferences}
 */
public class SimpleStringCache {

  private SharedPreferences preferences;
  private String cacheKey;

  /**
   * Creates a simple string cache over {@link android.content.SharedPreferences}
   */
  public SimpleStringCache(Context context, String cacheKey) {

    String prefKey = Constants.PACKAGE_NAME + "." + context.getPackageName();

    preferences = context.getSharedPreferences(prefKey, Context.MODE_PRIVATE);

    this.cacheKey = cacheKey;
  }

  /**
   * Loads the value that will be put into the cache. Returns null
   * if the value can't be calculated
   */
  public String load() {
    return null;
  }

  /**
   * Returns the cached value, or calls load to get the value.
   * Will save the new one if needs to call load
   */
  public String get() {
    String val = preferences.getString(cacheKey, null);
    if (val == null) {
      val = load();
      if (val != null) set(val);
    }
    return val;
  }

  /**
   * Sets the value in the cache
   */
  public void set(String val) {
    preferences.edit().putString(cacheKey, val).commit();
  }

  /**
   * Removes the cache key and resets the cache
   */
  public void reset() {
    preferences.edit().remove(cacheKey).commit();
  }

  /**
   * Returns true if a value has been persisted to disk.
   */
  public boolean isSet() {
    return preferences.contains(cacheKey);
  }
}
