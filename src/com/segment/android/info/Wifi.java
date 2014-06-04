package com.segment.android.info;


import org.json.JSONObject;

import com.segment.android.models.EasyJSONObject;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Wifi implements Info<JSONObject> {

	@Override
	public String getKey() {
		return "wifi";
	}

	@Override
	public JSONObject get(Context context) {

		EasyJSONObject object = new EasyJSONObject();

        if (PackageManager.PERMISSION_GRANTED == context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            
            object.put("available", wifi.isAvailable());
            object.put("connected", wifi.isConnected());
        }
		
		return object;
	}

}
