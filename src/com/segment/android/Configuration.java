package com.segment.android;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

public class Configuration {

	private static final String STRING_RESOURCE_KEY = "string";
	private static final String INTEGER_RESOURCE_KEY = "integer";
	
	private static final String SECRET_KEY = "analytics_secret";
	private static final String WRITE_KEY = "analytics_write_key";
	
	private static final String HOST_KEY = "analytics_host";
	private static final String DEBUG_KEY = "analytics_debug";
	private static final String FLUSH_AT_KEY = "analytics_flush_at";
	private static final String FLUSH_AFTER_KEY = "analytics_flush_after";
	private static final String MAX_QUEUE_SIZE_KEY = "analytics_max_queue_size";
	private static final String SETTINGS_CACHE_EXPIRY_KEY = "analytics_settings_cache_expiry";
	private static final String SETTINGS_SEND_LOCATION_KEY = "analytics_send_location";

	public static String getWriteKey(Context context) {
		String writeKey = getString(context, WRITE_KEY);
		if (writeKey != null) { 
			return writeKey;
		} else {
			return getString(context, SECRET_KEY);
		} 
	}
	
	public static Options getOptions(Context context) {
		Options options = new Options();
		
		String host = getString(context, HOST_KEY);
		if (!TextUtils.isEmpty(host)) options.setHost(host);

		Boolean debug = getBoolean(context, DEBUG_KEY);
		if (debug != null) options.setDebug(debug);
		
		Integer flushAt = getInteger(context, FLUSH_AT_KEY);
		if (flushAt != null) options.setFlushAt(flushAt);
		
		Integer flushAfter = getInteger(context, FLUSH_AFTER_KEY);
		if (flushAfter != null) options.setFlushAfter(flushAfter);	
		
		Integer maxQueueSize = getInteger(context, MAX_QUEUE_SIZE_KEY);
		if (maxQueueSize != null) options.setMaxQueueSize(maxQueueSize);
		
		Integer settingsCacheExpiry = getInteger(context, SETTINGS_CACHE_EXPIRY_KEY);
		if (settingsCacheExpiry != null) options.setSettingsCacheExpiry(settingsCacheExpiry);
		
		Boolean sendLocation = getBoolean(context, SETTINGS_SEND_LOCATION_KEY);
		if (sendLocation != null) options.setSendLocation(sendLocation);
		
		return options;
	}
	
	private static String getString(Context context, String key) {
		Resources resources = context.getResources();
		int id = resources.getIdentifier(key, STRING_RESOURCE_KEY, context.getPackageName());
		if (id > 0) 
			return context.getResources().getString(id);
		else
			return null;
	}
	
	private static Integer getInteger(Context context, String key) {
		Resources resources = context.getResources();
		int id = resources.getIdentifier(key, INTEGER_RESOURCE_KEY, context.getPackageName());
		if (id > 0) 
			return context.getResources().getInteger(id);
		else
			return null;
	}
	
	private static Boolean getBoolean(Context context, String key) {
		Resources resources = context.getResources();
		int id = resources.getIdentifier(key, STRING_RESOURCE_KEY, context.getPackageName());
		if (id > 0) 
			return Boolean.parseBoolean(context.getResources().getString(id));
		else
			return null;
	}
	
}
