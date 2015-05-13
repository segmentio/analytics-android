/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment, Inc.
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

package com.segment.analytics.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.pm.PackageManager.FEATURE_TELEPHONY;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.provider.Settings.Secure.ANDROID_ID;
import static android.provider.Settings.Secure.getString;

public final class Utils {

  public static final String THREAD_PREFIX = "SegmentAnalytics-";
  public static final int DEFAULT_FLUSH_INTERVAL = 30 * 1000; // 30s
  public static final int DEFAULT_FLUSH_QUEUE_SIZE = 20;
  final static String TAG = "Segment";
  @SuppressLint("SimpleDateFormat") private static final DateFormat ISO_8601_DATE_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  /** Creates a mutable HashSet instance containing the given elements in unspecified order */
  public static <T> Set<T> newSet(T... values) {
    Set<T> set = new HashSet<>(values.length);
    Collections.addAll(set, values);
    return set;
  }

  /** Returns the date as a string formatted with {@link #ISO_8601_DATE_FORMAT}. */
  public static String toISO8601Date(Date date) {
    return ISO_8601_DATE_FORMAT.format(date);
  }

  /** Returns the string as a date parsed with {@link #ISO_8601_DATE_FORMAT}. */
  public static Date toISO8601Date(String date) throws ParseException {
    return ISO_8601_DATE_FORMAT.parse(date);
  }

  /** Returns true if the application has the given permission. */
  public static boolean hasPermission(Context context, String permission) {
    return context.checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED;
  }

  /** Returns true if the application has the given feature. */
  public static boolean hasFeature(Context context, String feature) {
    return context.getPackageManager().hasSystemFeature(feature);
  }

  /** Returns the system service for the given string. */
  @SuppressWarnings("unchecked") public static <T> T getSystemService(Context context,
      String serviceConstant) {
    return (T) context.getSystemService(serviceConstant);
  }

  /** Returns true if the string is null, or empty (once trimmed). */
  public static boolean isNullOrEmpty(CharSequence text) {
    return TextUtils.isEmpty(text) || TextUtils.getTrimmedLength(text) == 0;
  }

  /** Returns true if the collection or has a size 0. */
  public static boolean isNullOrEmpty(Collection collection) {
    return collection == null || collection.size() == 0;
  }

  /** Returns true if the map is null or empty, false otherwise. */
  public static boolean isNullOrEmpty(Map map) {
    return map == null || map.size() == 0;
  }

  /** Creates a unique device id. */
  public static String getDeviceId(Context context) {
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
  public static SharedPreferences getSegmentSharedPreferences(Context context) {
    return context.getSharedPreferences("analytics-android", MODE_PRIVATE);
  }

  /** Get the string resource for the given key. Returns null if not found. */
  public static String getResourceString(Context context, String key) {
    int id = getIdentifier(context, "string", key);
    if (id != 0) {
      return context.getResources().getString(id);
    } else {
      return null;
    }
  }

  /** Get the identifier for the resource with a given type and key. */
  private static int getIdentifier(Context context, String type, String key) {
    return context.getResources().getIdentifier(key, type, context.getPackageName());
  }

  /**
   * Returns {@code true} if the phone is connected to a network, or if we don't have the enough
   * permissions. Returns {@code false} otherwise.
   */
  public static boolean isConnected(Context context) {
    if (!hasPermission(context, ACCESS_NETWORK_STATE)) {
      return true; // assume we have the connection and try to upload
    }
    ConnectivityManager cm = getSystemService(context, CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  /** Return {@code true} if a class with the given name is found. */
  public static boolean isOnClassPath(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException e) {
      // ignored
      return false;
    }
  }

  /**
   * Close the given {@link Closeable}. If an exception is thrown during {@link Closeable#close()},
   * this will quietly ignore it. Does nothing if {@code closeable} is {@code null}.
   */
  public static void closeQuietly(Closeable closeable) {
    if (closeable == null) return;
    try {
      closeable.close();
    } catch (IOException ignored) {
    }
  }

  /** Buffers the given {@code InputStream}. */
  public static BufferedReader buffer(InputStream is) {
    return new BufferedReader(new InputStreamReader(is));
  }

  /** Reads the give {@code InputStream} into a String. */
  public static String readFully(InputStream is) throws IOException {
    return readFully(buffer(is));
  }

  /** Reads the give {@code BufferedReader} into a String. */
  public static String readFully(BufferedReader reader) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (String line; (line = reader.readLine()) != null; ) {
      sb.append(line);
    }
    return sb.toString();
  }

  public static <T> Map<String, T> createMap() {
    return new NullableConcurrentHashMap<>();
  }

  /** Ensures that a directory is created in the given location, throws an IOException otherwise. */
  public static void createDirectory(File location) throws IOException {
    if (!(location.exists() || location.mkdirs() || location.isDirectory())) {
      throw new IOException("Could not create directory at " + location);
    }
  }

  public static void debug(String format, Object... extra) {
    Log.d(TAG, String.format(format, extra));
  }

  public static void error(Throwable error, String format, Object... extra) {
    Log.e(TAG, String.format(format, extra), error);
  }

  private Utils() {
    throw new AssertionError("No instances");
  }

  /**
   * A {@link ThreadPoolExecutor} implementation by {@link com.segment.analytics.Analytics}
   * instances. Exists as a custom type so that we can differentiate the use of defaults versus a
   * user-supplied instance.
   */
  public static class AnalyticsExecutorService extends ThreadPoolExecutor {
    private static final int DEFAULT_THREAD_COUNT = 1;
    // At most we perform two network requests concurrently
    private static final int MAX_THREAD_COUNT = 2;

    public AnalyticsExecutorService() {
      //noinspection Convert2Diamond
      super(DEFAULT_THREAD_COUNT, MAX_THREAD_COUNT, 0, TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<Runnable>(), new AnalyticsThreadFactory());
    }
  }

  public static class AnalyticsThreadFactory implements ThreadFactory {
    @SuppressWarnings("NullableProblems") public Thread newThread(Runnable r) {
      return new AnalyticsThread(r);
    }
  }

  private static class AnalyticsThread extends Thread {
    private static final AtomicInteger SEQUENCE_GENERATOR = new AtomicInteger(1);

    public AnalyticsThread(Runnable r) {
      super(r, THREAD_PREFIX + SEQUENCE_GENERATOR.getAndIncrement());
    }

    @Override public void run() {
      Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
      super.run();
    }
  }

  /** A {@link ConcurrentHashMap} that rejects null keys and values instead of failing. */
  public static class NullableConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {

    public NullableConcurrentHashMap() {
      super();
    }

    public NullableConcurrentHashMap(Map<? extends K, ? extends V> m) {
      super(m);
    }

    @Override public V put(K key, V value) {
      if (key == null || value == null) {
        return null;
      }
      return super.put(key, value);
    }
  }
}
