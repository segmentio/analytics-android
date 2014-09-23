package com.segment.analytics;

class WearTrackPayload extends JsonMap {
  private static final String EVENT_KEY = "event";
  private static final String PROPERTIES_KEY = "properties";

  WearTrackPayload(String json) {
    super(json);
  }

  WearTrackPayload(String event, Properties properties) {
    put(EVENT_KEY, event);
    put(PROPERTIES_KEY, properties);
  }

  Properties getProperties() {
    return getJsonMap(PROPERTIES_KEY, Properties.class);
  }

  String getEvent() {
    return getString(EVENT_KEY);
  }
}
