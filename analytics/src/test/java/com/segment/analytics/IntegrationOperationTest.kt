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

import com.google.common.collect.ImmutableMap
import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.Integration
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload
import java.io.IOException
import java.util.Collections
import kotlin.jvm.Throws
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class IntegrationOperationTest {

    @Mock lateinit var integration: Integration<Void>

    @Before
    fun setUp() { initMocks(this) }

    private fun track(payload: TrackPayload, name: String, settings: Map<String, Any>) {
        IntegrationOperation.segmentEvent(payload, emptyMap())
            .run(name, integration, ProjectSettings(settings))
    }

    @Test
    fun trackNoOptions() {
        val payload = TrackPayload.Builder().event("event").userId("userId").build()
        track(payload, "Mixpanel", emptyMap())
        verify(integration).track(payload)
    }

    @Test
    fun trackDisabledInOptions() {
        val payload = TrackPayload.Builder()
            .event("event")
            .userId("userId")
            .integrations(Collections.singletonMap("Mixpanel", false))
            .build()
        track(payload, "Mixpanel", emptyMap())
        verify(integration, never()).track(payload)
    }

    @Test
    fun trackAllDisabledInOptions() {
        val payload = TrackPayload.Builder()
            .event("event")
            .userId("userId")
            .integrations(Collections.singletonMap("All", false))
            .build()
        track(payload, "Mixpanel", emptyMap())
        verify(integration, never()).track(payload)
    }

    @Test
    fun trackAllDisabledInOptionsButIntegrationEnabled() {
        val payload = TrackPayload.Builder()
            .event("event")
            .userId("userId")
            .integrations(ImmutableMap.of("All", false, "Mixpanel", true))
            .build()
        track(payload, "Mixpanel", emptyMap())
        verify(integration).track(payload)
    }

    @Test
    fun trackAllDisabledInOptionsButIntegrationEnabledWithOptions() {
        val payload = TrackPayload.Builder()
            .event("event")
            .userId("userId")
            .integrations(ImmutableMap.of<String, Any?>("All", false, "Mixpanel", emptyMap<Any, Any>()))
            .build()
        track(payload, "Mixpanel", emptyMap())
        verify(integration).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun trackPlanForEvent() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Mixpanel",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": { 
                        "Completed Order": {}
                      }
                    }
                  }
                  """
            )
        )
        verify(integration).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun trackWithOptionsAndWithoutEventPlan() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed")
            .userId("userId")
            .integrations(ImmutableMap.of("Mixpanel", false))
            .build()
        track(
            payload,
            "Mixpanel",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": { 
                        "Completed Order": {}
                      }
                    }
                  }
                  """
            )
        )
        verify(integration, never()).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun trackPlanForEventWithOptions() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed")
            .userId("userId")
            .integrations(Collections.singletonMap("All", false))
            .build()
        track(
            payload,
            "Mixpanel",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "Completed Order": {}
                      }
                    }
                  }
                  """
            )
        )
        verify(integration, never()).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun trackPlanDisabledEvent() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Amplitude",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "Install Attributed": {
                          "enabled": false
                        }
                      }
                    }
                  }
                  """
            )
        )
        verify(integration, never()).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun trackPlanDisabledEventSendsToSegment() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Segment.io",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "Install Attributed": {
                          "enabled": false
                        }
                      }
                    }
                  }
                  """
            )
        )
        verify(integration).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun trackPlanDisabledIntegration() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Amplitude",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "Install Attributed": {
                          "integrations": {
                            "Amplitude": false
                          }
                        }
                      }
                    }
                  }
                  """
            )
        )
        verify(integration, never()).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun trackPlanEnabledIntegration() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Mixpanel",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "Install Attributed": {
                          "Mixpanel": true
                        }
                      }
                    }
                  }
                  """
            )
        )

        verify(integration).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun ignoresSegment() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Segment.io",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "Install Attributed": {
                          "integrations": {
                            "Segment.io": false
                          }
                        }
                      }
                    }
                  }
                  """
            )
        )
        verify(integration).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun defaultNewEventsEnabled() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Segment.io",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "__default": {
                          "enabled": true
                        }
                      }
                    }
                  }
                  """
            )
        )

        verify(integration).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun defaultNewEventsDisabled() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Mixpanel",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "__default": {
                          "enabled": false
                        }
                      }
                    }
                  }
                  """
            )
        )
        verify(integration, never()).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun defaultNewEventsDisabledSendToSegment() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Segment.io",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "__default": {
                          "enabled": false
                        }
                      }
                    }
                  }
                  """
            )
        )
        verify(integration).track(payload)
    }

    @Test
    @Throws(IOException::class)
    fun eventPlanOverridesSchemaDefualt() {
        val payload = TrackPayload.Builder()
            .event("Install Attributed").userId("userId").build()
        track(
            payload,
            "Mixpanel",
            Cartographer.INSTANCE.fromJson(
                """
                  {
                    "plan": {
                      "track": {
                        "__default": {
                          "enabled": true
                        },
                            "Install Attributed": {
                              "enabled": false
                            }
                      }
                    }
                  }
                  """
            )
        )
        verify(integration, never()).track(payload)
    }

    @Test
    fun identify() {
        val payload = IdentifyPayload.Builder().userId("userId").build()
        IntegrationOperation.segmentEvent(payload, emptyMap())
            .run("Mixpanel", integration, ProjectSettings(emptyMap()))
        verify(integration).identify(payload)
    }

    @Test
    fun alias() {
        val payload = AliasPayload.Builder()
            .previousId("foo").userId("userId").build()
        IntegrationOperation.segmentEvent(payload, emptyMap())
            .run("Mixpanel", integration, ProjectSettings(emptyMap()))
        verify(integration).alias(payload)
    }

    @Test
    fun group() {
        val payload = GroupPayload.Builder()
            .userId("userId").groupId("bar").build()
        IntegrationOperation.segmentEvent(payload, emptyMap())
            .run("Mixpanel", integration, ProjectSettings(emptyMap()))
        verify(integration).group(payload)
    }

    @Test
    fun track() {
        val payload = TrackPayload.Builder()
            .userId("userId").event("foo").build()
        IntegrationOperation.segmentEvent(payload, emptyMap())
            .run("Mixpanel", integration, ProjectSettings(emptyMap()))
        verify(integration).track(payload)
    }

    @Test
    fun screen() {
        val payload = ScreenPayload.Builder()
            .userId("userId").name("foobar").build()
        IntegrationOperation.segmentEvent(payload, emptyMap())
            .run("Mixpanel", integration, ProjectSettings(emptyMap()))
        verify(integration).screen(payload)
    }
}
