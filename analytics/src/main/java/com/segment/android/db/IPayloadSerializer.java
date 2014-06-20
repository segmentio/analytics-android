package com.segment.android.db;

import com.segment.android.models.BasePayload;

public interface IPayloadSerializer {
  String serialize(BasePayload payload);

  BasePayload deserialize(String str);
}
