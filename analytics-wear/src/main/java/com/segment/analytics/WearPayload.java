package com.segment.analytics;

import com.segment.analytics.internal.model.payloads.BasePayload;
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

  WearPayload(@BasePayload.Type int type, ValueMap payload) {
    if (!BasePayload.verifyType(type)) {
      throw new IllegalArgumentException(type + " is not a valid type value see BasePayload.Type");
    }
    put(TYPE_KEY, type);
    put(PAYLOAD_KEY, payload);
  }

  @SuppressWarnings("ResourceType")
  @BasePayload.Type int type() {
    return getInt(TYPE_KEY, BasePayload.TYPE_TRACK);
  }

  <T extends ValueMap> T payload(Class<T> clazz) {
    return getValueMap(PAYLOAD_KEY, clazz);
  }
}
