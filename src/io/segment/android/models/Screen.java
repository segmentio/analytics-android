package io.segment.android.models;

import java.util.Calendar;

import org.json.JSONObject;

public class Screen extends Track {
	
	public final static String ACTION = "screen";
	
	private static final String NAME_KEY = "name";
	
	public Screen (JSONObject obj) {
		super(obj);
	}
	
	public Screen(String sessionId,
				 String userId, 
				 String name, 
				 Props properties, 
				 Calendar timestamp,
				 Context context) {

		super(sessionId, userId, "Viewed " + name + " Screen", properties, timestamp, context);

		put("action", ACTION);
		
		setName(name);
	}

	public String getName() {
		return this.optString(NAME_KEY, null);
	}

	public void setName(String name) {
		this.put(NAME_KEY, name);
	}
}
