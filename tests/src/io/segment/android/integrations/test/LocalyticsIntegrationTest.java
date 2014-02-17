package io.segment.android.integrations.test;

import io.segment.android.integration.BaseIntegrationTest;
import io.segment.android.integration.Integration;
import io.segment.android.integrations.LocalyticsIntegration;
import io.segment.android.models.EasyJSONObject;

public class LocalyticsIntegrationTest extends BaseIntegrationTest {

	@Override
	public Integration getIntegration() {
		return new LocalyticsIntegration();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("appKey", "3ccfac0c5c366f11105f26b-c8ab109c-b6e1-11e2-88e8-005cf8cbabd8");
		return settings;
	}
	
}
