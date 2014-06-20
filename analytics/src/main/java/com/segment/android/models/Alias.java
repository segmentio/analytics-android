package com.segment.android.models;

import org.json.JSONObject;

public class Alias extends BasePayload {

  public final static String TYPE = "alias";

  private final static String PREVIOUS_ID_KEY = "previousId";
  private final static String USER_ID_KEY = "userId";

  public Alias(JSONObject obj) {
    super(obj);
  }

  public Alias(String previousId, String userId, Options options) {

    super(TYPE, options);
    setPreviousId(previousId);
    setUserId(userId);
  }

  public String getPreviousId() {
    return this.optString(PREVIOUS_ID_KEY, null);
  }

  public void setPreviousId(String previousId) {
    this.put(PREVIOUS_ID_KEY, previousId);
  }

  public String getUserId() {
    return this.optString(USER_ID_KEY, null);
  }

  public void setUserId(String userId) {
    this.put(USER_ID_KEY, userId);
  }
}
