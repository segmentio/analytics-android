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
	
}
