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
package com.segment.analytics

import java.io.IOException
import java.lang.UnsupportedOperationException
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProjectSettingsTest {

    @Test
    @Throws(IOException::class)
    fun deserialization() {
        val cartographer = Cartographer.INSTANCE
        val projectSettingsJson =
            """
                    |{
                    |    "Amplitude": {
                    |        "trackNamedPages": true,
                    |        "trackCategorizedPages": true,
                    |        "trackAllPages": false,
                    |        "apiKey": "x"
                    |    },
                    |    "Flurry": {
                    |        "apiKey": "x",
                    |        "captureUncaughtExceptions": false,
                    |        "useHttps": true,
                    |        "sessionContinueSeconds": 10
                    |    },
                    |    "Mixpanel": {
                    |        "people": true,
                    |        "token": "x",
                    |        "trackAllPages": false,
                    |        "trackCategorizedPages": true,
                    |        "trackNamedPages": true,
                    |        "increments": [
                    |            
                    |        ],
                    |        "legacySuperProperties": false
                    |    },
                    |    "Segment.io": {
                    |        "apiKey": "x"
                    |    }
                    |}
                    """.trimMargin()
        val projectSettings = ProjectSettings.create(cartographer.fromJson(projectSettingsJson))

        assertThat(projectSettings).hasSize(5)
        assertThat(projectSettings).containsKey("timestamp")
        assertThat(projectSettings).containsKey("Segment.io")

        try {
            projectSettings["foo"] = "bar"
            fail("projectSettings should be immutable")
        } catch (ignored: UnsupportedOperationException) {
        }
    }
}
