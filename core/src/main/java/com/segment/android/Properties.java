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

import com.segment.android.json.JsonMap;

/**
 * Just like traits, we also imbue some properties with semantic meaning, and you should only ever
 * use these property names for that purpose.
 */
public class Properties extends JsonMap {
  private static final String REVENUE_KEY = "revenue";
  private static final String CURRENCY_KEY = "currency";
  private static final String VALUE_KEY = "value";

  public Properties() {
  }

  @Override public Properties putValue(String key, Object value) {
    super.putValue(key, value);
    return this;
  }

  public Properties putRevenue(double revenue) {
    return putValue(REVENUE_KEY, revenue);
  }

  public Properties putCurrency(String currency) {
    return putValue(CURRENCY_KEY, currency);
  }

  public Properties putValue(String value) {
    return putValue(VALUE_KEY, value);
  }
}