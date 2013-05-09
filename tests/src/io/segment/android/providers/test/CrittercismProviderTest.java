package io.segment.android.providers.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.provider.BaseProviderTest;
import io.segment.android.provider.Provider;
import io.segment.android.providers.CrittercismProvider;

import org.junit.Test;

import com.crittercism.app.Crittercism;

public class CrittercismProviderTest extends BaseProviderTest {

	@Override
	public Provider getProvider() {
		return new CrittercismProvider();
	}

	@Override
	public EasyJSONObject getSettings() {
		EasyJSONObject settings = new EasyJSONObject();
		settings.put("appId", "51832f1a8b2e333349000007");
		settings.put("delaySendingAppLoad", false);
		settings.put("includeVersionCode", true);
		settings.put("shouldCollectLogcat", true);
		return settings;
	}

	@Test
	public void testHandledException() {
		reachReadyState();
		
		Crittercism.logHandledException(new RuntimeException("handled exception!")); 
		
		provider.flush();
	}
	
}
