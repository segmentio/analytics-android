package com.segment.android;

import android.util.Log;

public class Logger {

	public static final String TAG = Constants.TAG;
	
	private static boolean log;
	
	/**
	 * Set whether this logger logs (true to log)
	 */
	public static void setLog(boolean log) {
		Logger.log = log;
	}
	
	/**
	 * Get whether this logger logs (true to log)
	 */
	public static boolean isLogging() {
		return Logger.log;
	}
	
	public static void v(String msg) {
		if (log) Log.v(TAG, msg);
	}
	
	public static void v(String msg, Throwable tr) {
		if (log) Log.v(TAG, msg, tr);
	}
	
	public static void d(String msg) {
		if (log) Log.d(TAG, msg);
	}
	
	public static void d(String msg, Throwable tr) {
		if (log) Log.d(TAG, msg, tr);
	}

	public static void i(String msg) {
		if (log) Log.i(TAG, msg);
	}
	
	public static void i(String msg, Throwable tr) {
		if (log) Log.i(TAG, msg, tr);
	}
	
	public static void w(String msg) {
		if (log) Log.w(TAG, msg);
	}
	
	public static void w(String msg, Throwable tr) {
		if (log) Log.w(TAG, msg, tr);
	}
	
	public static void e(String msg) {
		if (log) Log.e(TAG, msg);
	}
	
	public static void e(String msg, Throwable tr) {
		if (log) Log.e(TAG, msg, tr);
	}
	
}
