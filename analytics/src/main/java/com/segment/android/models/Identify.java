package com.segment.android.models;

import org.json.JSONObject;

public class Identify extends BasePayload {

  public final static String TYPE = "identify";

  private final static String USER_ID_KEY = "userId";
  private final static String TRAITS_KEY = "traits";

  public Identify(JSONObject obj) {
    super(obj);
  }

  public Identify(String userId, Traits traits, Options options) {
    super(TYPE, options);
    setUserId(userId);
    setTraits(traits);
  }

  public String getUserId() {
    return this.optString(USER_ID_KEY, null);
  }

  public void setUserId(String userId) {
    this.put(USER_ID_KEY, userId);
  }

  public Traits getTraits() {
    JSONObject object = getObject(TRAITS_KEY);
    if (object == null) {
      return null;
    } else {
      return new Traits(object);
    }
  }

  public void setTraits(Traits traits) {
    this.put(TRAITS_KEY, traits);
  }
}
