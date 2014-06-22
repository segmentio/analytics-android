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

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.segment.android.Constants;
import java.util.UUID;

public class DeviceId {

  /**
   * Get a unique device id.
   *
   * @param context Android Activity context.
   * @return the unique device ID
   */
  public static String get(Context context) {
    // credit method: Amplitude's Android library

    // Android ID
    // Issues on 2.2, some phones have same Android ID due to manufacturer
    // error
    String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(),
        android.provider.Settings.Secure.ANDROID_ID);

    if (!(TextUtils.isEmpty(androidId) || androidId.equals("9774d56d682e549c"))) {
      return androidId;
    }

    // Serial number
    // Guaranteed to be on all non phones in 2.3+
    try {
      String serialNumber = (String) Build.class.getField("SERIAL").get(null);
      if (!TextUtils.isEmpty(serialNumber)) {
        return serialNumber;
      }
    } catch (Exception e) {
    }

    // Telephony ID
    // Guaranteed to be on all phones, requires READ_PHONE_STATE permission
    if (AndroidUtils.permissionGranted(context, Constants.Permission.READ_PHONE_STATE)
        && context.getPackageManager().hasSystemFeature("android.hardware.telephony")) {

      TelephonyManager telephone =
          ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
      String telephonyId = telephone.getDeviceId();
      if (!TextUtils.isEmpty(telephonyId)) {
        return telephonyId;
      }
    }

    // If this still fails, generate random identifier that does not persist
    // across installations
    return UUID.randomUUID().toString();
  }
}
