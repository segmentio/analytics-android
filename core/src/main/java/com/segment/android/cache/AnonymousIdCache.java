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

package com.segment.android.cache;

import android.content.Context;
import android.os.AsyncTask;
import com.segment.android.Constants;
import com.segment.android.Logger;

import static com.segment.android.utils.Utils.getAnonymousId;

public class AnonymousIdCache extends SimpleStringCache {

  public AnonymousIdCache(Context context) {
    super(context, Constants.SharedPreferences.ANONYMOUS_ID_KEY);

    if (!isSet()) {
      new GetDeviceIdTask(context, this).execute();
    }
  }

  static class GetDeviceIdTask extends AsyncTask<Void, Void, String> {
    final Context context;
    final AnonymousIdCache cache;

    GetDeviceIdTask(Context context, AnonymousIdCache cache) {
      this.context = context;
      this.cache = cache;
    }

    @Override protected String doInBackground(Void... params) {
      return getAnonymousId(context);
    }

    @Override protected void onPostExecute(String deviceId) {
      super.onPostExecute(deviceId);

      Logger.v("Generated anonymous device ID : %s", deviceId);
      if (!cache.isSet()) {
        Logger.v("Anonymous ID already set, skipping");
        cache.set(deviceId);
      }
    }
  }
}
