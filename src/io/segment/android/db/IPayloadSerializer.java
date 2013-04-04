package io.segment.android.db;

import io.segment.android.models.BasePayload;

public interface IPayloadSerializer {

	public String serialize(BasePayload payload);
	
	public BasePayload deseralize(String str);
	
}
