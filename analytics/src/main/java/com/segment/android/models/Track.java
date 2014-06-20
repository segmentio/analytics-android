package com.segment.android.models;

import org.json.JSONObject;

public class Track extends BasePayload {

  public final static String TYPE = "track";

  private final static String USER_ID_KEY = "userId";
  private static final String EVENT_KEY = "event";
  private static final String PROPERTIES_KEY = "properties";

  public Track(JSONObject obj) {
    super(obj);
  }

  public Track(String userId, String event, Props properties, Options options) {

    super(TYPE, options);
    setUserId(userId);
    setEvent(event);
    setProperties(properties);
  }

  public String getUserId() {
    return this.optString(USER_ID_KEY, null);
  }

  public void setUserId(String userId) {
    this.put(USER_ID_KEY, userId);
  }

  public String getEvent() {
    return this.optString(EVENT_KEY, null);
  }

  public void setEvent(String event) {
    this.put(EVENT_KEY, event);
  }

  public Props getProperties() {
    JSONObject object = getObject(PROPERTIES_KEY);
    if (object == null) {
      return null;
    } else {
      return new Props(object);
    }
  }

  public void setProperties(Props properties) {
    this.put(PROPERTIES_KEY, properties);
  }
}
