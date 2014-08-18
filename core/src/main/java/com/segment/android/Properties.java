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

import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Just like traits, we also imbue some properties with semantic meaning, and you should only ever
 * use these property names for that purpose.
 */
public class Properties {
  double revenue;
  String currency;
  String value;
  private Map<String, Object> other;

  public Properties() {
    other = new HashMap<String, Object>();
  }

  public Properties setRevenue(double revenue) {
    this.revenue = revenue;
    return this;
  }

  public Properties setCurrency(String currency) {
    this.currency = currency;
    return this;
  }

  public Properties setValue(String value) {
    this.value = value;
    return this;
  }

  public Properties put(String key, Object value) {
    other.put(key, value);
    return this;
  }

  public double getRevenue() {
    return revenue;
  }

  public String getCurrency() {
    return currency;
  }

  public String getValue() {
    return value;
  }

  public Map<String, Object> getOther() {
    return other;
  }

  public JSONObject asJsonObject() throws JSONException {
    JSONObject jsonObject = new JSONObject(other);
    jsonObject.put("revenue", revenue);
    jsonObject.put("currency", currency);
    jsonObject.put("value", value);
    return jsonObject;
  }
}