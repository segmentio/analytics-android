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

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;

public class SegmentBuilderTest extends BaseAndroidTestCase {
  @Mock Application application;

  @Override protected void setUp() throws Exception {
    super.setUp();

    // Setup Fake Required Permissions
    when(application.checkCallingOrSelfPermission(Manifest.permission.INTERNET)).thenReturn(
        PackageManager.PERMISSION_GRANTED);
    when(application.checkCallingOrSelfPermission(
        Manifest.permission.ACCESS_NETWORK_STATE)).thenReturn(PackageManager.PERMISSION_GRANTED);
  }

  public void testInvalidContextThrowsException() throws Exception {
    try {
      new Segment.Builder(null);
      fail("Null context should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMissingPermissionsThrowsException() throws Exception {
    when(application.checkCallingOrSelfPermission(Manifest.permission.INTERNET)).thenReturn(
        PackageManager.PERMISSION_DENIED);
    when(application.checkCallingOrSelfPermission(
        Manifest.permission.ACCESS_NETWORK_STATE)).thenReturn(PackageManager.PERMISSION_GRANTED);
    try {
      new Segment.Builder(application);
      fail("Missing internet permission should throw exception.");
    } catch (IllegalArgumentException expected) {
    }

    when(application.checkCallingOrSelfPermission(Manifest.permission.INTERNET)).thenReturn(
        PackageManager.PERMISSION_GRANTED);
    when(application.checkCallingOrSelfPermission(
        Manifest.permission.ACCESS_NETWORK_STATE)).thenReturn(PackageManager.PERMISSION_DENIED);
    try {
      new Segment.Builder(application);
      fail("Missing access state permission should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testInvalidApiKeyThrowsException() throws Exception {
    try {
      new Segment.Builder(application).writeKey(null);
      fail("Null apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
    }

    try {
      new Segment.Builder(application).writeKey("");
      fail("Empty apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
    }

    try {
      new Segment.Builder(application).writeKey("   ");
      fail("Blank apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testInvalidQueueSizeThrowsException() throws Exception {
    try {
      new Segment.Builder(application).maxQueueSize(-1);
      fail("maxQueueSize < 0 should throw exception.");
    } catch (IllegalArgumentException expected) {
    }

    try {
      new Segment.Builder(application).maxQueueSize(0);
      fail("maxQueueSize = 0 should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }
}
