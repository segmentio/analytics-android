package com.segment.android.models;


import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.segment.android.Logger;
import com.segment.android.utils.ISO8601;

import android.text.TextUtils;
import android.util.Log;

public class EasyJSONObject extends JSONObject {
	
	public EasyJSONObject() {
		super();
	}
	
	public EasyJSONObject(JSONObject obj) {
		super();
		merge(obj);
	}
	
	public EasyJSONObject(Object... kvs) {
		super();

		if (kvs != null) {
			if (kvs.length % 2 != 0) {
				Logger.w("Segment.io objects must be initialized with an " + 
						"even number of arguments, like so: [Key, Value, Key, Value]");	
			} else {
				if (kvs.length > 1) {
					for (int i = 0; i < kvs.length; i += 2) {
						String key = kvs[i].toString();
						Object val = kvs[i+1];
						this.putObject(key, val);
					}
				}
			}
		}
	}

	/**
	 * Shallow merge json obj into this json object. If a key already exists,
	 * then it wont be merged.
	 * @param obj json object to be copied into this object
	 */
	public void merge(JSONObject obj) {
		if (obj != null) {
			@SuppressWarnings("unchecked")
			Iterator<String> keys = obj.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				if (this.has(key)) continue; 
				Object val;
				try {
					val = obj.get(key);
					this.putObject(key, val);
				} catch (JSONException e) {
					Logger.w("JSON object had an invalid value during merge. " + 
						Log.getStackTraceString(e));	
				}
			}
		}
	}

	//
	// Put Handlers
	//
	
	public void put(String key, Calendar calendar) {
		if (calendar == null) {
			this.remove(key);
		} else {
			String timestampStr = ISO8601.fromCalendar(calendar);
			this.putObject(key, timestampStr);
		}
	}
	
	public JSONObject put(String key, int value) {
		return this.putObject(key, value);
	}
	
	public JSONObject put(String key, double value) {
		return this.putObject(key, value);
	}
	
	public JSONObject put(String key, boolean value) {
		return this.putObject(key, value);
	}

	public JSONObject put(String key, String value) {
		return this.putObject(key, value);
	}
	
	public JSONObject put(String key, JSONObject value) {
		return this.putObject(key, value);
	}

	public JSONObject put(String key, JSONArray value) {
		return this.putObject(key, value);
	}
	
	public JSONObject put(String key, Object value) {
		return this.putObject(key, value);
	}
	
	public JSONObject putObject(String key, Object value) {
		try {
			return super.put(key, value);
		} catch (JSONException e) {
			Logger.e("Failed to add json key => value" + 
					String.format("[%s => %s] : ", key, value) + 
					Log.getStackTraceString(e));
		}
		return null;
	}
	
	//
	// Get Handlers
	//
	
	public Calendar getCalendar(String key) {
		String timestampStr = this.optString(key, null);
		if (timestampStr != null) {
			try {
				return ISO8601.toCalendar(timestampStr);
			} catch (ParseException e) {
				Logger.w("Failed to parse timestamp string into ISO 8601 format: " + 
						Log.getStackTraceString(e));
			}	
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> getArray(String key) {
		try {
			JSONArray array = this.getJSONArray(key);
			List<T> list = new LinkedList<T>();
			for (int i = 0; i < array.length(); i += 1) {
				Object obj = array.get(i);
				list.add((T) obj);
			}
			return list;
		} catch (JSONException e) {
			return null;
		}
	}
	
	public JSONObject getObject(String key) {
		try {
			return this.getJSONObject(key);
		} catch (JSONException e) {
			return null;
		}
	}
	
	public String getString(String key) {
		try {
			return super.getString(key);
		} catch (JSONException e) {
			return null;
		}
	}
	
	public String getString(String key, String defaultString) {
		try {
			String str = super.getString(key);
			if (TextUtils.isEmpty(str)) return defaultString; 
			else return str;
		} catch (JSONException e) {
			return defaultString;
		}
	}
	
	public Boolean getBoolean(String key, Boolean defaultBoolean) {
		try {
			return super.getBoolean(key);
		} catch (JSONException e) {
			return defaultBoolean;
		}
	}
	
	public Integer getInt(String key, Integer defaultInteger) {
		try {
			return super.getInt(key);
		} catch (JSONException e) {
			return defaultInteger;
		}
	}
	
	public Double getDouble(String key, Double defaultDouble) {
		try {
			return super.getDouble(key);
		} catch (JSONException e) {
			return defaultDouble;
		}
	}
	
	public Object get(String key) {
		try {
			return super.get(key);
		} catch (JSONException e) {
			Logger.e("Failed to read json key. " + 
					String.format("[%s] : ", key));
		}
		return null;
	}
	
	//
	// Utils
	//
	
	public Map<String, String> toStringMap() {
		Map<String, String> map = new HashMap<String, String>();
	
		@SuppressWarnings("unchecked")
		Iterator<String> it = this.keys();
		while (it.hasNext()) {
			String key = it.next();
			String value = "" + this.get(key);
			if (value.length() > 255) value = value.substring(0, 255);
			map.put(key, value);
		}
		
		return map;
	}

	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof JSONObject)) return false;
		return equals(this, (JSONObject)o);
	}
	
	//
	// Equals Helpers
	//
	
	public static boolean equals(Object oneVal, Object twoVal) {
		if (oneVal == null || twoVal == null) return oneVal == twoVal;
		
		if (oneVal instanceof JSONObject) {
			// its a nested object
			if (!(twoVal instanceof JSONObject)) return false;
			JSONObject oneValObject = (JSONObject) oneVal;
			JSONObject twoValObject = (JSONObject) twoVal;
			
			// call equals recursively
			if (!equals(oneValObject, twoValObject)) return false;
			
		} else if (oneVal instanceof JSONArray) {
			// its an array
			if (!(twoVal instanceof JSONArray)) return false;
			JSONArray oneValArray = (JSONArray) oneVal;
			JSONArray twoValArray = (JSONArray) twoVal;
			
			// call the array equals method
			if (!equals(oneValArray, twoValArray)) return false;
		} else {
			// its a string, float, boolean, int, double, or a nested type
			
			if (!oneVal.equals(twoVal)) return false;
		}
		
		return true;
	}
	
	
	
	public static boolean equals (JSONArray one, JSONArray two) {
		if (one == null || two == null) return one == two;
		if (one.length() != two.length()) return false;
		
		for (int i = 0; i < one.length(); i += 1) {
			try {
				Object oneVal = one.get(i);
				Object twoVal = two.get(i);
				
				if (!equals(oneVal, twoVal)) return false;
				
			} catch (JSONException e) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean equals (JSONObject one, JSONObject two) {
		if (one == null || two == null) return one == two;
		if (one.length() != two.length()) return false;

		@SuppressWarnings("unchecked")
		Iterator<String> iterator = one.keys();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (!two.has(key)) return false;
			else {
				try {
					Object oneVal = one.get(key);
					Object twoVal = two.get(key);
					
					if (!equals(oneVal, twoVal)) return false;
					
				} catch (JSONException e) {
					return false;
				}
			}
		}
		
		return true;
	}
	
}