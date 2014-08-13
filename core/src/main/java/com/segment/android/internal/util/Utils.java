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

package com.segment.android.internal.util;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import java.util.Collection;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;
import static android.provider.Settings.System.AIRPLANE_MODE_ON;

public final class Utils {
  public static final String THREAD_PREFIX = "SegmentAnalytics-";

  private Utils() {
    throw new AssertionError("No instances");
  }

  /** Returns true if the application has the given permission. */
  public static boolean hasPermission(Context context, String permission) {
    return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
  }

  /** Returns true if the application has the given feature. */
  public static boolean hasFeature(Context context, String feature) {
    return context.getPackageManager().hasSystemFeature(feature);
  }

  /** Returns the system service for the given string. */
  @SuppressWarnings("unchecked")
  public static <T> T getSystemService(Context context, String serviceConstant) {
    return (T) context.getSystemService(serviceConstant);
  }

  /** Returns true if the string is null, or empty (once trimmed). */
  public static boolean isNullOrEmpty(String text) {
    // Rather than using text.trim().length() == 0, use getTrimmedLength to avoid allocating an
    // extra string object
    return TextUtils.isEmpty(text) || TextUtils.getTrimmedLength(text) == 0;
  }

  /** Returns true if the collection or has a size 0. */
  public static boolean isNullOrEmpty(Collection collection) {
    return collection == null || collection.size() == 0;
  }

  /** Returns true the phone is in AirplaneMode. */
  public static boolean isAirplaneModeOn(Context context) {
    ContentResolver contentResolver = context.getContentResolver();
    return android.provider.Settings.System.getInt(contentResolver, AIRPLANE_MODE_ON, 0) != 0;
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

  /** Returns a shared preferences for storing any library preferences. */
  public static SharedPreferences getSharedPreferences(Context context) {
    return context.getSharedPreferences("analytics-android", MODE_PRIVATE);
  }
}
