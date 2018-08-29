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

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ProjectSettingsTest {

  @Test
  public void deserialization() throws IOException {
    Cartographer cartographer = Cartographer.INSTANCE;
    String projectSettingsJson =
        "{\n"
            + "  \"Amplitude\": {\n"
            + "    \"trackNamedPages\": true,\n"
            + "    \"trackCategorizedPages\": true,\n"
            + "    \"trackAllPages\": false,\n"
            + "    \"apiKey\": \"x\"\n"
            + "  },\n"
            + "  \"Flurry\": {\n"
            + "    \"apiKey\": \"x\",\n"
            + "    \"captureUncaughtExceptions\": false,\n"
            + "    \"useHttps\": true,\n"
            + "    \"sessionContinueSeconds\": 10\n"
            + "  },\n"
            + "  \"Mixpanel\": {\n"
            + "    \"people\": true,\n"
            + "    \"token\": \"x\",\n"
            + "    \"trackAllPages\": false,\n"
            + "    \"trackCategorizedPages\": true,\n"
            + "    \"trackNamedPages\": true,\n"
            + "    \"increments\": [\n"
            + "      \n"
            + "    ],\n"
            + "    \"legacySuperProperties\": false\n"
            + "  },\n"
            + "  \"Segment.io\": {\n"
            + "    \"apiKey\": \"x\"\n"
            + "  }\n"
            + "}";
    ProjectSettings projectSettings =
        ProjectSettings.create(cartographer.fromJson(projectSettingsJson));

    assertThat(projectSettings).hasSize(5).containsKey("timestamp").containsKey("Segment.io");

    try {
      projectSettings.put("foo", "bar");
      fail("projectSettings should be immutable");
    } catch (UnsupportedOperationException ignored) {
    }
  }
}
