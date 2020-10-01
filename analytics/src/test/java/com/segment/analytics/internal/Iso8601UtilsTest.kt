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

import java.lang.RuntimeException
import java.text.ParseException
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class Iso8601UtilsTest {

    private lateinit var date: Date
    private lateinit var dateWithoutTime: Date
    private lateinit var dateZeroMillis: Date
    private lateinit var dateZeroSecondAndMillis: Date
    private lateinit var dateWithNano: Date

    @Before
    fun setUp() {
        var cal: Calendar = GregorianCalendar(2007, 8 - 1, 13, 19, 51, 23)
        cal.timeZone = TimeZone.getTimeZone("GMT")
        cal[Calendar.MILLISECOND] = 789
        date = cal.time
        cal[Calendar.MILLISECOND] = 0
        dateZeroMillis = cal.time
        cal[Calendar.SECOND] = 0
        dateZeroSecondAndMillis = cal.time
        cal = GregorianCalendar(2007, 8 - 1, 13, 0, 0, 0)
        cal[Calendar.MILLISECOND] = 0
        cal.setTimeZone(TimeZone.getTimeZone("GMT"))
        dateWithoutTime = cal.getTime()
        dateWithNano = NanoDate(1187207505345554387L)
    }

    @Test
    fun format() {
        assertThat(Iso8601Utils.format(date)).isEqualTo("2007-08-13T19:51:23.789Z")
        assertThat(Iso8601Utils.format(dateWithoutTime)).isEqualTo("2007-08-13T00:00:00.000Z")
        assertThat(Iso8601Utils.format(dateZeroMillis)).isEqualTo("2007-08-13T19:51:23.000Z")
        assertThat(Iso8601Utils.format(dateZeroSecondAndMillis)).isEqualTo("2007-08-13T19:51:00.000Z")
        assertThat(Iso8601Utils.format(dateWithNano)).isEqualTo("2007-08-15T19:51:45.345Z")
    }

    @Test
    fun formatNanos() {
        assertThat(Iso8601Utils.formatNanos(date)).isEqualTo("2007-08-13T19:51:23.789000000Z")
        assertThat(Iso8601Utils.formatNanos(dateWithoutTime))
            .isEqualTo("2007-08-13T00:00:00.000000000Z")
        assertThat(Iso8601Utils.formatNanos(dateZeroMillis))
            .isEqualTo("2007-08-13T19:51:23.000000000Z")
        assertThat(Iso8601Utils.formatNanos(dateZeroSecondAndMillis))
            .isEqualTo("2007-08-13T19:51:00.000000000Z")
        assertThat(Iso8601Utils.formatNanos(dateWithNano)).isEqualTo("2007-08-15T19:51:45.345554387Z")
    }

    @Test
    fun parse() {
        assertThat(Iso8601Utils.parse("2007-08-13T19:51:23.789Z")).isEqualTo(date)
        assertThat(Iso8601Utils.parse("2007-08-13T19:51:23Z")).isEqualTo(dateZeroMillis)
        assertThat(Iso8601Utils.parse("2007-08-13T21:51:23.789+02:00")).isEqualTo(date)
    }

    @Test
    fun parseWithNanos() {
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T19:51:23.789Z")).isEqualTo(date)
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T19:51:23.789000000Z")).isEqualTo(date)
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T19:51:23Z")).isEqualTo(dateZeroMillis)
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T19:51:23.000000000Z"))
            .isEqualTo(dateZeroMillis)
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T21:51:23.789+02:00")).isEqualTo(date)
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T21:51:23.789000000+02:00")).isEqualTo(date)
        assertThat(Iso8601Utils.parseWithNanos("2007-08-15T19:51:45.345554387Z"))
            .isEqualTo(dateWithNano)
        assertThat(Iso8601Utils.parseWithNanos("20070815T19:51:45.345554387Z")).isEqualTo(dateWithNano)
        assertThat(Iso8601Utils.parseWithNanos("2007-08-15T195145.345554387Z")).isEqualTo(dateWithNano)
        assertThat(Iso8601Utils.parseWithNanos("20070815T195145.345554387Z")).isEqualTo(dateWithNano)
        assertThat(Iso8601Utils.parseWithNanos("2007-08-15T21:51:45.345554387+02:00"))
            .isEqualTo(dateWithNano)
    }

    @Test
    fun parseShortDate() {
        assertThat(Iso8601Utils.parse("20070813T19:51:23.789Z")).isEqualTo(date)
        assertThat(Iso8601Utils.parse("20070813T19:51:23Z")).isEqualTo(dateZeroMillis)
        assertThat(Iso8601Utils.parse("20070813T21:51:23.789+02:00")).isEqualTo(date)
    }

    @Test
    fun parseShortTime() {
        assertThat(Iso8601Utils.parse("2007-08-13T195123.789Z")).isEqualTo(date)
        assertThat(Iso8601Utils.parse("2007-08-13T195123Z")).isEqualTo(dateZeroMillis)
        assertThat(Iso8601Utils.parse("2007-08-13T215123.789+02:00")).isEqualTo(date)
    }

    @Test
    fun parseShortDateTime() {
        assertThat(Iso8601Utils.parse("20070813T195123.789Z")).isEqualTo(date)
        assertThat(Iso8601Utils.parse("20070813T195123Z")).isEqualTo(dateZeroMillis)
        assertThat(Iso8601Utils.parse("20070813T215123.789+02:00")).isEqualTo(date)
    }

    @Test
    fun parseWithoutTime() {
        assertThat(Iso8601Utils.parse("2007-08-13Z")).isEqualTo(dateWithoutTime)
        assertThat(Iso8601Utils.parse("2007-08-13Z")).isEqualTo(dateWithoutTime)
        assertThat(Iso8601Utils.parse("2007-08-13Z")).isEqualTo(dateWithoutTime)
        assertThat(Iso8601Utils.parse("20070813+00:00")).isEqualTo(dateWithoutTime)
    }

    @Test
    @Throws(ParseException::class)
    fun parseOptional() {
        assertThat(Iso8601Utils.parse("2007-08-13T19:51Z")).isEqualTo(dateZeroSecondAndMillis)
        assertThat(Iso8601Utils.parse("2007-08-13T19:51Z")).isEqualTo(dateZeroSecondAndMillis)
        assertThat(Iso8601Utils.parse("2007-08-13T21:51+02:00")).isEqualTo(dateZeroSecondAndMillis)
    }

    @Test
    @Throws(ParseException::class)
    fun parseRfc3339Examples() {
        // Two digit milliseconds.
        assertThat(Iso8601Utils.parse("1985-04-12T23:20:50.52Z"))
            .isEqualTo(newDate(1985, 4, 12, 23, 20, 50, 520, 0))

        assertThat(Iso8601Utils.parse("1996-12-19T16:39:57-08:00"))
            .isEqualTo(newDate(1996, 12, 19, 16, 39, 57, 0, -8 * 60))

        // Truncated leap second.
        assertThat(Iso8601Utils.parse("1990-12-31T15:59:60-08:00"))
            .isEqualTo(newDate(1990, 12, 31, 23, 59, 59, 0, 0))

        // Truncated leap second.
        assertThat(Iso8601Utils.parse("1990-12-31T15:59:60-08:00"))
            .isEqualTo(newDate(1990, 12, 31, 15, 59, 59, 0, -8 * 60))

        // Two digit milliseconds.
        assertThat(Iso8601Utils.parse("1937-01-01T12:00:27.87+00:20"))
            .isEqualTo(newDate(1937, 1, 1, 12, 0, 27, 870, 20))
    }

    @Test
    @Throws(ParseException::class)
    fun fractionalSeconds() {
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.9Z"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 900, 0))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.09Z"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 90, 0))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.009Z"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 9, 0))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.0009Z"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 0, 0))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.2147483647Z"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 214, 0))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.2147483648Z"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 214, 0))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.9+02:00"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 900, 2 * 60))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.09+02:00"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 90, 2 * 60))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.009+02:00"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 9, 2 * 60))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.0009+02:00"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 0, 2 * 60))
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.2147483648+02:00"))
            .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 214, 2 * 60))
    }

    @Test
    fun decimalWithoutDecimalPointButNoFractionalSeconds() {
        try {
            Iso8601Utils.parse("1970-01-01T00:00:00.Z")
            fail()
        } catch (e: RuntimeException) {
            assertThat(e).hasMessage("Not an RFC 3339 date: 1970-01-01T00:00:00.Z")
        }
    }

    private fun newDate(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
        millis: Int,
        timezoneOffsetMinutes: Long
    ): Date {
        val calendar = GregorianCalendar(TimeZone.getTimeZone("GMT"))
        calendar.set(year, month - 1, day, hour, minute, second)
        calendar.set(Calendar.MILLISECOND, millis)
        return Date(calendar.getTimeInMillis() - TimeUnit.MINUTES.toMillis(timezoneOffsetMinutes))
    }
}
