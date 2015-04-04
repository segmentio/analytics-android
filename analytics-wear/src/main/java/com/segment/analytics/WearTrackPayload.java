package com.segment.analytics;

import java.util.Map;

class WearTrackPayload extends ValueMap {

  private static final String EVENT_KEY = "event";
  private static final String PROPERTIES_KEY = "properties";

  // For deserialization
  WearTrackPayload(Map<String, Object> delegate) {
    super(delegate);
  }

  WearTrackPayload(String event, Properties properties) {
    put(EVENT_KEY, event);
    put(PROPERTIES_KEY, properties);
  }

  Properties getProperties() {
    return getValueMap(PROPERTIES_KEY, Properties.class);
  }

  String getEvent() {
    return getString(EVENT_KEY);
  }
}
