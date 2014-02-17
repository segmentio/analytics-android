package io.segment.android.integrations.test;

import io.segment.android.integration.BaseIntegrationTest;
import io.segment.android.integration.Integration;
import io.segment.android.integrations.OmnitureIntegration;
import io.segment.android.models.EasyJSONObject;

public class OmnitureIntegrationTest extends BaseIntegrationTest {

	@Override
	public Integration getIntegration() {
		return new OmnitureIntegration();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("reportSuiteId", "xxxxxxxxx");
		settings.put("trackingServerUrl", "http://api.omniture.com");
		return settings;
	}

}