package com.segment.analytics.wear.model;

import com.segment.analytics.Properties;
import com.segment.analytics.json.JsonMap;

public class WearTrackPayload extends JsonMap {
  private static final String EVENT_KEY = "event";
  private static final String PROPERTIES_KEY = "properties";

  public WearTrackPayload(String json) {
    super(json);
  }

  public WearTrackPayload(String event, Properties properties) {
    put(EVENT_KEY, event);
    put(PROPERTIES_KEY, properties);
  }

  public Properties getProperties() {
    return getJsonMap(PROPERTIES_KEY, Properties.class);
  }

  public String getEvent() {
    return getString(EVENT_KEY);
  }
}
