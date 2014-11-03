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

package com.segment.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.pm.PackageManager.FEATURE_TELEPHONY;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.Settings.Secure.ANDROID_ID;
import static android.provider.Settings.Secure.getString;

final class Utils {
  static final String THREAD_PREFIX = "SegmentAnalytics-";
  static final DateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  final static String OWNER_MAIN = "Main";
  final static String OWNER_SEGMENT = "Dispatcher";
  final static String OWNER_INTEGRATION_MANAGER = "IntegrationManager";
  final static String VERB_CREATE = "create";
  final static String VERB_DISPATCH = "dispatch";
  final static String VERB_ENQUEUE = "enqueue";
  final static String VERB_FLUSH = "flush";
  final static String VERB_SKIP = "skip";
  final static String VERB_INITIALIZE = "initialize";
  final static String TAG = "Segment";
  // [thread] [verb] [id] {[extras]}
  final static String FORMAT = "%1$-20s %2$-12s %3$-36s {%4$s}";
  final static String EMPTY = "";

  private Utils() {
    throw new AssertionError("No instances");
  }

  /** Returns the date as a string formatted with {@link ISO_8601_DATE_FORMAT}. */
  static String toISO8601Date(Date date) {
    return ISO_8601_DATE_FORMAT.format(date);
  }

  /** Returns the date parsed with {@link ISO_8601_DATE_FORMAT}. */
  static Date fromISO8601Date(String date) {
    try {
      return ISO_8601_DATE_FORMAT.parse(date);
    } catch (ParseException e) {
      return null;
    }
  }

  /** Returns a {@code Set} of the objects in the specified array. */
  static <T> Set<T> asSet(T... array) {
    Set<T> set = new HashSet<T>();
    Collections.addAll(set, array);
    return set;
  }

  /** Returns true if the application has the given permission. */
  static boolean hasPermission(Context context, String permission) {
    return context.checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED;
  }

  /** Returns true if the application has the given feature. */
  static boolean hasFeature(Context context, String feature) {
    return context.getPackageManager().hasSystemFeature(feature);
  }

  /** Returns the system service for the given string. */
  @SuppressWarnings("unchecked")
  static <T> T getSystemService(Context context, String serviceConstant) {
    return (T) context.getSystemService(serviceConstant);
  }

  /** Returns true if the string is null, or empty (once trimmed). */
  static boolean isNullOrEmpty(String text) {
    // use getTrimmedLength to avoid allocating an extra string object
    return TextUtils.isEmpty(text) || TextUtils.getTrimmedLength(text) == 0;
  }

  /** Returns true if the collection or has a size 0. */
  static boolean isNullOrEmpty(Collection collection) {
    return collection == null || collection.size() == 0;
  }

  /** Returns true if the map is null or empty, false otherwise. */
  static boolean isNullOrEmpty(Map map) {
    return map == null || map.size() == 0;
  }

  /** Creates a unique device id to anonymously identify a user. */
  static String getDeviceId(Context context) {
    String androidId = getString(context.getContentResolver(), ANDROID_ID);
    if (!isNullOrEmpty(androidId) && !"9774d56d682e549c".equals(androidId) && !"unknown".equals(
        androidId) && !"000000000000000".equals(androidId)) {
      return androidId;
    }

    // Serial number, guaranteed to be on all non phones in 2.3+
    if (!isNullOrEmpty(Build.SERIAL)) {
      return Build.SERIAL;
    }

    // Telephony ID, guaranteed to be on all phones, requires READ_PHONE_STATE permission
    if (hasPermission(context, READ_PHONE_STATE) && hasFeature(context, FEATURE_TELEPHONY)) {
      TelephonyManager telephonyManager = getSystemService(context, TELEPHONY_SERVICE);
      String telephonyId = telephonyManager.getDeviceId();
      if (!isNullOrEmpty(telephonyId)) {
        return telephonyId;
      }
    }

    // If this still fails, generate random identifier that does not persist across installations
    return UUID.randomUUID().toString();
  }

  /** Returns a shared preferences for storing any library preferences. */
  static SharedPreferences getSharedPreferences(Context context) {
    return context.getSharedPreferences("analytics-android", MODE_PRIVATE);
  }

  /** Get the string resource for the given key. Returns null if not found. */
  static String getResourceString(Context context, String key) {
    int id = getIdentifier(context, "string", key);
    if (id != 0) {
      return context.getResources().getString(id);
    } else {
      return null;
    }
  }

  /**
   * Get the boolean resource for the given key. Throws {@link Resources.NotFoundException} if not
   * found.
   */
  static boolean getResourceBooleanOrThrow(Context context, String key) {
    int id = getIdentifier(context, "bool", key);
    if (id != 0) {
      return context.getResources().getBoolean(id);
    } else {
      // We won't ever have an error thrown since we check the id first, so we'll re-throw it up
      throw new Resources.NotFoundException("boolean with key " + key + " not found in resources");
    }
  }

  /**
   * Get the integer resource for the given key.Throws {@link Resources.NotFoundException} if not
   * found.
   */
  static int getResourceIntegerOrThrow(Context context, String key) {
    int id = getIdentifier(context, "integer", key);
    if (id != 0) {
      return context.getResources().getInteger(id);
    } else {
      // We won't ever have an error thrown since we check the id first, so we'll re-throw it up
      throw new Resources.NotFoundException("integer with key " + key + " not found in resources");
    }
  }

  /** Get the identifier for the resource with a given type and key. */
  private static int getIdentifier(Context context, String type, String key) {
    return context.getResources().getIdentifier(key, type, context.getPackageName());
  }

  /**
   * Returns true if the phone is connected to a network, or if we don't have the permission to
   * find
   * out. False otherwise.
   */
  static boolean isConnected(Context context) {
    if (!hasPermission(context, ACCESS_NETWORK_STATE)) {
      return true; // assume we have the connection and try to upload
    }
    ConnectivityManager cm = getSystemService(context, CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  /** Quit a thread safely if possible. */
  static void quitThread(HandlerThread thread) {
    if (Build.VERSION.SDK_INT < 18) {
      thread.quit();
    } else {
      thread.quitSafely();
    }
  }

  /** Panic from an unrecoverable error. */
  static void panic(final String string) {
    Analytics.HANDLER.post(new Runnable() {
      @Override public void run() {
        throw new AssertionError(string);
      }
    });
  }

  static boolean isOnClassPath(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException e) {
      // ignored
      return false;
    }
  }

  /** Throw an error if not called on main thread. */
  static void checkMain() {
    if (!isMain()) {
      throw new IllegalStateException("Method call should happen from the main thread.");
    }
  }

  private static boolean isMain() {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }

  /** Call only if debugging is enabled. */
  static void debug(String owner, String verb, String id, String extras) {
    Log.d(TAG, String.format(FORMAT, owner, verb, id == null ? EMPTY : id,
        extras == null ? EMPTY : extras));
  }

  /** Call only if debugging is enabled. */
  static void error(String owner, String verb, String id, Throwable throwable, String extras) {
    Log.e(TAG, String.format(FORMAT, owner, verb + " (error)", id == null ? EMPTY : id,
            extras == null ? EMPTY : extras) + Log.getStackTraceString(throwable)
    );
  }
}
