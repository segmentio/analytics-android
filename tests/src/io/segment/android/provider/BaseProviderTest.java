package io.segment.android.provider;

import io.segment.android.Analytics;
import io.segment.android.Options;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.test.TestCases;
import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.text.TextUtils;

public abstract class BaseProviderTest extends
		ActivityUnitTestCase<MockActivity> {

	protected MockActivity activity;
	protected Provider provider;

	public BaseProviderTest() {
		super(MockActivity.class);
	}

	@BeforeClass
	protected void setUp() throws Exception {
		super.setUp();

		provider = getProvider();

		Context context = getInstrumentation().getTargetContext();
		
		Intent intent = new Intent(context, MockActivity.class);
		startActivity(intent, null, null);
		
		activity = getActivity();
	}

	public abstract Provider getProvider();

	public abstract EasyJSONObject getSettings();

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

	protected void reachInitializedState() {

		EasyJSONObject settings = getSettings();

		provider.reset();

		Assert.assertEquals(ProviderState.NOT_INITIALIZED, provider.getState());

		try {
			provider.initialize(settings);

		} catch (InvalidSettingsException e) {
			Assert.assertTrue("Invalid settings.", false);
		}

		Assert.assertEquals(ProviderState.INITIALIZED, provider.getState());
	}

	@Test
	public void testValidState() {
		reachInitializedState();
	}

	protected void reachEnabledState() {
		reachInitializedState();

		provider.enable();

		Assert.assertEquals(ProviderState.ENABLED, provider.getState());
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

	protected void reachReadyState() {
		reachEnabledState();

		// initialize since we can't get the proper context otherwise
		Analytics.initialize(activity, "testsecret", new Options());
		
		provider.onCreate(activity);

		provider.onActivityStart(activity);

		Assert.assertEquals(ProviderState.READY, provider.getState());
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

	@AfterClass
	protected void tearDown() throws Exception {
		super.tearDown();
		
		provider.flush();
	}
}
