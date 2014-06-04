package com.segment.android.models;

import java.util.Calendar;

import org.json.JSONObject;

public class Options extends EasyJSONObject {

	private static final String ANONYMOUS_ID_KEY = "anonymousId";
	private static final String TIMESTAMP_KEY = "timestamp";
	private static final String INTEGRATIONS_KEY = "integrations";
	private static final String CONTEXT_KEY = "context";

	public Options() {
		super();
		initialize();
	}

	public Options(JSONObject obj) {
		super(obj);
		initialize();
	}

	public Options(Object... kvs) {
		super(kvs);
		initialize();
	}
	
	private void initialize() {
		if (this.getIntegrations() == null) this.put(INTEGRATIONS_KEY, new EasyJSONObject());
		if (this.getContext() == null) this.put(CONTEXT_KEY, new Context());
		if (this.getTimestamp() == null) this.put(TIMESTAMP_KEY, Calendar.getInstance());
	}


	public Options setAnonymousId(String anonymousId) {
		this.put(ANONYMOUS_ID_KEY, anonymousId);
		return this;
	}

	public Options setTimestamp(Calendar timestamp) {
		this.put(TIMESTAMP_KEY, timestamp);
		return this;
	}

	public Options setIntegration(String key, boolean enabled) {
		this.getIntegrations().put(key, enabled);
		return this;
	}
	
	public Options setContext(Context context) {
		this.put(CONTEXT_KEY, context);
		return this;
	}
	
	@Override
	public Options put(String key, Object value) {
		super.putObject(key, value);
		return this;
	}
	
	public String getAnonymousId() {
		return this.getString(ANONYMOUS_ID_KEY);
	}
	
	public Calendar getTimestamp() {
		return this.getCalendar(TIMESTAMP_KEY);
	}

	public EasyJSONObject getIntegrations() {
		return (EasyJSONObject) this.getObject(INTEGRATIONS_KEY);
	}
	
	public Context getContext() {
		return (Context) this.getObject(CONTEXT_KEY);
	}
	
}