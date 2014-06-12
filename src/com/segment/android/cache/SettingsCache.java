package com.segment.android.cache;


import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import com.segment.android.Logger;
import com.segment.android.cache.ISettingsLayer.SettingsCallback;
import com.segment.android.models.EasyJSONObject;

import android.content.Context;

public class SettingsCache extends SimpleStringCache {

	private static final String CACHE_KEY = "settings";
	
	private static final String SETTINGS_KEY = "settings";
	private static final String LAST_UPDATED_KEY = "lastUpdated";
	
	private int reloads;
	private ISettingsLayer layer;
	private int cacheForMs;
	
	public SettingsCache(Context context, ISettingsLayer layer, int cachedMs) {
		super(context, CACHE_KEY);
		this.layer = layer;
		this.cacheForMs = cachedMs;
	}

	@Override
	public String load() {
		return load(null);
	}
	
	/**
	 * Load the settings asynchronously.
	 * @param callback
	 * @return settings
	 */
	public String load(final SettingsCallback callback) {
		Logger.d("Requesting Segment.io settings ..");
		layer.fetch(new SettingsCallback () {
			@Override
			public void onSettingsLoaded(boolean success, EasyJSONObject settings) {
				if (settings == null) {
					Logger.w("Failed to fetch new Segment.io settings.");
				} else {
					// wrap the settings in a container object containing when
					// it was last updated
					EasyJSONObject container = new EasyJSONObject();
					
					Calendar rightNow = Calendar.getInstance();
					container.put(LAST_UPDATED_KEY, rightNow);
					container.put(SETTINGS_KEY, settings);
					
					reloads += 1;
					
					Logger.d("Successfully fetched new Segment.io settings.");
					
					set(container.toString());
				}
				
				if (callback != null) callback.onSettingsLoaded(success, settings);
			}
		});
		
		return null;
	}
	
	
	private EasyJSONObject parseContainer(String json) {
		if (json != null) {
			try {
				return new EasyJSONObject(new JSONObject(json));
			} catch (JSONException e) {
				Logger.w("Failed to parse json object representing cached settings.");
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the cached value, or calls load to get the value.
	 * Will automatically refresh every cacheForMs, and will save the new value in the cache afterwards. 
	 * @return
	 */
	@Override
	public String get() {
		String json = super.get();
		EasyJSONObject container = parseContainer(json);
		if (container != null) {
			// if we have it cached, let's check its time
			// to make sure its recent
			Calendar rightNow = Calendar.getInstance();
			Calendar lastUpdated = container.getCalendar(LAST_UPDATED_KEY);
			if (lastUpdated != null) {
				long timeCached = rightNow.getTimeInMillis() - lastUpdated.getTimeInMillis();
				if (timeCached > cacheForMs) {
					Logger.d("Segment.io settings cache expired, loading new settings ...");
					// it's been cached too long, we need to refresh
					load();
				}
				
				return container.toString();
			} else {
				Logger.w("Container exists, but without last updated key. JSON: " + 
						container.toString());
			}
		}
		
		return null;
	}

	/**
	 * Returns the settings object
	 * @return
	 */
	public EasyJSONObject getSettings() {
		String json = this.get();
		EasyJSONObject container = parseContainer(json);
		if (container != null) {
			JSONObject settings = container.getObject(SETTINGS_KEY);
			if (settings != null) return new EasyJSONObject(settings);
		}
		
		return null;
	}
	
	public int getReloads() {
		return reloads;
	}
}
