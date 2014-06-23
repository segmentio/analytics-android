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

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.utils.AndroidUtils;
import org.json.JSONObject;

public class Network implements Info<JSONObject> {

  @Override
  public String getKey() {
    return "network";
  }

  @Override
  public JSONObject get(Context context) {
    EasyJSONObject network = new EasyJSONObject();

    if (AndroidUtils.permissionGranted(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
      ConnectivityManager manager =
          (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      if (manager != null) {
        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi != null) network.put("wifi", wifi.isConnected());
        NetworkInfo bluetooth = manager.getNetworkInfo(ConnectivityManager.TYPE_BLUETOOTH);
        if (bluetooth != null) network.put("bluetooth", bluetooth.isConnected());
        NetworkInfo cellular = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (cellular != null) network.put("cellular", cellular.isConnected());
      }
    }

    TelephonyManager telephony =
        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    if (telephony != null) network.put("carrier", telephony.getNetworkOperatorName());

    return network;
  }
}
