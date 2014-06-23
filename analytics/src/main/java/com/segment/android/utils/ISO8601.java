/*
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

package com.segment.android.utils;

import android.annotation.SuppressLint;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Android answer:
 * http://stackoverflow.com/questions/2201925/converting-iso8601-compliant-string-to-java-util-date
 *
 * Helper class for handling ISO 8601 strings of the following format:
 * "2008-03-01T13:00:00+01:00". It also supports parsing the "Z" timezone.
 */
@SuppressLint("SimpleDateFormat")
public final class ISO8601 {
  /** Transform Calendar to ISO 8601 string. */

  public static String fromCalendar(final Calendar calendar) {
    Date date = calendar.getTime();
    String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(date);
    return formatted.substring(0, 22) + ":" + formatted.substring(22);
  }

  /** Get current date and time formatted as ISO 8601 string. */
  public static String now() {
    return fromCalendar(GregorianCalendar.getInstance());
  }

  /** Transform ISO 8601 string to Calendar. */
  public static Calendar toCalendar(final String iso8601string) throws ParseException {
    Calendar calendar = GregorianCalendar.getInstance();
    String s = iso8601string.replace("Z", "+00:00");
    try {
      s = s.substring(0, 22) + s.substring(23);
    } catch (IndexOutOfBoundsException e) {
      throw new ParseException("Invalid length", 0);
    }
    Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
    calendar.setTime(date);
    return calendar;
  }
}