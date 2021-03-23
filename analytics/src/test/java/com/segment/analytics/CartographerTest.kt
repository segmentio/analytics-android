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
import java.io.IOException
import java.io.Reader
import java.io.StringWriter
import java.util.UUID
import kotlin.collections.LinkedHashMap
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.data.MapEntry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CartographerTest {
    lateinit var cartographer: Cartographer

    @Before
    fun setUp() {
        cartographer = Cartographer.Builder().lenient(false).prettyPrint(true).build()
    }

    @Test
    @Throws(IOException::class)
    fun encodePrimitives() {
        val map = ImmutableMap.builder<String, Any>()
            .put("byte", 32.toByte())
            .put("boolean", true)
            .put("short", 100.toShort())
            .put("int", 1)
            .put("long", 43L)
            .put("float", 23f)
            .put("double", Math.PI)
            .put("char", 'a')
            .put("String", "string")
            .build()

        assertThat(cartographer.toJson(map))
            .isEqualTo(
                """
                        |{
                        |  "byte": 32,
                        |  "boolean": true,
                        |  "short": 100,
                        |  "int": 1,
                        |  "long": 43,
                        |  "float": 23.0,
                        |  "double": 3.141592653589793,
                        |  "char": "a",
                        |  "String": "string"
                        |}
                        """.trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun decodesPrimitives() {
        val json =
            """
                |{
                |  "byte": 32,
                |  "boolean": true,
                |  "short": 100,
                |  "int": 1,
                |  "long": 43,
                |  "float": 23.0,
                |  "double": 3.141592653589793,
                |  "char": "a",
                |  "String": "string"
                |}
                |""".trimMargin()
        val map = cartographer.fromJson(json)

        assertThat(map)
            .hasSize(9)
            .contains(MapEntry.entry("byte", 32.0))
            .contains(MapEntry.entry("boolean", true))
            .contains(MapEntry.entry("short", 100.0))
            .contains(MapEntry.entry("int", 1.0))
            .contains(MapEntry.entry("long", 43.0))
            .contains(MapEntry.entry("float", 23.0))
            .contains(MapEntry.entry("double", Math.PI))
            .contains(MapEntry.entry("char", "a"))
            .contains(MapEntry.entry("String", "string"))
    }

    @Test
    @Throws(IOException::class)
    fun prettyPrintDisabled() {
        val cartographer = Cartographer.Builder().prettyPrint(false).build()
        val map =
            ImmutableMap.builder<String, Any>()
                .put(
                    "a",
                    ImmutableMap.builder<String, Any>()
                        .put(
                            "b",
                            ImmutableMap.builder<String, Any>()
                                .put(
                                    "c",
                                    ImmutableMap.builder<String, Any>()
                                        .put(
                                            "d",
                                            ImmutableMap.builder<String, Any>()
                                                .put("e", "f")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()

        assertThat(cartographer.toJson(map))
            .isEqualTo("{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":\"f\"}}}}}")
    }

    @Test
    @Throws(IOException::class)
    fun encodesNestedMaps() {
        val map =
            ImmutableMap.builder<String, Any>()
                .put(
                    "a",
                    ImmutableMap.builder<String, Any>()
                        .put(
                            "b",
                            ImmutableMap.builder<String, Any>()
                                .put(
                                    "c",
                                    ImmutableMap.builder<String, Any>()
                                        .put(
                                            "d",
                                            ImmutableMap.builder<String, Any>()
                                                .put("e", "f")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()

        assertThat(cartographer.toJson(map))
            .isEqualTo(
                """
                        |{
                        |  "a": {
                        |    "b": {
                        |      "c": {
                        |        "d": {
                        |          "e": "f"
                        |        }
                        |      }
                        |    }
                        |  }
                        |}
                        """.trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun decodesNestedMaps() {
        val json =
            """
                |{
                |  "a": {
                |    "b": {
                |      "c": {
                |        "d": {
                |          "e": "f"
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        val map = cartographer.fromJson(json)
        val expected = ImmutableMap.builder<String, Any>()
            .put(
                "a",
                ImmutableMap.builder<String, Any>()
                    .put(
                        "b",
                        ImmutableMap.builder<String, Any>()
                            .put(
                                "c",
                                ImmutableMap.builder<String, Any>()
                                    .put(
                                        "d",
                                        ImmutableMap.builder<String, Any>()
                                            .put("e", "f")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        assertThat(map).isEqualTo(expected)
    }

    @Test
    @Throws(IOException::class)
    fun encodesArraysWithList() {
        val map = ImmutableMap.builder<String, Any>().put("a", listOf("b", "c", "d")).build()

        assertThat(cartographer.toJson(map))
            .isEqualTo(
                """
                        |{
                        |  "a": [
                        |    "b",
                        |    "c",
                        |    "d"
                        |  ]
                        |}
                        """.trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun decodesArraysWithList() {
        val json =
            """
                |{
                |  "a": [
                |    "b",
                |    "c",
                |    "d"
                |  ]
                |}
                """.trimMargin()

        val expected =
            ImmutableMap.builder<String, Any>().put("a", listOf("b", "c", "d")).build()

        assertThat(cartographer.fromJson(json)).isEqualTo(expected)
    }

    @Test
    @Throws(IOException::class)
    fun encodesArraysWithArrays() {
        val map =
            ImmutableMap.builder<String, Any>().put("a", arrayOf("b", "c", "d")).build()

        assertThat(cartographer.toJson(map))
            .isEqualTo(
                """
                        |{
                        |  "a": [
                        |    "b",
                        |    "c",
                        |    "d"
                        |  ]
                        |}
                        """.trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun encodesInfiniteAndNanDoubles() {
        val map = ImmutableMap.builder<String, Any>()
            .put("nan", Double.NaN)
            .put("positive_infinity", Double.POSITIVE_INFINITY)
            .put("negative_infinity", Double.NEGATIVE_INFINITY)
            .build()

        assertThat(cartographer.toJson(map))
            .isEqualTo(
                """
                        |{
                        |  "nan": 0.0,
                        |  "positive_infinity": 0.0,
                        |  "negative_infinity": 0.0
                        |}
                        """.trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun encodesInfiniteAndNanFloats() {
        val map = ImmutableMap.builder<String, Any>()
            .put("nan", Float.NaN)
            .put("positive_infinity", Float.POSITIVE_INFINITY)
            .put("negative_infinity", Float.NEGATIVE_INFINITY)
            .build()

        assertThat(cartographer.toJson(map))
            .isEqualTo(
                """
                        |{
                        |  "nan": 0.0,
                        |  "positive_infinity": 0.0,
                        |  "negative_infinity": 0.0
                        |}
                        """.trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun encodesPrimitiveArrays() {
        // Exercise a bug where primitive arrays would throw an IOException.
        // https://github.com/segmentio/analytics-android/issues/507
        val map: Map<String?, Any?> = ImmutableMap.builder<String?, Any?>().put("a", intArrayOf(1, 2)).build()

        assertThat(cartographer.toJson(map))
            .isEqualTo(
                """
                        |{
                        |  "a": [
                        |    1,
                        |    2
                        |  ]
                        |}
                        """.trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun decodesArraysAsArraysAsList() {
        val json =
            """
                |{
                |  "a": [
                |    "b",
                |    "c",
                |    "d"
                |  ]
                |}
                """.trimMargin()
        val expected: Map<String, Any> = ImmutableMap.builder<String, Any>()
            .put("a", listOf("b", "c", "d"))
            .build()

        assertThat(cartographer.fromJson(json)).isEqualTo(expected)
    }

    @Test
    @Throws(IOException::class)
    fun encodesArrayOfMap() {
        val map: Map<String, Any> = ImmutableMap.builder<String, Any>()
            .put(
                "a",
                listOf<ImmutableMap<*, *>>(
                    ImmutableMap.builder<String, Any>().put("b", "c").build(),
                    ImmutableMap.builder<String, Any>().put("b", "d").build(),
                    ImmutableMap.builder<String, Any>().put("b", "e").build()
                )
            )
            .build()

        assertThat(cartographer.toJson(map))
            .isEqualTo(
                """
                        |{
                        |  "a": [
                        |    {
                        |      "b": "c"
                        |    },
                        |    {
                        |      "b": "d"
                        |    },
                        |    {
                        |      "b": "e"
                        |    }
                        |  ]
                        |}
                        """.trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun decodesArrayOfMap() {
        val json =
            """
                |{
                |  "a": [
                |    {
                |      "b": "c"
                |    },
                |    {
                |      "b": "d"
                |    },
                |    {
                |      "b": "e"
                |    }
                |  ]
                |}
                """.trimMargin()

        val expected: Map<String, Any> = ImmutableMap.builder<String, Any>()
            .put(
                "a",
                listOf<ImmutableMap<*, *>>(
                    ImmutableMap.builder<String, Any>().put("b", "c").build(),
                    ImmutableMap.builder<String, Any>().put("b", "d").build(),
                    ImmutableMap.builder<String, Any>().put("b", "e").build()
                )
            )
            .build()

        assertThat(cartographer.fromJson(json)).isEqualTo(expected)
    }

    @Test
    @Throws(IOException::class)
    fun disallowsEncodingNullMap() {
        try {
            cartographer.toJson(null, StringWriter())
            fail("null map should throw Exception")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessage("map == null")
        }
    }

    @Test
    @Throws(IOException::class)
    fun disallowsEncodingToNullWriter() {
        try {
            cartographer.toJson(LinkedHashMap<Any, Any>(), null)
            fail("null writer should throw Exception")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessage("writer == null")
        }
    }

    @Test
    @Throws(IOException::class)
    fun disallowsDecodingNullReader() {
        try {
            cartographer.fromJson(null as Reader?)
            fail("null map should throw Exception")
        } catch (e: java.lang.IllegalArgumentException) {
            assertThat(e).hasMessage("reader == null")
        }
    }

    @Test
    @Throws(IOException::class)
    fun disallowsDecodingNullString() {
        try {
            cartographer.fromJson(null as String?)
            fail("null map should throw Exception")
        } catch (e: java.lang.IllegalArgumentException) {
            assertThat(e).hasMessage("json == null")
        }
    }

    @Test
    @Throws(IOException::class)
    fun disallowsDecodingEmptyString() {
        try {
            cartographer.fromJson("")
            fail("null map should throw Exception")
        } catch (e: java.lang.IllegalArgumentException) {
            assertThat(e).hasMessage("json empty")
        }
    }

    @Test
    @Throws(IOException::class)
    fun encodesNumberMax() {
        val writer = StringWriter()
        val map = LinkedHashMap<String, Any>()
        map["byte"] = Byte.MAX_VALUE
        map["short"] = Short.MAX_VALUE
        map["int"] = Int.MAX_VALUE
        map["long"] = Long.MAX_VALUE
        map["float"] = Float.MAX_VALUE
        map["double"] = Double.MAX_VALUE
        map["char"] = Character.MAX_VALUE

        cartographer.toJson(map, writer)

        assertThat(writer.toString())
            .isEqualTo(
                """
                        |{
                        |  "byte": 127,
                        |  "short": 32767,
                        |  "int": 2147483647,
                        |  "long": 9223372036854775807,
                        |  "float": 3.4028235E38,
                        |  "double": 1.7976931348623157E308,
                        |  "char": "￿"
                        |}""".trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun encodesNumberMin() {
        val writer = StringWriter()
        val map: MutableMap<String?, Any?> = java.util.LinkedHashMap()
        map["byte"] = Byte.MIN_VALUE
        map["short"] = Short.MIN_VALUE
        map["int"] = Int.MIN_VALUE
        map["long"] = Long.MIN_VALUE
        map["float"] = Float.MIN_VALUE
        map["double"] = Double.MIN_VALUE
        map["char"] = Character.MIN_VALUE
        cartographer.toJson(map, writer)
        assertThat(writer.toString())
            .isEqualTo(
                """
                        |{
                        |  "byte": -128,
                        |  "short": -32768,
                        |  "int": -2147483648,
                        |  "long": -9223372036854775808,
                        |  "float": 1.4E-45,
                        |  "double": 4.9E-324,
                        |  "char": "\u0000"
                        |}
                        """.trimMargin()
            )
    }

    @Test
    @Throws(IOException::class)
    fun encodesLargeDocuments() {
        val map = LinkedHashMap<String, Any>()
        for (i in 1 until 100) {
            map[UUID.randomUUID().toString()] = UUID.randomUUID().toString()
        }
        val writer = StringWriter()
        cartographer.toJson(map, writer)
    }
}
