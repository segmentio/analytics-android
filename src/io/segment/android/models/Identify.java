package io.segment.android.models;

import java.util.Calendar;

import org.json.JSONObject;

public class Identify extends BasePayload {

	public final static String ACTION = "identify";
	
	private final static String TRAITS_KEY = "traits";

	public Identify (JSONObject obj) {
		super(obj);
	}
	
	public Identify(String sessionId, String userId, Traits traits, Calendar timestamp,
			Context context) {

		super(sessionId, userId, timestamp, context);

		put("action", ACTION);
		
		setTraits(traits);
	}

	public Traits getTraits() {
		JSONObject object = getObject(TRAITS_KEY);
		if (object == null) return null;
		else return new Traits(object);
	}

	public void setTraits(Traits traits) {
		this.put(TRAITS_KEY, traits);
	}
}
