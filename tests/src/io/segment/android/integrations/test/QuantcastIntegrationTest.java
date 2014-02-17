package io.segment.android.integrations.test;

import io.segment.android.integration.BaseIntegrationTest;
import io.segment.android.integration.Integration;
import io.segment.android.integrations.QuantcastIntegration;
import io.segment.android.models.EasyJSONObject;

public class QuantcastIntegrationTest extends BaseIntegrationTest {

	@Override
	public Integration getIntegration() {
		return new QuantcastIntegration();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("apiKey", "0ni8b9kcadbkirvq-7hqqc0yfvgqb6mk2");
		return settings;
	}

}