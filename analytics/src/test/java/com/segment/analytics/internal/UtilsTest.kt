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
package com.segment.analytics.internal

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import com.google.common.collect.ImmutableMap
import com.nhaarman.mockitokotlin2.whenever
import com.segment.analytics.internal.Utils.NullableConcurrentHashMap
import com.segment.analytics.internal.Utils.assertNotNull
import com.segment.analytics.internal.Utils.assertNotNullOrEmpty
import com.segment.analytics.internal.Utils.buffer
import com.segment.analytics.internal.Utils.coerceToFloat
import com.segment.analytics.internal.Utils.copySharedPreferences
import com.segment.analytics.internal.Utils.hasFeature
import com.segment.analytics.internal.Utils.hasPermission
import com.segment.analytics.internal.Utils.isConnected
import com.segment.analytics.internal.Utils.isNullOrEmpty
import com.segment.analytics.internal.Utils.newSet
import com.segment.analytics.internal.Utils.readFully
import com.segment.analytics.internal.Utils.transform
import java.io.IOException
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import okio.Buffer
import org.assertj.android.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UtilsTest {

    @Mock
    lateinit var context: Context

    @Before
    fun setUp() { initMocks(this) }

    @Test
    fun emptyString() {
        assertThat(isNullOrEmpty(null as String?)).isTrue()
        assertThat(isNullOrEmpty("")).isTrue()
        assertThat(isNullOrEmpty(" ")).isTrue()
        assertThat(isNullOrEmpty(" a ")).isFalse()
        assertThat(isNullOrEmpty("a")).isFalse()
    }

    @Test
    fun emptyMap() {
        assertThat(isNullOrEmpty(null as Map<*, *>?)).isTrue()
        val map: MutableMap<String, Any> = LinkedHashMap(20)
        assertThat(isNullOrEmpty(map)).isTrue()
        map["foo"] = "bar"
        assertThat(isNullOrEmpty(map)).isFalse()
        map.clear()
        assertThat(isNullOrEmpty(map)).isTrue()
    }

    @Test
    fun emptyCollections() {
        assertThat(isNullOrEmpty(null as Collection<*>?)).isTrue()
        val collection: MutableCollection<String> = ArrayList()
        assertThat(isNullOrEmpty(collection)).isTrue()
        collection.add("foo")
        assertThat(isNullOrEmpty(collection)).isFalse()
        collection.clear()
        assertThat(isNullOrEmpty(collection)).isTrue()
    }

    @Test
    fun testTransform() {
        val input: MutableMap<String, Int> = LinkedHashMap()
        input["a"] = 1
        input["b"] = 2
        input["c"] = 3
        input["d"] = 4
        val mapper: MutableMap<String, String?> = LinkedHashMap()
        mapper["a"] = "\$a"
        mapper["c"] = ""
        mapper["d"] = null
        assertThat(transform(input, mapper))
            .containsExactly(MapEntry.entry("\$a", 1), MapEntry.entry("b", 2))
    }

    @Test
    @Throws(Exception::class)
    fun returnsConnectedIfMissingPermission() {
        whenever(context.checkCallingOrSelfPermission(permission.ACCESS_NETWORK_STATE))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        assertThat(isConnected(context)).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun nullableConcurrentHashMapPutIgnoresNulls() {
        val map: MutableMap<String?, String?> = NullableConcurrentHashMap()
        map[null] = null
        map["foo"] = null
        map[null] = "bar"
        assertThat(map).isEmpty()
    }

    @Test
    @Throws(Exception::class)
    fun nullableConcurrentHashMapPutAllIgnoresNulls() {
        val values: MutableMap<String?, String?> = LinkedHashMap()
        values[null] = null
        values["foo"] = null
        values[null] = "bar"

        val map: MutableMap<String?, String?> = NullableConcurrentHashMap()
        map.putAll(values)
        assertThat(map).isEmpty()
    }

    @Test
    fun copySharedPreferences() {
        val src = RuntimeEnvironment.application
            .getSharedPreferences("src", Context.MODE_PRIVATE)
        src.edit().clear().apply()
        src.edit()
            .putBoolean("aBool", true)
            .putString("aString", "foo")
            .putInt("anInt", 2)
            .putFloat("aFloat", 3.14f)
            .putLong("aLong", 12345678910L)
            .putStringSet("aStringSet", HashSet(listOf("foo", "bar")))
            .apply()
        val target = RuntimeEnvironment.application
            .getSharedPreferences("target", Context.MODE_PRIVATE)
        target.edit().clear().apply()
        copySharedPreferences(src, target)
        assertThat(target)
            .contains("aBool", true)
            .contains("aString", "foo")
            .contains("anInt", 2)
            .contains("aFloat", 3.14f)
            .contains("aLong", 12345678910L)
            .contains("aStringSet", HashSet(listOf("foo", "bar")))
    }

    @Test
    fun newSet() {
        assertThat(newSet("foo", "bar")).containsOnly("foo", "bar")
    }

    @Test
    fun coerceToFloat() {
        assertThat(coerceToFloat("not a number", 1f)).isEqualTo(1f)
        assertThat(coerceToFloat(3.14f, 0f)).isEqualTo(3.14f)
        assertThat(coerceToFloat(1, 0f)).isEqualTo(1f)
    }

    @Test
    fun hasPermission() {
        whenever(context.checkCallingOrSelfPermission(permission.INTERNET))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        assertThat(hasPermission(context, permission.INTERNET)).isFalse()
        whenever(context.checkCallingOrSelfPermission(permission.INTERNET))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        assertThat(hasPermission(context, permission.INTERNET)).isTrue()
    }

    @Test
    fun hasFeature() {
        val packageManager = mock(PackageManager::class.java)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(false)
        assertThat(hasFeature(context, PackageManager.FEATURE_TELEPHONY)).isFalse()
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(true)
        assertThat(hasFeature(context, PackageManager.FEATURE_TELEPHONY)).isTrue()
    }

    @Test
    fun assertNotNullOrEmptyMap() {
        try {
            assertNotNullOrEmpty(null as Map<*, *>?, "foo")
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("foo cannot be null or empty")
        }
        try {
            assertNotNullOrEmpty(ImmutableMap.of<Any, Any>(), "bar")
            fail()
        } catch (e: NullPointerException) {
            assertThat(e).hasMessage("bar cannot be null or empty")
        }
        assertThat(assertNotNullOrEmpty(ImmutableMap.of("key", "val"), "foo"))
            .isEqualTo(ImmutableMap.of("key", "val"))
    }

    @Test
    fun assertNotNullOrEmptyText() {
        try {
            assertNotNullOrEmpty(null as String?, "foo")
            fail()
        } catch (e: java.lang.NullPointerException) {
            assertThat(e).hasMessage("foo cannot be null or empty")
        }
        try {
            assertNotNullOrEmpty("", "bar")
            fail()
        } catch (e: java.lang.NullPointerException) {
            assertThat(e).hasMessage("bar cannot be null or empty")
        }
        assertThat(assertNotNullOrEmpty("foo", "bar")).isEqualTo("foo")
    }

    @Test
    fun assertNotNull() {
        try {
            assertNotNull<Any?>(null, "foo")
            fail()
        } catch (e: java.lang.NullPointerException) {
            assertThat(e).hasMessage("foo == null")
        }
        val something = Any()
        assertThat(assertNotNull(something, "foo")).isEqualTo(something)
    }

    @Test
    @Throws(IOException::class)
    fun readFully() {
        assertThat(readFully(buffer(Buffer().writeUtf8("foo\nbar").inputStream())))
            .isEqualTo("foobar")
    }
}
