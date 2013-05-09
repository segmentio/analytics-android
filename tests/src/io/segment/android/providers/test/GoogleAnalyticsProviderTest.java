package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.GoogleAnalyticsProvider;

public class GoogleAnalyticsProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new GoogleAnalyticsProvider();
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