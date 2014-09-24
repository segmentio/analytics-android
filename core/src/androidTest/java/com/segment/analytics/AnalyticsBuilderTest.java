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

package com.segment.analytics;

import android.content.Context;
import java.util.Calendar;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.INTERNET;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;

public class AnalyticsBuilderTest extends BaseAndroidTestCase {
  @Mock Context context;
  String stubbedKey;

  @Override protected void setUp() throws Exception {
    super.setUp();

    when(context.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_GRANTED);
    stubbedKey = "stub";
  }

  public void testNullContextThrowsException() throws Exception {
    try {
      new Analytics.Builder(null, null);
      fail("Null context should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("Context must not be null.");
    }

    try {
      new Analytics.Builder(null, stubbedKey);
      fail("Null context should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("Context must not be null.");
    }
  }

  public void testMissingPermissionsThrowsException() throws Exception {
    when(context.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_DENIED);
    try {
      new Analytics.Builder(context, stubbedKey);
      fail("Missing internet permission should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("INTERNET permission is required.");
    }

    when(context.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_GRANTED);
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    try {
      new Analytics.Builder(context, stubbedKey);
    } catch (IllegalArgumentException expected) {
      fail("Missing access state permission should not throw exception.");
    }
  }

  public void testInvalidApiKeyThrowsException() throws Exception {
    try {
      new Analytics.Builder(context, null);
      fail("Null apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("writeKey must not be null or empty.");
    }

    try {
      new Analytics.Builder(context, "");
      fail("Empty apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("writeKey must not be null or empty.");
    }

    try {
      new Analytics.Builder(context, "    ");
      fail("Blank apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("writeKey must not be null or empty.");
    }
  }

  public void testInvalidQueueSizeThrowsException() throws Exception {
    try {
      new Analytics.Builder(context, stubbedKey).maxQueueSize(-1);
      fail("maxQueueSize < 0 should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("maxQueueSize must be greater than or equal to zero.");
    }

    try {
      new Analytics.Builder(context, stubbedKey).maxQueueSize(0);
      fail("maxQueueSize = 0 should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("maxQueueSize must be greater than or equal to zero.");
    }

    Analytics.Builder builder = new Analytics.Builder(context, stubbedKey).maxQueueSize(10);
    try {
      builder.maxQueueSize(20);
      fail("setting maxQueueSize again should throw exception.");
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("maxQueueSize is already set.");
    }
  }

  public void testInvalidOptionsThrowsException() throws Exception {
    try {
      new Analytics.Builder(context, stubbedKey).defaultOptions(null);
      fail("null options should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("defaultOptions must not be null.");
    }

    try {
      new Analytics.Builder(context, stubbedKey).defaultOptions(
          new Options().setTimestamp(Calendar.getInstance()));
      fail("default options with timestamp should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("default option must not contain timestamp.");
    }

    try {
      new Analytics.Builder(context, stubbedKey).defaultOptions(new Options())
          .defaultOptions(new Options());
      fail("setting options twice should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("defaultOptions is already set.");
    }
  }

  public void testInvalidTagThrowsException() throws Exception {
    try {
      new Analytics.Builder(context, stubbedKey).tag(null);
      fail("Null apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("tag must not be null or empty.");
    }

    try {
      new Analytics.Builder(context, stubbedKey).tag("");
      fail("Empty apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("tag must not be null or empty.");
    }

    try {
      new Analytics.Builder(context, stubbedKey).tag("    ");
      fail("Blank apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("tag must not be null or empty.");
    }

    try {
      new Analytics.Builder(context, stubbedKey).tag(stubbedKey).tag(stubbedKey);
      fail("Blank apiKey should throw exception.");
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("tag is already set.");
    }
  }
}
