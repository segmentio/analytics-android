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

package com.segment.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import com.segment.android.internal.util.Logger;
import com.segment.android.internal.util.Utils;
import com.segment.android.json.JsonMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.net.ConnectivityManager.TYPE_BLUETOOTH;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static com.segment.android.internal.util.Utils.getSystemService;

/**
 * Context is a dictionary of extra, free-form information about a specific API call. You can add
 * any custom data to the context dictionary that you'd like to have access to in the raw logs.
 * <p/>
 * Some keys in the context dictionary have semantic meaning and will be collected for you
 * automatically, depending on the library you send data from.Some keys need to be manually
 * entered, such as IP Address, speed, etc.
 * <p/>
 * This is not persisted to disk, and is recomputed each time the app starts. If you set a key
 * manually, you'll have to update it as well for each app start if you want it to persist between
 * sessions.
 */
public class AnalyticsContext extends JsonMap {
  private static final String APP_KEY = "app";
  private static final String APP_NAME_KEY = "name";
  private static final String APP_VERSION_KEY = "version";
  private static final String APP_PACKAGE_NAME_KEY = "packageName";
  private static final String APP_VERSION_CODE_KEY = "versionCode";
  private static final String APP_VERSION_NAME_KEY = "versionName";
  private static final String APP_BUILD_KEY = "build";

  void putApp(Context context) {
    try {
      PackageInfo packageInfo =
          context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      Map<String, Object> app = new LinkedHashMap<String, Object>(6);
      app.put(APP_NAME_KEY, packageInfo.applicationInfo.name);
      app.put(APP_VERSION_KEY, packageInfo.versionName);
      app.put(APP_PACKAGE_NAME_KEY, packageInfo.packageName);
      app.put(APP_VERSION_CODE_KEY, packageInfo.versionCode);
      app.put(APP_VERSION_NAME_KEY, packageInfo.versionName);
      app.put(APP_BUILD_KEY, packageInfo.packageName + '@' + packageInfo.versionCode);
      put(APP_KEY, app);
    } catch (PackageManager.NameNotFoundException e) {
      Logger.e(e, "Could not retrieve package for name %s", context.getPackageName());
    }
  }

  private static final String CAMPAIGN_KEY = "campaign";
  private static final String CAMPAIGN_NAME_KEY = "name";
  private static final String CAMPAIGN_SOURCE_KEY = "source";
  private static final String CAMPAIGN_MEDIUM_KEY = "medium";
  private static final String CAMPAIGN_TERM_KEY = "term";
  private static final String CAMPAIGN_CONTENT_KEY = "content";

  public AnalyticsContext putCampaign(String name, String source, String medium, String term,
      String content) {
    Map<String, Object> campaign = new LinkedHashMap<String, Object>(5);
    campaign.put(CAMPAIGN_NAME_KEY, name);
    campaign.put(CAMPAIGN_SOURCE_KEY, source);
    campaign.put(CAMPAIGN_MEDIUM_KEY, medium);
    campaign.put(CAMPAIGN_TERM_KEY, term);
    campaign.put(CAMPAIGN_CONTENT_KEY, content);
    put(CAMPAIGN_KEY, campaign);
    return this;
  }

  private static final String DEVICE_KEY = "device";
  private static final String DEVICE_ID_KEY = "id";
  private static final String DEVICE_MANUFACTURER_KEY = "manufacturer";
  private static final String DEVICE_MODEL_KEY = "model";
  private static final String DEVICE_NAME_KEY = "name";
  private static final String DEVICE_TYPE_KEY = "type";
  private static final String DEVICE_BRAND_KEY = "brand";
  // Ignored for Android
  // String idfv;
  // String idfa;
  // String adTrackingEnabled;

  void putDevice() {
    Map<String, Object> device = new LinkedHashMap<String, Object>(5);
    device.put(DEVICE_ID_KEY, Build.ID);
    device.put(DEVICE_MANUFACTURER_KEY, Build.MANUFACTURER);
    device.put(DEVICE_MODEL_KEY, Build.MODEL);
    device.put(DEVICE_NAME_KEY, Build.DEVICE);
    // device.put(DEVICE_TYPE_KEY, Build.ID);
    device.put(DEVICE_BRAND_KEY, Build.BRAND);
    put(DEVICE_KEY, device);
  }

  private static final String LIBRARY_KEY = "library";
  private static final String LIBRARY_NAME_KEY = "name";
  private static final String LIBRARY_VERSION_KEY = "version";
  // Android Specific
  private static final String LIBRARY_VERSION_NAME_KEY = "versionName";
  private static final String LIBRARY_DEBUG_KEY = "debug";
  private static final String LIBRARY_BUILD_TYPE_KEY = "buildType";
  private static final String LIBRARY_FLAVOR_KEY = "flavor";
  private static final String LIBRARY_LOGGING = "logging";

  void putLibrary() {
    Map<String, Object> library = new LinkedHashMap<String, Object>(6);
    library.put(LIBRARY_NAME_KEY, "analytics-android");
    library.put(LIBRARY_VERSION_KEY, BuildConfig.VERSION_CODE);
    library.put(LIBRARY_VERSION_NAME_KEY, BuildConfig.VERSION_NAME);
    library.put(LIBRARY_DEBUG_KEY, BuildConfig.DEBUG);
    library.put(LIBRARY_BUILD_TYPE_KEY, BuildConfig.BUILD_TYPE);
    library.put(LIBRARY_FLAVOR_KEY, BuildConfig.FLAVOR);
    library.put(LIBRARY_LOGGING, Logger.isLogging());
    put(LIBRARY_KEY, library);
  }

  private static final String LOCATION_KEY = "location";
  private static final String LOCATION_LATITUDE_KEY = "latitude";
  private static final String LOCATION_LONGITUDE_KEY = "longitude";
  private static final String LOCATION_SPEED_KEY = "speed";

  public AnalyticsContext putLocation(double latitude, double longitude, double speed) {
    Map<String, Double> location = new LinkedHashMap<String, Double>(3);
    location.put(LOCATION_LATITUDE_KEY, latitude);
    location.put(LOCATION_LONGITUDE_KEY, longitude);
    location.put(LOCATION_SPEED_KEY, speed);
    put(LOCATION_KEY, location);
    return this;
  }

  private static final String NETWORK_KEY = "network";
  private static final String NETWORK_BLUETOOTH_KEY = "bluetooth";
  private static final String NETWORK_CARRIER_KEY = "carrier";
  private static final String NETWORK_CELLULAR_KEY = "cellular";
  private static final String NETWORK_WIFI_KEY = "wifi";

  void putNetwork(Context context) {
    Map<String, Object> network = new LinkedHashMap<String, Object>(4);
    if (Utils.hasPermission(context, ACCESS_NETWORK_STATE)) {
      ConnectivityManager connectivityManager = getSystemService(context, CONNECTIVITY_SERVICE);
      if (connectivityManager != null) {
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(TYPE_WIFI);
        network.put(NETWORK_WIFI_KEY, wifiInfo != null && wifiInfo.isConnected());
        NetworkInfo bluetoothInfo = connectivityManager.getNetworkInfo(TYPE_BLUETOOTH);
        network.put(NETWORK_BLUETOOTH_KEY, bluetoothInfo != null && bluetoothInfo.isConnected());
        NetworkInfo cellularInfo = connectivityManager.getNetworkInfo(TYPE_MOBILE);
        network.put(NETWORK_CELLULAR_KEY, cellularInfo != null && cellularInfo.isConnected());
      }
    }

    TelephonyManager telephonyManager = getSystemService(context, TELEPHONY_SERVICE);
    if (telephonyManager != null) {
      network.put(NETWORK_CARRIER_KEY, telephonyManager.getNetworkOperatorName());
    } else {
      network.put(NETWORK_CARRIER_KEY, "unknown");
    }

    put(NETWORK_KEY, network);
  }

  private static final String OS_KEY = "os";
  private static final String OS_NAME_KEY = "name";
  private static final String OS_VERSION_KEY = "version";
  // Android Specific
  private static final String OS_SDK_KEY = "sdk";

  void putOs() {
    Map<String, Object> os = new LinkedHashMap<String, Object>(3);
    os.put(APP_NAME_KEY, Build.VERSION.CODENAME);
    os.put(APP_VERSION_KEY, Build.VERSION.RELEASE);
    os.put(OS_SDK_KEY, Build.VERSION.SDK_INT);
    put(OS_KEY, os);
  }

  private static final String REFERRER_KEY = "referrer";
  private static final String REFERRER_ID_KEY = "id";
  private static final String REFERRER_LINK_KEY = "link";
  private static final String REFERRER_NAME_KEY = "name";
  private static final String REFERRER_TYPE_KEY = "type";
  private static final String REFERRER_URL_KEY = "url";

  public AnalyticsContext putReferrer(String id, String link, String name, String type,
      String url) {
    Map<String, Object> referrer = new LinkedHashMap<String, Object>(5);
    referrer.put(REFERRER_ID_KEY, id);
    referrer.put(REFERRER_LINK_KEY, link);
    referrer.put(REFERRER_NAME_KEY, name);
    referrer.put(REFERRER_TYPE_KEY, type);
    referrer.put(REFERRER_URL_KEY, url);
    put(REFERRER_KEY, referrer);
    return this;
  }

  private static final String SCREEN_KEY = "screen";
  private static final String SCREEN_DENSITY_KEY = "density";
  private static final String SCREEN_HEIGHT_KEY = "height";
  private static final String SCREEN_WIDTH_KEY = "width";
  private static final String SCREEN_DENSITY_DPI_KEY = "densityDpi";
  private static final String SCREEN_DENSITY_BUCKET_KEY = "densityBucket";
  private static final String SCREEN_SCALED_DENSITY_KEY = "scaledDensity";

  void putScreen(Context context) {
    Map<String, Object> screen = new LinkedHashMap<String, Object>(6);
    WindowManager manager = getSystemService(context, Context.WINDOW_SERVICE);
    Display display = manager.getDefaultDisplay();
    DisplayMetrics displayMetrics = new DisplayMetrics();
    display.getMetrics(displayMetrics);
    screen.put(SCREEN_DENSITY_KEY, displayMetrics.density);
    screen.put(SCREEN_HEIGHT_KEY, displayMetrics.heightPixels);
    screen.put(SCREEN_WIDTH_KEY, displayMetrics.widthPixels);
    screen.put(SCREEN_DENSITY_DPI_KEY, displayMetrics.densityDpi);
    screen.put(SCREEN_DENSITY_BUCKET_KEY, getDensityString(displayMetrics));
    screen.put(SCREEN_SCALED_DENSITY_KEY, displayMetrics.scaledDensity);
    put(SCREEN_KEY, screen);
  }

  private static String getDensityString(DisplayMetrics displayMetrics) {
    switch (displayMetrics.densityDpi) {
      case DisplayMetrics.DENSITY_LOW:
        return "ldpi";
      case DisplayMetrics.DENSITY_MEDIUM:
        return "mdpi";
      case DisplayMetrics.DENSITY_HIGH:
        return "hdpi";
      case DisplayMetrics.DENSITY_XHIGH:
        return "xhdpi";
      case DisplayMetrics.DENSITY_XXHIGH:
        return "xxhdpi";
      case DisplayMetrics.DENSITY_XXXHIGH:
        return "xxxhdpi";
      case DisplayMetrics.DENSITY_TV:
        return "tvdpi";
      default:
        return "unknown";
    }
  }

  private static final String LOCALE_KEY = "locale";
  private static final String TRAITS_KEY = "traits";
  private static final String USER_AGENT_KEY = "userAgent";

  static AnalyticsContext singleton = null;

  public static AnalyticsContext with(Context context) {
    if (singleton == null) {
      synchronized (AnalyticsContext.class) {
        if (singleton == null) {
          singleton = new AnalyticsContext(context);
        }
      }
    }
    return singleton;
  }

  private AnalyticsContext(Context context) {
    putApp(context);
    // todo: campaign
    putDevice();
    // todo: ip
    putLibrary();
    put(LOCALE_KEY, Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
    // todo: location
    putNetwork(context);
    putOs();
    // todo: referrer
    putScreen(context);
    // todo: groupId
    put(USER_AGENT_KEY, System.getProperty("http.agent"));
  }

  public AnalyticsContext putTraits(Traits traits) {
    return putValue(TRAITS_KEY, traits);
  }

  @Override public AnalyticsContext putValue(String key, Object value) {
    super.putValue(key, value);
    return this;
  }
}