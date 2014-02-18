package io.segment.android.integration;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.integration.IntegrationState;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.test.TestCases;
import junit.framework.Assert;

import org.junit.Test;

import android.app.Activity;
import android.text.TextUtils;

/**
 * Automated generic provider activity tests built on 
 * top of {@link BaseProviderInitializationActivity}.
 *
 */
public abstract class BaseIntegrationTest 
	extends BaseIntegrationInitializationActivity {

	@Test
	public void testGetKey() {
		Assert.assertFalse(TextUtils.isEmpty(integration.getKey()));
	}

	@Test
	public void testInvalidState() throws InvalidSettingsException {

		integration.reset();

		Assert.assertEquals(IntegrationState.NOT_INITIALIZED, integration.getState());

		// empty json object, should fail
		EasyJSONObject settings = new EasyJSONObject();

		try {
			integration.initialize(settings);
		} catch (InvalidSettingsException e) {
			// do nothing
		}

		Assert.assertEquals(IntegrationState.INVALID, integration.getState());
	}

	@Test
	public void testValidState() {
		reachInitializedState();
	}

	@Test
	public void testEnabledState() {
		reachEnabledState();
	}

	@Test
	public void testDisabledState() {
		reachEnabledState();
		integration.disable();
		Assert.assertEquals(IntegrationState.DISABLED, integration.getState());
	}

	@Test
	public void testReady() {
		reachReadyState();
	}

	@Test
	public void testIdentifying() {
		reachReadyState();
		integration.identify(TestCases.identify);
	}

	@Test
	public void testTrack() {
		reachReadyState();
		integration.track(TestCases.track);
	}
	

	@Test
	public void testScreen() {
		reachReadyState();
		integration.screen(TestCases.screen);
	}

	@Test
	public void testAlias() {
		reachReadyState();
		integration.alias(TestCases.alias);
	}

	@Test
	public void testFlushing() {
		reachReadyState();
		integration.flush();
	}

	@Test
	public void testActivityStart() {
		reachReadyState();
		Activity activity = getActivity();
		integration.onActivityStart(activity);
	}
	
	@Test
	public void testActivityPause() {
		reachReadyState();
		Activity activity = getActivity();
		integration.onActivityPause(activity);
	}
	
	@Test
	public void testActivityResume() {
		reachReadyState();
		Activity activity = getActivity();
		integration.onActivityResume(activity);
	}
	
	@Test
	public void testActivityStop() {
		reachReadyState();
		Activity activity = getActivity();
		integration.onActivityStop(activity);
	}

}
