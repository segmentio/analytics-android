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

import android.app.Activity;
import android.os.Bundle;

/**
 * A base activity that automatically configures your Segmnet.io analytics
 * provider. An alternative is overriding these methods:
 *
 * @Override protected void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState);
 * Analytics.onCreate(this);
 * }
 * @Override protected void onStart() {
 * super.onStart();
 * Analytics.activityStart(this);
 * }
 * @Override protected void onResume() {
 * super.onResume();
 * Analytics.activityResume(this);
 * }
 * @Override protected void onPause() {
 * Analytics.activityPause(this);
 * super.onPause();
 * }
 * @Override protected void onStop() {
 * super.onStop();
 * Analytics.activityStop(this);
 * }
 *
 * This activity automatically initializes the Segment.io Android client.
 *
 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
 * you to conveniently consume the API without making any HTTP requests
 * yourself.
 *
 * This client is also designed to be thread-safe and to not block each of
 * your calls to make a HTTP request. It uses batching to efficiently send
 * your requests on a separate resource-constrained thread pool.
 */
public class TrackedActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Analytics.onCreate(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
    Analytics.activityStart(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Analytics.activityResume(this);
  }

  @Override
  protected void onPause() {
    Analytics.activityPause(this);
    super.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    Analytics.activityStop(this);
  }
}
