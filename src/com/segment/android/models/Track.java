package com.segment.android.models;

import java.util.Calendar;

import org.json.JSONObject;

public class Track extends BasePayload {
	
	public final static String ACTION = "track";
	
	private static final String EVENT_KEY = "event";
	private static final String PROPERTIES_KEY = "properties";
	
	public Track (JSONObject obj) {
		super(obj);
	}
	
	public Track(String sessionId,
				 String userId, 
				 String event, 
				 Props properties, 
				 Calendar timestamp,
				 Context context) {

		super(sessionId, userId, timestamp, context);

		put("action", ACTION);
		
		setEvent(event);
		setProperties(properties);
	}

	public String getEvent() {
		return this.optString(EVENT_KEY, null);
	}

	public void setEvent(String event) {
		this.put(EVENT_KEY, event);
	}

	public Props getProperties() {
		JSONObject object = getObject(PROPERTIES_KEY);
		if (object == null) return null;
		else return new Props(object);
	}

	public void setProperties(Props properties) {
		this.put(PROPERTIES_KEY, properties);
	}
}
