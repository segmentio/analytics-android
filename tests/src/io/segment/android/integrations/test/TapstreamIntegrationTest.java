package io.segment.android.integrations.test;

import io.segment.android.integration.BaseIntegrationTest;
import io.segment.android.integration.Integration;
import io.segment.android.integrations.TapstreamIntegration;
import io.segment.android.models.EasyJSONObject;

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