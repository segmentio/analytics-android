package io.segment.android.info;

import io.segment.android.models.EasyJSONObject;

import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class Device implements Info<JSONObject> {

	@Override
	public String getKey() {
		return "device";
	}

	@Override
	public JSONObject get(Context context) {

		EasyJSONObject device = new EasyJSONObject();
		
		PackageInfo packageInfo;
		try {
			packageInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			device.put("versionCode", packageInfo.versionCode);
			device.put("versionName", packageInfo.versionName);
			
		} catch (NameNotFoundException e) {}
		
		device.put("sdk", android.os.Build.VERSION.SDK_INT);
		device.put("release",  android.os.Build.VERSION.RELEASE);
		device.put("brand", android.os.Build.BRAND);
		device.put("manufacturer",  android.os.Build.MANUFACTURER);
		
		return device;
	}

}
