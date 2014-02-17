package io.segment.android.integrations.mixpanel.test;

import io.segment.android.integration.BaseIntegrationTest;
import io.segment.android.integration.Integration;
import io.segment.android.integrations.MixpanelIntegration;
import io.segment.android.models.EasyJSONObject;

public class MixpanelProviderTest extends BaseIntegrationTest {

	@Override
	public Integration getIntegration() {
		return new MixpanelIntegration();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("token", "89f86c4aa2ce5b74cb47eb5ec95ad1f9");
		settings.put("people", true);
		return settings;
	}
	
}
