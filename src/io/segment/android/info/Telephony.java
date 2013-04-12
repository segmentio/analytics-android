package io.segment.android.info;

import io.segment.android.models.EasyJSONObject;

import org.json.JSONObject;

import android.content.Context;
import android.telephony.TelephonyManager;

public class Telephony implements Info<JSONObject> {

	@Override
	public String getKey() {
		return "telephony";
	}

	@Override
	public JSONObject get(Context context) {

		EasyJSONObject telephony = new EasyJSONObject();

		TelephonyManager manager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		
		if (manager != null) {
			telephony.put("carrier",  manager.getNetworkOperatorName());
			
			telephony.put("radio", getRadio(manager));
		}
		
		return telephony;
	}
	
	private String getRadio(TelephonyManager manager) {
		switch(manager.getPhoneType()) {
	        case 0x00000000: 
	            return "none";
	        case 0x00000001:
	            return "gsm";
	        case 0x00000002:
	            return "cdma";
	        case 0x00000003:
	            return "sip";
	        default:
	            return null;
	        }
	}
}
