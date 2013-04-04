package io.segment.android.models;

import java.util.Date;

public class Identify extends BasePayload {

	@SuppressWarnings("unused")
	private String action = "identify";

	private Traits traits;

	public Identify(String userId, Traits traits, Date timestamp,
			Context context) {

		super(userId, timestamp, context);

		this.traits = traits;
	}

	public Traits getTraits() {
		return traits;
	}

	public void setTraits(Traits traits) {
		this.traits = traits;
	}
}
