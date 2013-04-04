package io.segment.android.models;

import java.util.Date;

public class Alias extends BasePayload {
	
	private final static String FROM_KEY = "from";
	private final static String TO_KEY = "from";

	public Alias(String from,
				 String to, 
				 Date timestamp,
				 Context context) {
		
		super(null, timestamp, context);

		this.put("action", "alias");
		
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
