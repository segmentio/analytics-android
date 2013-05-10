package io.segment.android.models;

import org.json.JSONObject;

public class EventProperties extends EasyJSONObject {

	public EventProperties() {
		super();
	}

	public EventProperties(JSONObject json) {
		super(json);
	}
	
	public EventProperties(Object... kvs) {
		super(kvs);
	}
	
}
