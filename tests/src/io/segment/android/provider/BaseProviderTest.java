package io.segment.android.provider;

import io.segment.android.errors.InvalidSettingsException;
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
public abstract class BaseProviderTest 
	extends BaseProviderInitializationActivity {

	@Test
	public void testGetKey() {
		Assert.assertFalse(TextUtils.isEmpty(provider.getKey()));
	}

	@Test
	public void testInvalidState() throws InvalidSettingsException {

		provider.reset();

		Assert.assertEquals(ProviderState.NOT_INITIALIZED, provider.getState());

		// empty json object, should fail
		EasyJSONObject settings = new EasyJSONObject();

		try {
			provider.initialize(settings);
		} catch (InvalidSettingsException e) {
			// do nothing
		}

		Assert.assertEquals(ProviderState.INVALID, provider.getState());
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
		provider.disable();
		Assert.assertEquals(ProviderState.DISABLED, provider.getState());
	}

	@Test
	public void testReady() {
		reachReadyState();
	}

	@Test
	public void testIdentifying() {
		reachReadyState();

		provider.identify(TestCases.identify);
	}

	@Test
	public void testTrack() {
		reachReadyState();

		provider.track(TestCases.track);
	}
	

	@Test
	public void testScreen() {
		reachReadyState();

		provider.screen(TestCases.screen);
	}

	@Test
	public void testAlias() {
		reachReadyState();

		provider.alias(TestCases.alias);
	}

	@Test
	public void testFlushing() {
		reachReadyState();

		provider.flush();
	}

	@Test
	public void testActivityStop() {

		reachReadyState();
		
		Activity activity = getActivity();
		
		provider.onActivityStop(activity);
	}

}
