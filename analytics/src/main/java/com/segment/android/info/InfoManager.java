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

package com.segment.android.info;

import com.segment.android.Config;
import com.segment.android.models.EasyJSONObject;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONObject;

/**
 * A manager that uses plugin information getters to construct
 * an object that contains contextual information about the Android device
 */
public class InfoManager {

  private List<Info<?>> managers;

  public InfoManager(Config options) {
    managers = new LinkedList<Info<?>>();

    managers.add(new App());
    managers.add(new Device());
    managers.add(new Network());
    managers.add(new Screen());
    managers.add(new Locale());
    if (options.shouldSendLocation()) managers.add(new Location());
    managers.add(new OS());
    managers.add(new UserAgent());
  }

  /**
   * Builds an object that contains contextual information about the phone
   *
   * @param context Android context for the phone
   * @return JSONObject containing parsed information about the phone
   */
  public JSONObject build(android.content.Context context) {

    EasyJSONObject info = new EasyJSONObject();

    for (Info<?> manager : managers) {
      String key = manager.getKey();
      Object val = manager.get(context);

      if (val != null) {
        info.putObject(key, val);
      }
    }

    return info;
  }
}
