package io.segment.android.models;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class EasyJsonObject extends JSONObject {

	private static final String TAG = EasyJsonObject.class.getName();
	
	public EasyJsonObject() {
		super();
	}
	
	public EasyJsonObject(Object... kvs) {
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


	public JSONObject put(String key, int value) {
		return this.put(key, value);
	}
	
	public JSONObject put(String key, double value) {
		return this.put(key, value);
	}
	
	public JSONObject put(String key, boolean value) {
		return this.put(key, value);
	}
	
	@Override
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