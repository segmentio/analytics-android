package com.segment.analytics;

import java.io.IOException;
import java.util.Map;

class WearPayload extends ValueMap {
  /** Type of the payload */
  private static final String TYPE_KEY = "type";

  /** The actual payload */
  private static final String PAYLOAD_KEY = "payload";

  WearPayload(Map<String, Object> delegate) throws IOException {
    super(delegate);
  }

  WearPayload(BasePayload.Type type, ValueMap payload) {
    put(TYPE_KEY, type);
    put(PAYLOAD_KEY, payload);
  }

  BasePayload.Type type() {
    return getEnum(BasePayload.Type.class, TYPE_KEY);
  }

  <T extends ValueMap> T payload(Class<T> clazz) {
    return getValueMap(PAYLOAD_KEY, clazz);
  }
}
