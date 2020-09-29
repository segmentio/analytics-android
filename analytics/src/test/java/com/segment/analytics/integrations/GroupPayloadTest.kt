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
import com.nhaarman.mockitokotlin2.any
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class GroupPayloadTest {

    lateinit var builder: GroupPayload.Builder

    @Before
    fun setUp() {
        builder = GroupPayload.Builder().userId("userId")
    }

    @Test
    fun invalidGroupIdThrows() {
        try {
            GroupPayload.Builder().groupId(any())
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("groupId cannot be null or empty")
        }

        try {
            //noinspection CheckResult,ConstantConditions
            GroupPayload.Builder().groupId("")
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("groupId cannot be null or empty")
        }
    }

    @Test
    fun groupId() {
        val payload = builder.groupId("group_id").build()
        assertThat(payload.groupId()).isEqualTo("group_id")
        assertThat(payload).containsEntry(GroupPayload.GROUP_ID_KEY, "group_id")
    }

    @Test
    fun groupIdIsRequired() {
        try {
            GroupPayload.Builder().userId("user_id").build()
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("groupId cannot be null or empty")
        }
    }

    @Test
    @Nullable
    fun invalidTraitThrows() {
        try {
            builder.traits(any())
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("traits == null")
        }
    }

    @Test
    fun traits() {
        val payload = builder
            .groupId("group_id")
            .traits(mapOf("foo" to "bar"))
            .build()
        assertThat(payload.traits())
            .isEqualTo(mapOf("foo" to "bar"))
        assertThat(payload)
            .containsEntry(GroupPayload.TRAITS_KEY, mapOf("foo" to "bar"))
    }
}
