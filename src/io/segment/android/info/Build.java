package io.segment.android.info;

import io.segment.android.models.EasyJSONObject;

import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class Build implements Info<JSONObject> {

	@Override
	public String getKey() {
		return "build";
	}

	@Override
	public JSONObject get(Context context) {

		EasyJSONObject build = new EasyJSONObject();
		
		PackageInfo packageInfo;
		try {
			packageInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			build.put("code", packageInfo.versionCode);
			build.put("name", packageInfo.versionName);
			
		} catch (NameNotFoundException e) {}
	
		return build;
	}

}
