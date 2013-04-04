package io.segment.android.models;

import java.util.Date;

public class Alias extends BasePayload {
	
	@SuppressWarnings("unused")
	private String action = "alias";

	private String from;
	private String to;

	public Alias(String from,
				 String to, 
				 Date timestamp,
				 Context context) {

		super(null, timestamp, context);

		this.from = from;
		this.to = to;
	}

	public String getFrom() {
		return from;
	}
	
	public void setFrom(String from) {
		this.from = from;
	}
	
	public String getTo() {
		return to;
	}
	
	public void setTo(String to) {
		this.to = to;
	}

}
