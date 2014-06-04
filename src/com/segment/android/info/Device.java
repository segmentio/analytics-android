package com.segment.android.info;


import org.json.JSONObject;

import com.segment.android.models.EasyJSONObject;

import android.content.Context;

public class Device implements Info<JSONObject> {
	
	@Override
	public String getKey() {
		return "device";
	}

	@Override
	public JSONObject get(Context context) {

		EasyJSONObject device = new EasyJSONObject();
		
		device.put("sdk", android.os.Build.VERSION.SDK_INT);
		device.put("release",  android.os.Build.VERSION.RELEASE);
		device.put("brand", android.os.Build.BRAND);
		device.put("model", android.os.Build.MODEL);
		device.put("manufacturer",  android.os.Build.MANUFACTURER);
		
		return device;	
	}

}
