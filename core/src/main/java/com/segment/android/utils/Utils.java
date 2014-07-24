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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Utils {
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

  /** Returns true if the string is null, or empty (when trimmed). */
  public static boolean isNullOrEmpty(String text) {
    // Rather than using text.trim().length() == 0, use getTrimmedLength to avoid allocating an
    // extra string object
    return TextUtils.isEmpty(text) || TextUtils.getTrimmedLength(text) == 0;
  }

  /**
   * Throws {@link IllegalStateException} if called from the main thread.
   */
  public static void checkNotMain() {
    if (isMain()) {
      throw new IllegalStateException("Method call should not happen from the main thread.");
    }
  }

  /**
   * Throws {@link IllegalStateException} if not called from the main thread.
   */
  public static void checkMain() {
    if (!isMain()) {
      throw new IllegalStateException("Method call should happen from the main thread.");
    }
  }

  /**
   * Returns true if called on the main thread.
   */
  static boolean isMain() {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }

  public static boolean isOnClasspath(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Creates a unique device id to anonymously track a user. Only use this as a fallback if {@link
   * #getAdvertisingId(Context)} returns null.
   */
  public static String getDeviceId(Context context) {
    // credit method: Amplitude's Android library

    // Android ID
    // Issues on 2.2, some phones have same Android ID due to manufacturer
    // error
    String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(),
        android.provider.Settings.Secure.ANDROID_ID);

    Set<String> invalidIds = new HashSet<String>();
    invalidIds.add("");
    invalidIds.add("9774d56d682e549c");
    invalidIds.add("unknown");
    invalidIds.add("000000000000000");
    invalidIds.add("Android");
    invalidIds.add("DEFACE");

    if (!isNullOrEmpty(androidId) && !invalidIds.contains(androidId)) {
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

  /**
   * This will check if the Play Service are available on the device and application, and return an
   * advertising ID if so. Must not be called from the main thread.
   */
  public static String getAdvertisingId(Context context) {
    if (!isOnClasspath("com.google.android.gms.ads.identifier.AdvertisingIdClient")) {
      return null;
    } else {
      return AdvertisingIdProvider.get(context);
    }
  }

  /**
   * Retrieve an anonymousId. We'll try to look up the advertising Id, and if that is unavailable,
   * we'll generate a device ID. Must not be called from the main thread.
   */
  public static String getAnonymousId(Context context) {
    String advertisingId = getAdvertisingId(context);
    return isNullOrEmpty(advertisingId) ? getDeviceId(context) : advertisingId;
  }
}
