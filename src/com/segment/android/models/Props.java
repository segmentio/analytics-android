package com.segment.android.models;

import org.json.JSONObject;

public class Props extends EasyJSONObject {

	public Props() {
		super();
	}

	public Props(JSONObject json) {
		super(json);
	}
	
	public Props(Object... kvs) {
		super(kvs);
	}
	
	@Override
	public Props put(String key, String value) {
		super.put(key, value);
		return this;
	}

	@Override
	public Props put(String key, Object value) {
		super.putObject(key, value);
		return this;
	}
}