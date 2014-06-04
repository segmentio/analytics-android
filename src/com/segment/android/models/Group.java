package com.segment.android.models;

import java.util.Calendar;

import org.json.JSONObject;

public class Group extends BasePayload {

	public final static String ACTION = "group";
	
	private final static String TRAITS_KEY = "traits";
	private final static String GROUP_ID_KEY = "groupId";
	
	public Group (JSONObject obj) {
		super(obj);
	}
	
	public Group(String sessionId, String userId, String groupId, Traits traits, Calendar timestamp,
			Context context) {

		super(sessionId, userId, timestamp, context);

		put("action", ACTION);
		
		setGroupId(groupId);
		setTraits(traits);
	}

	public String getGroupId() {
		return this.optString(GROUP_ID_KEY, null);
	}
	
	public void setGroupId(String groupId) {
		this.put(GROUP_ID_KEY, groupId);
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
