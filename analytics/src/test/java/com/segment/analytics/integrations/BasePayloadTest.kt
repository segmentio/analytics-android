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
package com.segment.analytics.integrations

import androidx.annotation.Nullable
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.nhaarman.mockitokotlin2.any
import com.segment.analytics.integrations.BasePayload.Type
import java.util.Date
import org.assertj.core.api.Assertions
import org.assertj.core.data.MapEntry
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class BasePayloadTest {

    private lateinit var builders: List<BasePayload.Builder<out BasePayload, out BasePayload.Builder<*, *>>>

    @Before
    fun setUp() {
        builders =
            ImmutableList.of(
                AliasPayload.Builder().previousId("previousId").userId("userId"),
                TrackPayload.Builder().event("event"),
                ScreenPayload.Builder().name("name"),
                GroupPayload.Builder().groupId("groupId"),
                IdentifyPayload.Builder().traits(ImmutableMap.of<String, Any>("foo", "bar"))
            )
    }

    @Test
    fun channelIsSet() {
        for (builder in builders) {
            val payload = builder.userId("user_id").build()
            Assertions.assertThat(payload).containsEntry(BasePayload.CHANNEL_KEY, BasePayload.Channel.mobile)
        }
    }

    @Test
    fun nullTimestampThrows() {
        for (i in 1 until builders.size) {
            val builder = builders[i]
            try {
                builder.timestamp(any())
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("timestamp == null")
            }
        }
    }

    @Test
    fun timestamp() {
        val timestamp = Date()
        for (builder in builders) {
            val payload = builder.userId("user_id").timestamp(timestamp).build()
            Assertions.assertThat(payload.timestamp()).isEqualTo(timestamp)
            Assertions.assertThat(payload).containsKey(BasePayload.TIMESTAMP_KEY)
        }
    }

    @Test
    fun type() {
        for (builder in builders) {
            val payload = builder.userId("user_id").build()
            Assertions.assertThat(payload.type())
                .isIn(Type.alias, Type.track, Type.screen, Type.group, Type.identify)
            Assertions.assertThat(payload).containsKey(BasePayload.TYPE_KEY)
        }
    }

    @Test
    fun anonymousId() {
        for (builder in builders) {
            val payload = builder.anonymousId("anonymous_id").build()
            Assertions.assertThat(payload.anonymousId()).isEqualTo("anonymous_id")
            Assertions.assertThat(payload).containsEntry(BasePayload.ANONYMOUS_ID_KEY, "anonymous_id")
        }
    }

    @Test
    fun invalidUserIdThrows() {
        for (i in 1 until builders.size) {
            val builder = builders[i]

            try {
                builder.userId(" ") // null
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("userId cannot be null or empty")
            }

            try {
                builder.userId("")
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("userId cannot be null or empty")
            }
        }
    }

    @Test
    fun userId() {
        for (builder in builders) {
            val payload = builder.userId("user_id").build()
            Assertions.assertThat(payload.userId()).isEqualTo("user_id")
            Assertions.assertThat(payload).containsEntry(BasePayload.USER_ID_KEY, "user_id")
        }
    }

    @Test
    fun requiresUserIdOrAnonymousId() {
        for (i in 1 until builders.size) {
            val builder = builders[i]

            try {
                builder.build()
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("either userId or anonymousId is required")
            }
        }
    }

    @Test
    fun invalidMessageIdThrows() {
        for (i in 1 until builders.size) {
            val builder = builders[i]

            try {
                builder.messageId(" ") // null
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("messageId cannot be null or empty")
            }

            try {
                builder.messageId("")
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("messageId cannot be null or empty")
            }
        }
    }

    @Test
    fun messageId() {
        for (builder in builders) {
            val payload = builder.userId("user_id").messageId("message_id").build()
            Assertions.assertThat(payload.messageId()).isEqualTo("message_id")
            Assertions.assertThat(payload).containsEntry(BasePayload.MESSAGE_ID, "message_id")
        }
    }

    @Test
    fun messageIdIsGenerated() {
        for (builder in builders) {
            val payload = builder.userId("user_id").build()
            Assertions.assertThat(payload.messageId()).isNotEmpty()
            Assertions.assertThat(payload).containsKey(BasePayload.MESSAGE_ID)
        }
    }

    @Test
    fun nullContextThrows() {
        for (i in 1 until builders.size) {
            val builder = builders[i]

            try {
                builder.context(any())
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("context == null")
            }
        }
    }

    @Test
    fun context() {
        for (builder in builders) {
            val payload =
                builder.userId("user_id").context(ImmutableMap.of("foo", "bar")).build()
            Assertions.assertThat(payload.context()).containsExactly(MapEntry.entry("foo", "bar"))
            Assertions.assertThat(payload)
                .containsEntry(BasePayload.CONTEXT_KEY, ImmutableMap.of("foo", "bar"))
        }
    }

    @Test
    fun invalidIntegrationKeyThrows() {
        for (i in 1 until builders.size) {
            val builder = builders[i]

            try {
                //noinspection CheckResult,ConstantConditions
                builder.integration(any(), false)
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("key cannot be null or empty")
            }

            try {
                //noinspection CheckResult,ConstantConditions
                builder.integration("", true)
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("key cannot be null or empty")
            }
        }
    }

    @Test
    @Nullable
    fun invalidIntegrationOption() {
        for (i in 1 until builders.size) {
            val builder = builders[i]

            try {
                //noinspection CheckResult,ConstantConditions
                builder.integration(any(), ImmutableMap.of("foo", "bar") as Map<String, Any>)
                Assert.fail() // null
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("key cannot be null or empty")
            }

            try {
                //noinspection CheckResult,ConstantConditions
                builder.integration("", ImmutableMap.of("foo", "bar") as Map<String, Any>)
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("key cannot be null or empty")
            }

            try {
                builder.integration("foo", mapOf())
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("options cannot be null or empty")
            }

            try {
                builder.integration("bar", ImmutableMap.of())
                Assert.fail()
            } catch (e: NullPointerException) {
                Assertions.assertThat(e).hasMessage("options cannot be null or empty")
            }
        }
    }

    @Test
    fun integrations() {
        for (builder in builders) {
            val payload =
                builder.userId("user_id").integrations(ImmutableMap.of("foo", "bar")).build()
            Assertions.assertThat(payload.integrations()).containsExactly(MapEntry.entry("foo", "bar"))
            Assertions.assertThat(payload)
                .containsEntry(BasePayload.INTEGRATIONS_KEY, ImmutableMap.of("foo", "bar"))
        }
    }

    @Test
    fun integration() {
        for (builder in builders) {
            val payload = builder.userId("user_id").integration("foo", false).build()
            Assertions.assertThat(payload.integrations()).containsExactly(MapEntry.entry("foo", false))
        }
    }

    @Test
    fun integrationOptions() {
        for (builder in builders) {
            val payload =
                builder
                    .userId("user_id")
                    .integration("foo", ImmutableMap.of("bar", true) as Map<String, Any>).build()
            Assertions.assertThat(payload.integrations())
                .containsExactly(MapEntry.entry("foo", ImmutableMap.of("bar", true)))
        }
    }

    @Test
    fun putValue() {
        for (builder in builders) {
            val payload = builder.userId("user_id").build().putValue("foo", "bar")
            Assertions.assertThat(payload).containsEntry("foo", "bar")
        }
    }

    @Test
    fun builderCopy() {
        for (builder in builders) {
            val payload =
                builder.userId("user_id").build().toBuilder().userId("a_new_user_id").build()
            Assertions.assertThat(payload.userId()).isEqualTo("a_new_user_id")
        }
    }
}
