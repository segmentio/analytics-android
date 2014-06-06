package com.segment.android.utils;

import java.util.UUID;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.segment.android.Constants;

public class DeviceId {

	/**
	 * Get a unique device id.
	 * @param context Android Activity context.
	 * @return the unique device ID
	 */
	public static String get (Context context) {
		// credit method: Amplitude's Android library
		
		// Android ID
		// Issues on 2.2, some phones have same Android ID due to manufacturer
		// error
		String androidId = android.provider.Settings.Secure.getString(
				context.getContentResolver(),
				android.provider.Settings.Secure.ANDROID_ID);
		
		if (!(TextUtils.isEmpty(androidId) || androidId.equals("9774d56d682e549c"))) {
			return androidId;
		}

		// Serial number
		// Guaranteed to be on all non phones in 2.3+
		try {
			String serialNumber = (String) Build.class.getField("SERIAL").get(null);
			if (!TextUtils.isEmpty(serialNumber)) {
				return serialNumber;
			}
		} catch (Exception e) {}

		// Telephony ID
		// Guaranteed to be on all phones, requires READ_PHONE_STATE permission
		if (AndroidUtils.permissionGranted(context, Constants.Permission.READ_PHONE_STATE) && 
			context.getPackageManager().hasSystemFeature("android.hardware.telephony")) {
			
			TelephonyManager telephone = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
			String telephonyId = telephone.getDeviceId();
			if (!TextUtils.isEmpty(telephonyId)) {
				return telephonyId;
			}
		}

		// If this still fails, generate random identifier that does not persist
		// across installations
		return  UUID.randomUUID().toString();
	}
	
}
