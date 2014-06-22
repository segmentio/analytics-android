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

import com.segment.android.models.EasyJSONObject;
import java.util.Map;
import org.json.JSONObject;

public class Parameters {

  /**
   * Returns a copy of passed in json object, with keys mapped
   * from => to as dictated by the second move parameter.
   *
   * @param json The input json
   * @param map Maps parameters from => to
   * @return A copied object with the parameters mapped
   */
  public static EasyJSONObject move(JSONObject json, Map<String, String> map) {

    EasyJSONObject copy = new EasyJSONObject(json);

    // go through all the keys we want to map from
    for (String fromKey : map.keySet()) {
      // if the json object has the from key
      if (copy.has(fromKey)) {
        // get the key that we want to set it to
        String toKey = map.get(fromKey);

        Object val = copy.get(fromKey);
        copy.remove(fromKey);
        copy.put(toKey, val);
      }
    }

    return copy;
  }
}
