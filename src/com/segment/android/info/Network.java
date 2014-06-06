package com.segment.android.info;

import org.json.JSONObject;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.segment.android.models.EasyJSONObject;
import com.segment.android.utils.AndroidUtils;

public class Network implements Info<JSONObject> {
	
	@Override
	public String getKey() {
		return "network";
	}

	@Override
	public JSONObject get(Context context) {
		EasyJSONObject network = new EasyJSONObject();

        if (AndroidUtils.permissionGranted(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager != null) {
	            NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	            if (wifi != null) network.put("wifi", wifi.isConnected());
            	NetworkInfo bluetooth = manager.getNetworkInfo(ConnectivityManager.TYPE_BLUETOOTH);
            	if (bluetooth != null) network.put("bluetooth", bluetooth.isConnected());
            	NetworkInfo cellular = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            	if (cellular != null) network.put("cellular", cellular.isConnected());
            }
        }
     
		TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (telephony != null) network.put("carrier",  telephony.getNetworkOperatorName());
		
		return network;	
	}
}
