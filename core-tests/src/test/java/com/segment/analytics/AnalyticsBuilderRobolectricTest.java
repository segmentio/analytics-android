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
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.INTERNET;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.segment.analytics.Analytics.Builder;
import static com.segment.analytics.TestUtils.mockApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class AnalyticsBuilderRobolectricTest {
  final String stubbedKey = "stub";
  Context context;

  @Before
  public void setUp() {
    initMocks(this);
    context = mockApplication();
  }

  @Test public void nullContextThrowsException() throws Exception {
    try {
      new Builder(null, null);
      fail("Null context should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("Context must not be null.");
    }

    try {
      new Builder(null, stubbedKey);
      fail("Null context should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("Context must not be null.");
    }
  }

  @Test public void missingPermissionsThrowsException() throws Exception {
    when(context.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_DENIED);
    try {
      new Builder(context, stubbedKey);
      fail("Missing internet permission should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("INTERNET permission is required.");
    }

    when(context.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_GRANTED);
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    try {
      new Builder(context, stubbedKey);
    } catch (IllegalArgumentException expected) {
      fail("Missing access state permission should not throw exception.");
    }
  }

  @Test public void invalidApiKeyThrowsException() throws Exception {
    try {
      new Builder(context, null);
      fail("Null apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("writeKey must not be null or empty.");
    }

    try {
      new Builder(context, "");
      fail("Empty apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("writeKey must not be null or empty.");
    }

    try {
      new Builder(context, "    ");
      fail("Blank apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("writeKey must not be null or empty.");
    }
  }

  @Test public void invalidQueueSizeThrowsException() throws Exception {
    try {
      new Builder(context, stubbedKey).maxQueueSize(-1);
      fail("queueSize < 0 should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("queueSize must be greater than or equal to zero.");
    }

    try {
      new Builder(context, stubbedKey).maxQueueSize(0);
      fail("queueSize = 0 should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("queueSize must be greater than or equal to zero.");
    }

    try {
      Builder builder = new Builder(context, stubbedKey).maxQueueSize(10);
      builder.maxQueueSize(20);
    } catch (IllegalStateException unexpected) {
      fail("maxQueueSize can be set multiple times.");
    }

    try {
      new Builder(context, stubbedKey).queueSize(-1);
      fail("queueSize < 0 should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("queueSize must be greater than or equal to zero.");
    }

    try {
      new Builder(context, stubbedKey).queueSize(0);
      fail("queueSize = 0 should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("queueSize must be greater than or equal to zero.");
    }

    try {
      Builder builder = new Builder(context, stubbedKey).queueSize(10);
      builder.queueSize(20);
    } catch (IllegalStateException unexpected) {
      fail("queueSize can be set multiple times.");
    }
  }

  @Test public void invalidFlushIntervalThrowsException() throws Exception {
    try {
      new Builder(context, stubbedKey).flushInterval(-1);
      fail("flushInterval < 0 should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("flushInterval must be greater than or equal to 1.");
    }

    try {
      new Builder(context, stubbedKey).flushInterval(0);
      fail("flushInterval < 1 should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("flushInterval must be greater than or equal to 1.");
    }

    try {
      Builder builder = new Builder(context, stubbedKey).flushInterval(25);
      builder.flushInterval(10);
    } catch (IllegalStateException unexpected) {
      fail("flushInterval can be set multiple times.");
    }
  }

  @Test public void invalidOptionsThrowsException() throws Exception {
    try {
      new Builder(context, stubbedKey).defaultOptions(null);
      fail("null options should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("defaultOptions must not be null.");
    }

    try {
      new Builder(context, stubbedKey).defaultOptions(new Options().setTimestamp(new Date()));
      fail("default options with timestamp should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("default option must not contain timestamp.");
    }

    try {
      new Builder(context, stubbedKey).defaultOptions(new Options()).defaultOptions(new Options());
      fail("setting options twice should throw exception.");
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("defaultOptions is already set.");
    }
  }

  @Test public void invalidTagThrowsException() throws Exception {
    try {
      new Builder(context, stubbedKey).tag(null);
      fail("Null apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("tag must not be null or empty.");
    }

    try {
      new Builder(context, stubbedKey).tag("");
      fail("Empty apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("tag must not be null or empty.");
    }

    try {
      new Builder(context, stubbedKey).tag("    ");
      fail("Blank apiKey should throw exception.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("tag must not be null or empty.");
    }

    try {
      new Builder(context, stubbedKey).tag(stubbedKey).tag(stubbedKey);
      fail("Blank apiKey should throw exception.");
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("tag is already set.");
    }
  }
}
