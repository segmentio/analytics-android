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

package com.segment.android;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

class ISO8601Time {
  private static final DateFormat ISO_8601_DATE_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  private final Date date;

  private ISO8601Time(Date date) {
    this.date = date;
  }

  static ISO8601Time now() {
    return new ISO8601Time(Calendar.getInstance().getTime());
  }

  static ISO8601Time parse(String time) throws ParseException {
    return new ISO8601Time(ISO_8601_DATE_FORMAT.parse(time));
  }

  static ISO8601Time from(Date date) {
    return new ISO8601Time(date); // exposes mutability?
  }

  static ISO8601Time from(Calendar calendar) {
    return new ISO8601Time(calendar.getTime()); // exposes mutability?
  }

  static ISO8601Time from(long timestamp) {
    return new ISO8601Time(new Date(timestamp));
  }

  @Override public String toString() {
    return ISO_8601_DATE_FORMAT.format(date);
  }
}
