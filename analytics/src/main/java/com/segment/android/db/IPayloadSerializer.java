package com.segment.android.db;

import com.segment.android.models.BasePayload;

public interface IPayloadSerializer {

  public String serialize(BasePayload payload);

  public BasePayload deseralize(String str);
}
