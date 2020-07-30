package com.segment.analytics

import com.google.common.collect.ImmutableMap
import com.segment.analytics.integrations.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.*

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
    Mockito.verify(integration).track(payload)
  }

  @Test
  fun trackDisabledInOptions() {
    val payload = TrackPayload.Builder()
        .event("event")
        .userId("userId")
        .integrations(Collections.singletonMap("Mixpanel", false))
        .build()
    track(payload, "Mixpanel", emptyMap())
    Mockito.verify(integration, Mockito.never()).track(payload)
  }

  @Test
  fun trackAllDisabledInOptions() {
    val payload = TrackPayload.Builder()
        .event("event")
        .userId("userId")
        .integrations(Collections.singletonMap("All", false))
        .build()
    track(payload, "Mixpanel", emptyMap())
    Mockito.verify(integration, Mockito.never()).track(payload)
  }

  @Test
  fun trackAllDisabledInOptionsButIntegrationEnabled() {
    val payload = TrackPayload.Builder()
        .event("event")
        .userId("userId")
        .integrations(ImmutableMap.of("All", false, "Mixpanel", true))
        .build()
    track(payload, "Mixpanel", emptyMap())
    Mockito.verify(integration).track(payload)
  }

  @Test
  fun trackAllDisabledInOptionsButIntegrationEnabledWithOptions() {
    val payload = TrackPayload.Builder()
        .event("event")
        .userId("userId")
        .integrations(ImmutableMap.of<String, Any?>("All", false, "Mixpanel", emptyMap<Any, Any>()))
        .build()
    track(payload, "Mixpanel", emptyMap())
    Mockito.verify(integration).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun trackPlanForEvent() {
    val payload = TrackPayload.Builder()
        .event("Intall Attributed").userId("userId").build()
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "Completed Order": {}
                            }
                          }
                        }"""))
    Mockito.verify(integration).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun trackWithOptionsAndWithoutEventPlan() {
    val payload = TrackPayload.Builder()
        .event("Intall Attributed")
        .userId("userId")
        .integrations(ImmutableMap.of("Mixpanel", false))
        .build()
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "Completed Order": {}
                            }
                          }
                        }"""))
    Mockito.verify(integration, Mockito.never()).track(payload)
  }

  @Throws(IOException::class)
  @Test
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
            """{
                        "plan": {
                        "track": {
                        "Completed Order": {}
                            }
                          }
                        }"""))
    Mockito.verify(integration, Mockito.never()).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun trackPlanDisabledEvent() {
    val payload = TrackPayload.Builder()
        .event("Install Attributed").userId("userId").build()
    track(
        payload,
        "Amplitude",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "Install Attributed": {
                        "enabled": false
                              }
                            }
                          }
                        }"""))
    Mockito.verify(integration, Mockito.never()).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun trackPlanDisabledEventSendsToSegment() {
    val payload = TrackPayload.Builder()
        .event("Install Attributed").userId("userId").build()
    track(
        payload,
        "Segment.io",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "Install Attributed": {
                        "enabled": false
                              }
                            }
                          }
                        }"""))
    Mockito.verify(integration).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun trackPlanDisabledIntegration() {
    val payload = TrackPayload.Builder()
        .event("Install Attributed").userId("userId").build()
    track(
        payload,
        "Amplitude",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "Install Attributed": {
                        "integrations": {
                        "Amplitude": false
                                }
                              }
                            }
                          }
                        }"""))
    Mockito.verify(integration, Mockito.never()).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun trackPlanEnabledIntegration() {
    val payload = TrackPayload.Builder()
        .event("Install Attributed").userId("userId").build()
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "Install Attributed": {
                        "integrations": {
                        "Mixpanel": true
                                }
                              }
                            }
                          }
                        }"""))

    Mockito.verify(integration).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun ignoresSegment() {
    val payload = TrackPayload.Builder()
        .event("Install Attributed").userId("userId").build()
    track(
        payload,
        "Segment.io",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "Install Attributed": {
                        "integrations": {
                        "Segment.io": false
                                }
                              }
                            }
                          }
                        }"""))
    Mockito.verify(integration).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun defaultNewEventsEnabled() {
    val payload = TrackPayload.Builder()
        .event("Install Attributed").userId("userId").build()
    track(
        payload,
        "Segment.io",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "__default": {
                        "enabled": true
                              }
                            }
                          }
                        }"""))

    Mockito.verify(integration).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun defaultNewEventsDisabled() {
    val payload = TrackPayload.Builder()
        .event("Install Attributed").userId("userId").build()
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "__default": {
                        "enabled": false
                              }
                            }
                          }
                        }"""))
    Mockito.verify(integration, Mockito.never()).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun defaultNewEventsDisabledSendToSegment() {
    val payload = TrackPayload.Builder()
        .event("Install Attributed").userId("userId").build()
    track(
        payload,
        "Segment.io",
        Cartographer.INSTANCE.fromJson(
            """{
                        "plan": {
                        "track": {
                        "__default": {
                        "enabled": false
                              }
                            }
                          }
                        }"""))
    Mockito.verify(integration).track(payload)
  }

  @Throws(IOException::class)
  @Test
  fun eventPlanOverridesSchemaDefualt() {
    val payload = TrackPayload.Builder()
        .event("Install Attributed").userId("userId").build()
    track(
        payload,
        "Mixpanel",
        Cartographer.INSTANCE.fromJson(
            """{
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
                        }"""))
    Mockito.verify(integration, Mockito.never()).track(payload)
  }

  @Test
  fun identify() {
    val payload = IdentifyPayload.Builder().userId("userId").build()
    IntegrationOperation.segmentEvent(payload, emptyMap())
        .run("Mixpanel", integration, ProjectSettings(emptyMap()))
    Mockito.verify(integration).identify(payload)
  }

  @Test
  fun alias() {
    val payload = AliasPayload.Builder()
        .previousId("foo").userId("userId").build()
    IntegrationOperation.segmentEvent(payload, emptyMap())
        .run("Mixpanel", integration, ProjectSettings(emptyMap()))
    Mockito.verify(integration).alias(payload)
  }

  @Test
  fun group() {
    val payload = GroupPayload.Builder()
        .userId("userId").groupId("bar").build()
    IntegrationOperation.segmentEvent(payload, emptyMap())
        .run("Mixpanel", integration, ProjectSettings(emptyMap()))
    Mockito.verify(integration).group(payload)
  }

  @Test
  fun track() {
    val payload = TrackPayload.Builder()
        .userId("userId").event("foo").build()
    IntegrationOperation.segmentEvent(payload, emptyMap())
        .run("Mixpanel", integration, ProjectSettings(emptyMap()))
    Mockito.verify(integration).track(payload)
  }

  @Test
  fun screen() {
    val payload = ScreenPayload.Builder()
        .userId("userId").name("foobar").build()
    IntegrationOperation.segmentEvent(payload, emptyMap())
        .run("Mixpanel", integration, ProjectSettings(emptyMap()))
    Mockito.verify(integration).screen(payload)
  }

}