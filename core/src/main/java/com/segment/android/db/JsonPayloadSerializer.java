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

package com.segment.android.db;

import com.segment.android.Logger;
import com.segment.android.models.Alias;
import com.segment.android.models.BasePayload;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonPayloadSerializer implements IPayloadSerializer {

  @Override
  public String serialize(BasePayload payload) {
    return payload.toString();
  }

  @Override
  public BasePayload deserialize(String str) {
    JSONObject obj;
    try {
      obj = new JSONObject(str);
      String type = obj.getString("type");
      if (type.equals(Identify.TYPE)) {
        return new Identify(obj);
      } else if (type.equals(Track.TYPE)) {
        return new Track(obj);
      } else if (type.equals(Alias.TYPE)) {
        return new Alias(obj);
      } else if (type.equals(Group.TYPE)) {
        return new Group(obj);
      } else if (type.equals(Screen.TYPE)) {
        return new Screen(obj);
      } else {
        Logger.e("Failed to convert json to base payload because of unknown type: %s", type);
      }
    } catch (JSONException e) {
      Logger.e(e, "Failed to convert json to base payload");
    }

    return null;
  }
}
