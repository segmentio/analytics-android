package io.segment.android.models;

import io.segment.android.utils.ISO8601;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class EasyJSONObject extends JSONObject {

	private static final String TAG = EasyJSONObject.class.getName();
	
	public EasyJSONObject() {
		super();
	}
	
	public EasyJSONObject(Object... kvs) {
		super();

		if (kvs != null) {
			if (kvs.length % 2 != 0) {
				Log.w(TAG, "Segment.io objects must be initialized with an " + 
						"even number of arguments, like so: [Key, Value, Key, Value]");	
			} else {
				if (kvs.length > 1) {
					for (int i = 0; i < kvs.length; i += 2) {
						String key = kvs[i].toString();
						Object val = kvs[i+1];
						this.put(key, val);
					}
				}
			}
		}
	}


	//
	// Put Handlers
	//
	
	public void put(String key, Date value) {
		if (value == null) {
			this.remove(key);
		} else {
	        Calendar calendar = GregorianCalendar.getInstance();
	        calendar.setTime(value);
			String timestampStr = ISO8601.fromCalendar(calendar);
			this.put(key, timestampStr);
		}
	}
	
	public JSONObject put(String key, int value) {
		return this.put(key, value);
	}
	
	public JSONObject put(String key, double value) {
		return this.put(key, value);
	}
	
	public JSONObject put(String key, boolean value) {
		return this.put(key, value);
	}
	
	//
	// Get Handlers
	//
	
	public Date getDate(String key) {
		String timestampStr = this.optString(key, null);
		if (timestampStr != null) {
			try {
				Calendar cal = ISO8601.toCalendar(timestampStr);
				if (cal != null) {
					return cal.getTime();
				}
			} catch (ParseException e) {
				Log.w(TAG, "Failed to parse timestamp string into ISO 8601 format: " + 
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
	
	@SuppressWarnings("unchecked")
	public <T> T getObject(String key) {
		try {
			JSONObject obj = this.getJSONObject(key);
			return (T) obj;
		} catch (JSONException e) {
			return null;
		}
	}
	

	public Object get(String key) {
		try {
			return super.get(key);
		} catch (JSONException e) {
			Log.e(TAG, "Failed to read json key. " + 
					String.format("[%s] : ", key) + 
					Log.getStackTraceString(e));
		}
		return null;
	}
	
	public JSONObject put(String key, Object value) {
		try {
			return super.put(key, value);
		} catch (JSONException e) {
			Log.e(TAG, "Failed to add json key => value" + 
					String.format("[%s => %s] : ", key, value) + 
					Log.getStackTraceString(e));
		}
		return null;
	}
}