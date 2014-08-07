/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.android.integration;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import com.segment.android.Analytics;
import com.segment.android.Config;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.models.EasyJSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * An Activity Unit Test that's capable of setting up a provider
 * for testing. Doesn't actually include tests, but expects
 * child to do so.
 */
public abstract class BaseIntegrationInitializationActivity
    extends ActivityUnitTestCase<MockActivity> {

  protected MockActivity activity;
  protected Integration integration;

  public BaseIntegrationInitializationActivity() {
    super(MockActivity.class);
  }

  @BeforeClass
  protected void setUp() throws Exception {
    super.setUp();

    integration = getIntegration();

    Context context = getInstrumentation().getTargetContext();
    System.setProperty("dexmaker.dexcache", context.getCacheDir().getPath());

    Intent intent = new Intent(context, MockActivity.class);
    startActivity(intent, null, null);

    activity = getActivity();
  }

  public abstract Integration getIntegration();

  public abstract EasyJSONObject getSettings();

  protected void reachInitializedState() {

    EasyJSONObject settings = getSettings();

    integration.reset();

    assertThat(integration.getState()).isEqualTo(IntegrationState.NOT_INITIALIZED);

    try {
      integration.initialize(settings);
    } catch (InvalidSettingsException e) {
      fail("invalid settings");
    }

    assertThat(integration.getState()).isEqualTo(IntegrationState.INITIALIZED);
  }

  protected void reachEnabledState() {
    reachInitializedState();

    integration.enable();

    assertThat(integration.getState()).isEqualTo(IntegrationState.ENABLED);
  }

  protected void reachReadyState() {
    reachEnabledState();

    // initialize since we can't get the proper context otherwise
    Analytics.initialize(activity, "testsecret", new Config());

    integration.onCreate(activity);

    integration.onActivityStart(activity);

    assertThat(integration.getState()).isEqualTo(IntegrationState.READY);
  }

  @AfterClass
  protected void tearDown() throws Exception {
    super.tearDown();

    integration.flush();
  }
}
