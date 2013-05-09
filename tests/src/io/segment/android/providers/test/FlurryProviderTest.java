package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.FlurryProvider;

public class FlurryProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new FlurryProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("apiKey", "X4Z6T4FZFCQF6TNGMZ93");
		settings.put("sessionLength", 30);
		settings.put("captureUncaughtExceptions", true);
		settings.put("useHttps", true);
		return settings;
	}

}