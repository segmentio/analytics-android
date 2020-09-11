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
package com.segment.analytics.internal;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class Iso8601UtilsTest {

    private Date date;
    private Date dateWithoutTime;
    private Date dateZeroMillis;
    private Date dateZeroSecondAndMillis;
    private Date dateWithNano;

    @Before
    public void setUp() {
        Calendar cal = new GregorianCalendar(2007, 8 - 1, 13, 19, 51, 23);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.MILLISECOND, 789);
        date = cal.getTime();
        cal.set(Calendar.MILLISECOND, 0);
        dateZeroMillis = cal.getTime();
        cal.set(Calendar.SECOND, 0);
        dateZeroSecondAndMillis = cal.getTime();

        cal = new GregorianCalendar(2007, 8 - 1, 13, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateWithoutTime = cal.getTime();
        dateWithNano = new NanoDate(1187207505345554387L);
    }

    @Test
    public void format() {
        assertThat(Iso8601Utils.format(date)).isEqualTo("2007-08-13T19:51:23.789Z");
        assertThat(Iso8601Utils.format(dateWithoutTime)).isEqualTo("2007-08-13T00:00:00.000Z");
        assertThat(Iso8601Utils.format(dateZeroMillis)).isEqualTo("2007-08-13T19:51:23.000Z");
        assertThat(Iso8601Utils.format(dateZeroSecondAndMillis))
                .isEqualTo("2007-08-13T19:51:00.000Z");
        assertThat(Iso8601Utils.format(dateWithNano)).isEqualTo("2007-08-15T19:51:45.345Z");
    }

    @Test
    public void formatNanos() {
        assertThat(Iso8601Utils.formatNanos(date)).isEqualTo("2007-08-13T19:51:23.789000000Z");
        assertThat(Iso8601Utils.formatNanos(dateWithoutTime))
                .isEqualTo("2007-08-13T00:00:00.000000000Z");
        assertThat(Iso8601Utils.formatNanos(dateZeroMillis))
                .isEqualTo("2007-08-13T19:51:23.000000000Z");
        assertThat(Iso8601Utils.formatNanos(dateZeroSecondAndMillis))
                .isEqualTo("2007-08-13T19:51:00.000000000Z");
        assertThat(Iso8601Utils.formatNanos(dateWithNano))
                .isEqualTo("2007-08-15T19:51:45.345554387Z");
    }

    @Test
    public void parse() {
        assertThat(Iso8601Utils.parse("2007-08-13T19:51:23.789Z")).isEqualTo(date);
        assertThat(Iso8601Utils.parse("2007-08-13T19:51:23Z")).isEqualTo(dateZeroMillis);
        assertThat(Iso8601Utils.parse("2007-08-13T21:51:23.789+02:00")).isEqualTo(date);
    }

    @Test
    public void parseWithNanos() {
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T19:51:23.789Z")).isEqualTo(date);
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T19:51:23.789000000Z")).isEqualTo(date);
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T19:51:23Z")).isEqualTo(dateZeroMillis);
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T19:51:23.000000000Z"))
                .isEqualTo(dateZeroMillis);
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T21:51:23.789+02:00")).isEqualTo(date);
        assertThat(Iso8601Utils.parseWithNanos("2007-08-13T21:51:23.789000000+02:00"))
                .isEqualTo(date);
        assertThat(Iso8601Utils.parseWithNanos("2007-08-15T19:51:45.345554387Z"))
                .isEqualTo(dateWithNano);
        assertThat(Iso8601Utils.parseWithNanos("20070815T19:51:45.345554387Z"))
                .isEqualTo(dateWithNano);
        assertThat(Iso8601Utils.parseWithNanos("2007-08-15T195145.345554387Z"))
                .isEqualTo(dateWithNano);
        assertThat(Iso8601Utils.parseWithNanos("20070815T195145.345554387Z"))
                .isEqualTo(dateWithNano);
        assertThat(Iso8601Utils.parseWithNanos("2007-08-15T21:51:45.345554387+02:00"))
                .isEqualTo(dateWithNano);
    }

    @Test
    public void parseShortDate() {
        assertThat(Iso8601Utils.parse("20070813T19:51:23.789Z")).isEqualTo(date);
        assertThat(Iso8601Utils.parse("20070813T19:51:23Z")).isEqualTo(dateZeroMillis);
        assertThat(Iso8601Utils.parse("20070813T21:51:23.789+02:00")).isEqualTo(date);
    }

    @Test
    public void parseShortTime() {
        assertThat(Iso8601Utils.parse("2007-08-13T195123.789Z")).isEqualTo(date);
        assertThat(Iso8601Utils.parse("2007-08-13T195123Z")).isEqualTo(dateZeroMillis);
        assertThat(Iso8601Utils.parse("2007-08-13T215123.789+02:00")).isEqualTo(date);
    }

    @Test
    public void parseShortDateTime() {
        assertThat(Iso8601Utils.parse("20070813T195123.789Z")).isEqualTo(date);
        assertThat(Iso8601Utils.parse("20070813T195123Z")).isEqualTo(dateZeroMillis);
        assertThat(Iso8601Utils.parse("20070813T215123.789+02:00")).isEqualTo(date);
    }

    @Test
    public void parseWithoutTime() {
        assertThat(Iso8601Utils.parse("2007-08-13Z")).isEqualTo(dateWithoutTime);
        assertThat(Iso8601Utils.parse("2007-08-13Z")).isEqualTo(dateWithoutTime);
        assertThat(Iso8601Utils.parse("2007-08-13Z")).isEqualTo(dateWithoutTime);
        assertThat(Iso8601Utils.parse("20070813+00:00")).isEqualTo(dateWithoutTime);
    }

    @Test
    public void parseOptional() throws java.text.ParseException {
        assertThat(Iso8601Utils.parse("2007-08-13T19:51Z")).isEqualTo(dateZeroSecondAndMillis);
        assertThat(Iso8601Utils.parse("2007-08-13T19:51Z")).isEqualTo(dateZeroSecondAndMillis);
        assertThat(Iso8601Utils.parse("2007-08-13T21:51+02:00")).isEqualTo(dateZeroSecondAndMillis);
    }

    @Test
    public void parseRfc3339Examples() throws java.text.ParseException {
        // Two digit milliseconds.
        assertThat(Iso8601Utils.parse("1985-04-12T23:20:50.52Z"))
                .isEqualTo(newDate(1985, 4, 12, 23, 20, 50, 520, 0));

        assertThat(Iso8601Utils.parse("1996-12-19T16:39:57-08:00"))
                .isEqualTo(newDate(1996, 12, 19, 16, 39, 57, 0, -8 * 60));

        // Truncated leap second.
        assertThat(Iso8601Utils.parse("1990-12-31T15:59:60-08:00"))
                .isEqualTo(newDate(1990, 12, 31, 23, 59, 59, 0, 0));

        // Truncated leap second.
        assertThat(Iso8601Utils.parse("1990-12-31T15:59:60-08:00"))
                .isEqualTo(newDate(1990, 12, 31, 15, 59, 59, 0, -8 * 60));

        // Two digit milliseconds.
        assertThat(Iso8601Utils.parse("1937-01-01T12:00:27.87+00:20"))
                .isEqualTo(newDate(1937, 1, 1, 12, 0, 27, 870, 20));
    }

    @Test
    public void fractionalSeconds() throws java.text.ParseException {
        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.9Z"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 900, 0));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.09Z"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 90, 0));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.009Z"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 9, 0));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.0009Z"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 0, 0));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.2147483647Z"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 214, 0));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.2147483648Z"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 214, 0));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.9+02:00"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 900, 2 * 60));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.09+02:00"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 90, 2 * 60));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.009+02:00"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 9, 2 * 60));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.0009+02:00"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 0, 2 * 60));

        assertThat(Iso8601Utils.parse("1970-01-01T00:00:00.2147483648+02:00"))
                .isEqualTo(newDate(1970, 1, 1, 0, 0, 0, 214, 2 * 60));
    }

    @Test
    public void decimalWithoutDecimalPointButNoFractionalSeconds() {
        try {
            Iso8601Utils.parse("1970-01-01T00:00:00.Z");
            fail();
        } catch (RuntimeException e) {
            assertThat(e).hasMessage("Not an RFC 3339 date: 1970-01-01T00:00:00.Z");
        }
    }

    private Date newDate(
            int year,
            int month,
            int day,
            int hour,
            int minute,
            int second,
            int millis,
            int timezoneOffsetMinutes) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        calendar.set(year, month - 1, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, millis);
        return new Date(
                calendar.getTimeInMillis() - TimeUnit.MINUTES.toMillis(timezoneOffsetMinutes));
    }
}
