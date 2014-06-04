package com.segment.android.utils;

import android.content.pm.PackageManager;

public class AndroidUtils {

	/**
	 * Returns whether the permission currently exists for this application
	 * @param context
	 * @param permission
	 * @return
	 */
	public static boolean permissionGranted(android.content.Context context, String permission) {
		return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
	}
	

	/**
	 * Returns whether the permissions currently exists for this application
	 * @param context
	 * @param permissions
	 * @return
	 */
	public static boolean permissionGranted(android.content.Context context, String[] permissions) {
		for (String permission : permissions) {
			if (!permissionGranted(context, permission)) return false;
		}
		return true;
	}

}
