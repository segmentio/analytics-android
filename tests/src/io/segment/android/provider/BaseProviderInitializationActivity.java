package io.segment.android.provider;

import io.segment.android.Analytics;
import io.segment.android.Options;
import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityUnitTestCase;

/**
 * An Activity Unit Test that's capable of setting up a provider
 * for testing. Doesn't actually include tests, but expects
 * child to do so.
 *
 */
public abstract class BaseProviderInitializationActivity extends
		ActivityUnitTestCase<MockActivity> {

	protected MockActivity activity;
	protected Provider provider;

	public BaseProviderInitializationActivity() {
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

	protected void reachEnabledState() {
		reachInitializedState();

		provider.enable();

		Assert.assertEquals(ProviderState.ENABLED, provider.getState());
	}

	protected void reachReadyState() {
		reachEnabledState();

		// initialize since we can't get the proper context otherwise
		Analytics.initialize(activity, "testsecret", new Options());
		
		provider.onCreate(activity);

		provider.onActivityStart(activity);

		Assert.assertEquals(ProviderState.READY, provider.getState());
	}

	@AfterClass
	protected void tearDown() throws Exception {
		super.tearDown();
		
		provider.flush();
	}
}
