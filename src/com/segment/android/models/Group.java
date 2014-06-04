package com.segment.android.models;

import org.json.JSONObject;

public class Group extends BasePayload {

	public final static String TYPE = "group";
	
	private final static String USER_ID_KEY = "userId";
	private final static String GROUP_ID_KEY = "groupId";
	private final static String TRAITS_KEY = "traits";
	
	public Group (JSONObject obj) {
		super(obj);
	}
	
	public Group(String userId, String groupId, Traits traits, Options options) {
		super(TYPE, options);
		setUserId(userId);
		setGroupId(groupId);
		setTraits(traits);
	}
	
	public String getUserId() {
		return this.optString(USER_ID_KEY, null);
	}

	public void setUserId(String userId) {
		this.put(USER_ID_KEY, userId);
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
