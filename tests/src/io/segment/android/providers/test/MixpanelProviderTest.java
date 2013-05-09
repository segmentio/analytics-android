package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.MixpanelProvider;

public class MixpanelProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new MixpanelProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("token", "89f86c4aa2ce5b74cb47eb5ec95ad1f9");
		return settings;
	}
	
}
