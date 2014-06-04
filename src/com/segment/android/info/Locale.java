package com.segment.android.info;


import org.json.JSONObject;

import com.segment.android.models.EasyJSONObject;

import android.content.Context;
import android.telephony.TelephonyManager;

public class Locale implements Info<JSONObject> {

	@Override
	public String getKey() {
		return "locale";
	}

	@Override
	public JSONObject get(Context context) {

		EasyJSONObject locale = new EasyJSONObject();

		TelephonyManager manager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		
		locale.put("carrier",  manager.getNetworkOperatorName());
		locale.put("country",  java.util.Locale.getDefault().getDisplayCountry());
		locale.put("language", java.util.Locale.getDefault().getDisplayLanguage());
		
		return locale;
	}
}
