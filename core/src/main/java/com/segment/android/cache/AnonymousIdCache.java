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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;
import com.segment.android.Constants;
import com.segment.android.Logger;
import java.util.UUID;

import static com.segment.android.utils.Utils.getSystemService;
import static com.segment.android.utils.Utils.hasFeature;
import static com.segment.android.utils.Utils.hasPermission;
import static com.segment.android.utils.Utils.isNullOrEmpty;

public class AnonymousIdCache extends SimpleStringCache {
  private Context context;

  public AnonymousIdCache(Context context) {
    super(context, Constants.SharedPreferences.ANONYMOUS_ID_KEY);

    this.context = context;

    new GetAdvertisingIdTask(context, this).execute();
  }

  static class GetAdvertisingIdTask extends AsyncTask<Void, Void, String> {
    private final Context context;
    private final AnonymousIdCache cache;

    GetAdvertisingIdTask(Context context, AnonymousIdCache cache) {
      this.context = context;
      this.cache = cache;
    }

    @Override protected String doInBackground(Void... params) {
      String advertisingId = null;
      try {
        advertisingId = getAdvertisingId(context);
      } catch (Exception e) {
        // Ignored below
      }

      if (isNullOrEmpty(advertisingId)) {
        Logger.d("Advertising id could not be loaded. Using device ID.");
        return getDeviceId(context);
      } else {
        Logger.d("Using advertising ID %s.", advertisingId);
        return advertisingId;
      }
    }

    @Override protected void onPostExecute(String s) {
      super.onPostExecute(s);
      cache.set(s);
    }
  }

  /**
   * Get the advertising ID on the device if available. Do not call from the main thread!
   */
  public static String getAdvertisingId(Context context) {
    try {
      Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
      return AdvertisingId.get(context);
    } catch (ClassNotFoundException e) {
      Logger.e(e, "Google Play Services included - not using advertising ID.");
    }
    return null;
  }

  /** Creates a unique device id to anonymously track a user. */
  public static String getDeviceId(Context context) {
    // credit method: Amplitude's Android library

    // Android ID
    // Issues on 2.2, some phones have same Android ID due to manufacturer
    // error
    String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(),
        android.provider.Settings.Secure.ANDROID_ID);

    if (!(isNullOrEmpty(androidId) || androidId.equals("9774d56d682e549c"))) {
      return androidId;
    }

    // Serial number
    // Guaranteed to be on all non phones in 2.3+
    if (!isNullOrEmpty(Build.SERIAL)) {
      return Build.SERIAL;
    }

    // Telephony ID, guaranteed to be on all phones, requires READ_PHONE_STATE permission
    if (hasPermission(context, Manifest.permission.READ_PHONE_STATE) && hasFeature(context,
        PackageManager.FEATURE_TELEPHONY)) {

      TelephonyManager telephonyManager = getSystemService(context, Context.TELEPHONY_SERVICE);
      String telephonyId = telephonyManager.getDeviceId();
      if (!isNullOrEmpty(telephonyId)) {
        return telephonyId;
      }
    }

    // If this still fails, generate random identifier that does not persist
    // across installations
    return UUID.randomUUID().toString();
  }

  @Override
  public String load() {
    // We'll use the device ID if needed before advertising ID is set
    return getDeviceId(context);
  }
}
