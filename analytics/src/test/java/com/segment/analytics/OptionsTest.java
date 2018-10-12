/**
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class OptionsTest {

  Options options;

  @Before
  public void setUp() {
    options = new Options();
  }

  @Test
  public void disallowsDisablingSegmentIntegration() throws Exception {
    try {
      options.setIntegration("Segment.io", true);
      fail("shouldn't be able to set option for Segment integration.");
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("Segment integration cannot be enabled or disabled.");
    }
  }

  @Test
  public void setIntegration() throws Exception {
    options.setIntegration("Mixpanel", true);
    options.setIntegration("All", false);
    options.setIntegration(Analytics.BundledIntegration.BUGSNAG, false);
    options.setIntegrationOptions(
        "Amplitude", new ImmutableMap.Builder<String, Object>().put("email", "foo").build());
    options.setIntegrationOptions(
        Analytics.BundledIntegration.TAPSTREAM,
        new ImmutableMap.Builder<String, Object>().put("appId", "bar").build());

    assertThat(options.integrations()) //
        .isEqualTo(
            new ImmutableMap.Builder<String, Object>() //
                .put("Mixpanel", true)
                .put("All", false)
                .put("Bugsnag", false)
                .put(
                    "Amplitude",
                    new ImmutableMap.Builder<String, Object>().put("email", "foo").build())
                .put(
                    "Tapstream",
                    new ImmutableMap.Builder<String, Object>().put("appId", "bar").build())
                .build());
  }

  @Test
  public void setOptions() {
    options.putContext("foo", "bar");
    options.putContext(
        "library",
        new ImmutableMap.Builder<String, Object>().put("name", "analytics-test").build());

    assertThat(options.context())
        .isEqualTo(
            new ImmutableMap.Builder<String, Object>()
                .put("foo", "bar")
                .put(
                    "library",
                    new ImmutableMap.Builder<String, Object>()
                        .put("name", "analytics-test")
                        .build())
                .build());
  }
}
