package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.QuantcastProvider;

public class QuantcastProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new QuantcastProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("apiKey", "0ni8b9kcadbkirvq-7hqqc0yfvgqb6mk2");
		return settings;
	}

}