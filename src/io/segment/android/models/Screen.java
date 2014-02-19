package io.segment.android.models;

import java.util.Calendar;

import org.json.JSONObject;

public class Screen extends Track {
	
	public final static String ACTION = "screen";
	
	private static final String SCREEN_KEY = "screen";
	
	public Screen (JSONObject obj) {
		super(obj);
	}
	
	public Screen(String sessionId,
				 String userId, 
				 String screen, 
				 Props properties, 
				 Calendar timestamp,
				 Context context) {

		super(sessionId, userId, "Viewed " + screen, properties, timestamp, context);

		put("action", ACTION);
		
		setScreen(screen);
	}

	public String getScreen() {
		return this.optString(SCREEN_KEY, null);
	}

	public void setScreen(String screen) {
		this.put(SCREEN_KEY, screen);
	}
}
