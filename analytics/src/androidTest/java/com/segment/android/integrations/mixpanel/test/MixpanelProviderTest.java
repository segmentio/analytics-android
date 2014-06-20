package com.segment.android.integrations.mixpanel.test;

import com.segment.android.integration.BaseIntegrationTest;
import com.segment.android.integration.Integration;
import com.segment.android.integrations.MixpanelIntegration;
import com.segment.android.models.EasyJSONObject;


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
