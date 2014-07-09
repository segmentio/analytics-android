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

import com.segment.android.BuildConfig;
import org.json.JSONObject;

public class Context extends EasyJSONObject {

  private static final String LIBRARY_KEY = "library";
  private static final String LIBRARY_NAME_KEY = "name";
  private static final String LIBRARY_VERSION_KEY = "version";

  private static final String LIBRARY_NAME_VALUE = "analytics-android";

  public Context() {
    super();
    addLibraryContext();
  }

  public Context(JSONObject obj) {
    super(obj);
    addLibraryContext();
  }

  public Context(Object... kvs) {
    super(kvs);
    addLibraryContext();
  }

  private void addLibraryContext() {
    this.putObject(LIBRARY_KEY, new Props().put(LIBRARY_NAME_KEY, LIBRARY_NAME_VALUE)
        .put(LIBRARY_VERSION_KEY, BuildConfig.VERSION_NAME));
  }

  @Override
  public Context put(String key, String value) {
    super.put(key, value);
    return this;
  }

  @Override
  public Context put(String key, Object value) {
    super.putObject(key, value);
    return this;
  }
}