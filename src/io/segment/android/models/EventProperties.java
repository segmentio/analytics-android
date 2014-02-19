package io.segment.android.models;

import org.json.JSONObject;

/**
 * @deprecated  As of release 0.6.0, replaced by {@link Props}
 */
public class EventProperties extends Props {

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
