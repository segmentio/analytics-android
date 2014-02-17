package io.segment.android.integrations.test;

import io.segment.android.integration.BaseIntegrationTest;
import io.segment.android.integration.Integration;
import io.segment.android.integrations.CrittercismIntegration;
import io.segment.android.models.EasyJSONObject;

import org.junit.Test;

import com.crittercism.app.Crittercism;

public class CrittercismIntegrationTest extends BaseIntegrationTest {

	@Override
	public Integration getIntegration() {
		return new CrittercismIntegration();
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
		
		integration.flush();
	}
	
}
