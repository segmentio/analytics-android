package io.segment.android.integrations.test;

import io.segment.android.integration.BaseIntegrationTest;
import io.segment.android.integration.Integration;
import io.segment.android.integrations.GoogleAnalyticsIntegration;
import io.segment.android.models.EasyJSONObject;

public class GoogleAnalyticsIntegrationTest extends BaseIntegrationTest {

	@Override
	public Integration getIntegration() {
		return new GoogleAnalyticsIntegration();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("mobileTrackingId", "UA-27033709-9");
		settings.put("anonymizeIp", false);
		settings.put("reportUncaughtExceptions", true);
		settings.put("mobileHttps", true);
		return settings;
	}

}