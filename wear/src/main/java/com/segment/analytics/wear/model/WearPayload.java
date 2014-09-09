package com.segment.analytics.wear.model;

import com.segment.analytics.internal.payload.BasePayload;
import com.segment.analytics.json.JsonMap;

public class WearPayload extends JsonMap {
  /** Type of the payload */
  private static final String TYPE_KEY = "type";

  /** The actual payload */
  private static final String PAYLOAD_KEY = "payload";

  public WearPayload(String json) {
    super(json);
  }

  public WearPayload(BasePayload.Type type, JsonMap payload) {
    put(TYPE_KEY, type);
    put(PAYLOAD_KEY, payload);
  }

  public BasePayload.Type type() {
    return getEnum(BasePayload.Type.class, TYPE_KEY);
  }

  public <T extends JsonMap> T payload(Class<T> clazz) {
    return getJsonMap(PAYLOAD_KEY, clazz);
  }
}
