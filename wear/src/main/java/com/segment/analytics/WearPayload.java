package com.segment.analytics;

import java.io.IOException;

class WearPayload extends JsonMap {
  /** Type of the payload */
  private static final String TYPE_KEY = "type";

  /** The actual payload */
  private static final String PAYLOAD_KEY = "payload";

  WearPayload(String json) throws IOException {
    super(json);
  }

  WearPayload(BasePayload.Type type, JsonMap payload) {
    put(TYPE_KEY, type);
    put(PAYLOAD_KEY, payload);
  }

  BasePayload.Type type() {
    return getEnum(BasePayload.Type.class, TYPE_KEY);
  }

  <T extends JsonMap> T payload(Class<T> clazz) {
    return getJsonMap(PAYLOAD_KEY, clazz);
  }
}
