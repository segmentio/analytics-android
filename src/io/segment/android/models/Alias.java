package io.segment.android.models;

import java.util.Calendar;

import org.json.JSONObject;

public class Alias extends BasePayload {
	
	public final static String ACTION = "alias";
	
	private final static String FROM_KEY = "from";
	private final static String TO_KEY = "to";
	
	public Alias (JSONObject obj) {
		super(obj);
	}
	
	public Alias(String from,
				 String to, 
				 Calendar timestamp,
				 Context context) {
		
		super(null, timestamp, context);

		put("action", ACTION);
		
		setFrom(from);
		setTo(to);
	}

	public String getFrom() {
		return this.optString(FROM_KEY, null);
	}
	
	public void setFrom(String from) {
		this.put(FROM_KEY, from);
	}
	
	public String getTo() {
		return this.optString(TO_KEY, null);
	}
	
	public void setTo(String to) {
		this.put(TO_KEY, to);
	}

}
