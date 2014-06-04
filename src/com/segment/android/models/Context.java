package com.segment.android.models;

import org.json.JSONObject;

import com.segment.android.Analytics;

public class Context extends EasyJSONObject {

	private static final String LIBRARY_KEY = "library";
	private static final String LIBRARY_NAME_KEY = "name";
	private static final String LIBRARY_VERSION_KEY = "version";
	
	private static final String LIBRARY_NAME_VALUE = "analytics-android";

	public Context() {
		super();
		addLibraryContext();
	}

	public Context(JSONObject obj) {
		super(obj);
		addLibraryContext();
	}

	public Context(Object... kvs) {
		super(kvs);
		addLibraryContext();
	}

	private void addLibraryContext() {
		this.putObject(LIBRARY_KEY, new Props()
			.put(LIBRARY_NAME_KEY, LIBRARY_NAME_VALUE)
			.put(LIBRARY_VERSION_KEY, Analytics.VERSION));
	}
	
}