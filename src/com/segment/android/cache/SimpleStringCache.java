package com.segment.android.cache;

import com.segment.android.Constants;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Simple String cache built over the {@link SharedPreferences}
 *
 */
public class SimpleStringCache {

	private SharedPreferences preferences;
	private String cacheKey;
	
	/**
	 * Creates a simple string cache over {@link SharedPreferences}
	 * @param context
	 * @param cacheKey
	 */
	public SimpleStringCache(Context context, String cacheKey) { 

		String prefKey = Constants.PACKAGE_NAME + "." +  context.getPackageName();
		
		preferences = context.getSharedPreferences(
				 prefKey, android.content.Context.MODE_PRIVATE);
		
		this.cacheKey = cacheKey;
	}
	
	/**
	 * Loads the value that will be put into the cache. Returns null
	 * if the value can't be calculated
	 * @return
	 */
	public String load() {
		return null;
	}
	
	/**
	 * Returns the cached value, or calls load to get the value.
	 * Will save the new one if needs to call load
	 * @return
	 */
	public String get() {
		String val = preferences.getString(cacheKey, null);
		if (val == null) {
			val = load();
			if (val != null) set(val);
		}
		return val;
	}

	/**
	 * Sets the value in the cache
	 * @param val
	 */
	public void set(String val) {
		preferences.edit()
		   		   .putString(cacheKey, val)
		   		   .commit();
	}
	
	/**
	 * Removes the cache key and resets the cache
	 */
	public void reset() {
		preferences.edit().remove(cacheKey).commit();
	}
	
}
