package io.segment.android.info;

import io.segment.android.models.EasyJSONObject;

import org.json.JSONObject;

import android.content.Context;

public class Device implements Info<JSONObject> {

	private SessionId sessionId = new SessionId();
	
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
		device.put("id",  sessionId.get(context));
		
		return device;	
	}

}
