package io.segment.android.models;

import java.util.Date;

public class Track extends BasePayload {
	
	@SuppressWarnings("unused")
	private String action = "track";

	private String event;
	private EventProperties properties;

	public Track(String userId, 
				 String event, 
				 EventProperties properties, 
				 Date timestamp,
				 Context context) {

		super(userId, timestamp, context);

		this.event = event;
		this.properties = properties;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public EventProperties getProperties() {
		return properties;
	}

	public void setProperties(EventProperties properties) {
		this.properties = properties;
	}

}
