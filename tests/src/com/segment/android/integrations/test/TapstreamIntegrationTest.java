package com.segment.android.integrations.test;

import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.TapstreamIntegration;
import com.segment.android.models.EasyJSONObject;


public class TapstreamIntegrationTest extends BaseIntegrationTest {

	@Override
	public Integration getIntegration() {
		return new TapstreamIntegration();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("accountName", "segmentio");
		settings.put("sdkSecret", "FHH7ypHPQQ2EyNxGIidatQ");
		return settings;
	}

}