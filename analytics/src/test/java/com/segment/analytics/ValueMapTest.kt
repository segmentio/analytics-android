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
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.segment.analytics.TestUtils.PROJECT_SETTINGS_JSON_SAMPLE
import kotlin.jvm.Throws
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.data.MapEntry
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ValueMapTest {

    lateinit var valueMap: ValueMap
    lateinit var cartographer: Cartographer

    @Before
    fun setUp() {
        initMocks(this)
        valueMap = ValueMap()
        cartographer = Cartographer.INSTANCE
    }

    @Test
    @Throws(Exception::class)
    fun disallowsNullMap() {
        try {
            ValueMap(null)
            fail("Null Map should throw exception.")
        } catch (ignored: IllegalArgumentException) {
        }
    }

    @Test
    @Throws(Exception::class)
    fun emptyMap() {
        assertThat(valueMap).hasSize(0).isEmpty()
    }

    @Test
    fun methodsAreForwardedCorrectly() {
        val delegate: MutableMap<String, Any> = spy(LinkedHashMap())
        val key: Any = "foo"

        valueMap = ValueMap(delegate)

        valueMap.clear()
        verify(delegate).clear()

        valueMap.containsKey(key)
        verify(delegate).containsKey(key)

        valueMap.entries
        verify(delegate).entries

        valueMap[key]
        verify(delegate)[key]

        valueMap.isEmpty()
        verify(delegate).isEmpty()

        valueMap.keys
        verify(delegate).keys

        valueMap["foo"] = key
        verify(delegate)["foo"] = key

        val map: MutableMap<String, Any> = LinkedHashMap()
        valueMap.putAll(map)
        verify(delegate).putAll(map)

        valueMap.remove(key)
        verify(delegate).remove(key)

        valueMap.size
        verify(delegate).size

        valueMap.values
        verify(delegate).values

        valueMap["bar"] = key
        verify(delegate)["bar"] = key
    }

    @Test
    @Throws(Exception::class)
    fun simpleConversation() {
        val stringPi = Math.PI.toString()

        valueMap["double_pi"] = Math.PI
        assertThat(valueMap.getString("double_pi")).isEqualTo(stringPi)

        valueMap["string_pi"] = Math.PI
        assertThat(valueMap.getDouble("string_pi", 0.0)).isEqualTo(Math.PI)
    }

    @Test
    @Throws(Exception::class)
    fun enumDeserialization() {
        valueMap["value1"] = MyEnum.VALUE1
        valueMap["value2"] = MyEnum.VALUE2
        val json: String = cartographer.toJson(valueMap)
        assertThat(json)
            .isIn(
                "{\"value2\":\"VALUE2\",\"value1\":\"VALUE1\"}",
                "{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}"
            )
        valueMap = ValueMap(cartographer.fromJson("{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}"))
        assertThat(valueMap)
            .contains(MapEntry.entry("value1", "VALUE1"))
            .contains(MapEntry.entry("value2", "VALUE2"))
        assertThat(valueMap.getEnum(MyEnum::class.java, "value1")).isEqualTo(MyEnum.VALUE1)
        assertThat(valueMap.getEnum(MyEnum::class.java, "value2")).isEqualTo(MyEnum.VALUE2)
    }

    @Test
    fun allowsNullValues() {
        valueMap[null] = "foo"
        valueMap["foo"] = null
    }

    @Test
    @Throws(Exception::class)
    fun nestedMaps() {
        val nested = ValueMap()
        nested["value"] = "box"
        valueMap["nested"] = nested

        assertThat(valueMap).hasSize(1).contains(MapEntry.entry("nested", nested))
        assertThat(cartographer.toJson(valueMap)).isEqualTo("{\"nested\":{\"value\":\"box\"}}")

        valueMap = ValueMap(cartographer.fromJson("{\"nested\":{\"value\":\"box\"}}"))
        assertThat(valueMap).hasSize(1).contains(MapEntry.entry("nested", nested))
    }

    @Test
    @Throws(Exception::class)
    fun customJsonMapDeserialization() {
        val settings = Settings.Settings(cartographer.fromJson(PROJECT_SETTINGS_JSON_SAMPLE))
        assertThat(settings).hasSize(4)
        assertThat(settings).containsKey("Amplitude")
        assertThat(settings.containsKey("Segment"))
        assertThat(settings).containsKey("Flurry")
        assertThat(settings).containsKey("Mixpanel")

        // Map Constructor
        val mixpanelSettings = settings.getMixpanelSettings()
        assertThat(mixpanelSettings)
            .contains(MapEntry.entry("token", "f7afe0cb436685f61a2b203254779e02"))
            .contains(MapEntry.entry("people", true))
            .contains(MapEntry.entry("trackNamedPages", true))
            .contains(MapEntry.entry("trackCategorizedPages", true))
            .contains(MapEntry.entry("trackAllPages", false))

        try {
            settings.getAmplitudeSettings()
        } catch (exception: AssertionError) {
            assertThat(exception)
                .hasMessage(
                    """
                        |Could not create instance of com.segment.analytics.ValueMapTest.AmplitudeSettings.
                        |java.lang.NoSuchMethodException: com.segment.analytics.ValueMapTest${"$"}AmplitudeSettings.<init>(java.util.Map)
                    """.trimMargin()
                )
        }
    }

    @Test
    @Throws(Exception::class)
    fun projectSettings() {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        val valueMap = ValueMap(cartographer.fromJson(PROJECT_SETTINGS_JSON_SAMPLE))

        assertThat(valueMap.getValueMap("Amplitude"))
            .isNotNull()
            .hasSize(4)
            .contains(MapEntry.entry("apiKey", "ad3c426eb736d7442a65da8174bc1b1b"))
            .contains(MapEntry.entry("trackNamedPages", true))
            .contains(MapEntry.entry("trackCategorizedPages", true))
            .contains(MapEntry.entry("trackAllPages", false))
        assertThat(valueMap.getValueMap("Flurry"))
            .isNotNull()
            .hasSize(4)
            .contains(MapEntry.entry("apiKey", "8DY3D6S7CCWH54RBJ9ZM"))
            .contains(MapEntry.entry("captureUncaughtExceptions", false))
            .contains(MapEntry.entry("useHttps", true))
            .contains(MapEntry.entry("sessionContinueSeconds", 10.0))
    }

    @Test
    @Throws(Exception::class)
    fun toJsonObject() {
        val jsonObject: JSONObject =
            ValueMap(cartographer.fromJson(PROJECT_SETTINGS_JSON_SAMPLE)).toJsonObject()
        val amplitude = jsonObject.getJSONObject("Amplitude")
        assertThat(amplitude).isNotNull()
        assertThat(amplitude.length()).isEqualTo(4)
        assertThat(amplitude.getString("apiKey")).isEqualTo("ad3c426eb736d7442a65da8174bc1b1b")
        assertThat(amplitude.getBoolean("trackNamedPages")).isTrue()
        assertThat(amplitude.getBoolean("trackCategorizedPages")).isTrue()
        assertThat(amplitude.getBoolean("trackAllPages")).isFalse()

        val flurry = jsonObject.getJSONObject("Flurry")
        assertThat(flurry).isNotNull()
        assertThat(flurry.length()).isEqualTo(4)
        assertThat(flurry.getString("apiKey")).isEqualTo("8DY3D6S7CCWH54RBJ9ZM")
        assertThat(flurry.getBoolean("useHttps")).isTrue()
        assertThat(flurry.getBoolean("captureUncaughtExceptions")).isFalse()
        assertThat(flurry.getDouble("sessionContinueSeconds")).isEqualTo(10.0)
    }

    @Test
    @Throws(Exception::class)
    fun toJsonObjectWithNullValue() {
        valueMap["foo"] = null

        val jsonObject = valueMap.toJsonObject()
        assertThat(jsonObject.get("foo")).isEqualTo(JSONObject.NULL)
    }

    @Test
    fun getInt() {
        assertThat(valueMap.getInt("a missing key", 1)).isEqualTo(1)

        valueMap.putValue("a number", 3.14)
        assertThat(valueMap.getInt("a number", 0)).isEqualTo(3)

        valueMap.putValue("a string number", "892")
        assertThat(valueMap.getInt("a string number", 0)).isEqualTo(892)

        valueMap.putValue("a string", "not really an int")
        assertThat(valueMap.getInt("a string", 0)).isEqualTo(0)
    }

    @Test
    fun getLong() {
        assertThat(valueMap.getLong("a missing key", 2)).isEqualTo(2)

        valueMap.putValue("a number", 3.14)
        assertThat(valueMap.getLong("a number", 0)).isEqualTo(3)

        valueMap.putValue("a string number", "88")
        assertThat(valueMap.getLong("a string number", 0)).isEqualTo(88)

        valueMap.putValue("a string", "not really a long")
        assertThat(valueMap.getLong("a string", 0)).isEqualTo(0)
    }

    @Test
    fun getFloat() {
        assertThat(valueMap.getFloat("foo", 0F)).isEqualTo(0F)

        valueMap.putValue("foo", 3.14)
        assertThat(valueMap.getFloat("foo", 0F)).isEqualTo(3.14F)
    }

    @Test
    fun getDouble() {
        assertThat(valueMap.getDouble("a missing key", 3.0)).isEqualTo(3.0)

        valueMap.putValue("a number", Math.PI)
        assertThat(valueMap.getDouble("a number", Math.PI)).isEqualTo(Math.PI)

        valueMap.putValue("a string number", "3.14")
        assertThat(valueMap.getDouble("a string number", 0.0)).isEqualTo(3.14)

        valueMap.putValue("a string", "not really a double")
        assertThat(valueMap.getDouble("a string", 0.0)).isEqualTo(0.0)
    }

    @Test
    fun getChar() {
        assertThat(valueMap.getChar("a missing key", 'a')).isEqualTo('a')

        valueMap.putValue("a string", "f")
        assertThat(valueMap.getChar("a string", 'a')).isEqualTo('f')

        valueMap.putValue("a char", 'b')
        assertThat(valueMap.getChar("a char", 'a')).isEqualTo('b')
    }

    enum class Soda {
        PEPSI,
        COKE
    }

    @Test
    fun getEnum() {
        // removed because Kotlin null safety won't allow null values to be input to getEnum
        /*try {
            valueMap.getEnum(null, "foo")
            fail("should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
           assertThat(e).hasMessage("enumType may not be null")
        }*/

        assertThat(valueMap.getEnum(Soda::class.java, "a missing key")).isNull()

        valueMap.putValue("pepsi", Soda.PEPSI)
        assertThat(valueMap.getEnum(Soda::class.java, "pepsi")).isEqualTo(Soda.PEPSI)

        valueMap.putValue("coke", "COKE")
        assertThat(valueMap.getEnum(Soda::class.java, "coke")).isEqualTo(Soda.COKE)
    }

    @Test
    fun getValueMapWithClass() {
        valueMap["foo"] = "not a map"
        assertThat(valueMap.getValueMap("foo", Traits::class.java)).isNull()
    }

    @Test
    fun getList() {
        valueMap["foo"] = "not a list"
        assertThat(valueMap.getList("foo", Traits::class.java)).isNull()
    }

    @Test
    fun toStringMap() {
        assertThat(valueMap.toStringMap()).isEmpty()
        valueMap["foo"] = "bar"
        assertThat(valueMap.toStringMap()) //
            .isEqualTo(ImmutableMap.Builder<String, Any>().put("foo", "bar").build())
    }

    private enum class MyEnum {
        VALUE1,
        VALUE2
    }

    class Settings : ValueMap() {
        class Settings(map: Map<String, Any>) : ValueMap(map) {

            fun getAmplitudeSettings(): AmplitudeSettings {
                return getValueMap("Amplitude", AmplitudeSettings::class.java)
            }

            fun getMixpanelSettings(): MixpanelSettings {
                return getValueMap("Mixpanel", MixpanelSettings::class.java)
            }
        }
    }

    class MixpanelSettings(delegate: Map<String, Any>) : ValueMap(delegate)

    class AmplitudeSettings : ValueMap() {
        init {
            throw AssertionError("string constructors must not be called when deserializing")
        }
    }
}
