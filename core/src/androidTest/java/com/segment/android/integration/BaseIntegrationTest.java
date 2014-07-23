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

import android.app.Activity;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.test.TestCases;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Automated generic provider activity tests built on
 * top of {@link BaseIntegrationInitializationActivity}.
 */
public abstract class BaseIntegrationTest extends BaseIntegrationInitializationActivity {

  @Test
  public void testGetKey() {
    assertThat(integration.getKey()).isNotNull();
  }

  @Test
  public void testInvalidState() throws InvalidSettingsException {

    integration.reset();

    assertThat(integration.getState()).isEqualTo(IntegrationState.NOT_INITIALIZED);

    // empty json object, should fail
    EasyJSONObject settings = new EasyJSONObject();

    try {
      integration.initialize(settings);
    } catch (InvalidSettingsException e) {
      // do nothing
    }

    assertThat(integration.getState()).isEqualTo(IntegrationState.INVALID);
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
    assertThat(integration.getState()).isEqualTo(IntegrationState.DISABLED);
  }

  @Test
  public void testReady() {
    reachReadyState();
  }

  @Test
  public void testIdentifying() {
    reachReadyState();
    integration.identify(TestCases.identify());
  }

  @Test
  public void testGroup() {
    reachReadyState();
    integration.group(TestCases.group());
  }

  @Test
  public void testTrack() {
    reachReadyState();
    integration.track(TestCases.track());
  }

  @Test
  public void testScreen() {
    reachReadyState();
    integration.screen(TestCases.screen());
  }

  @Test
  public void testAlias() {
    reachReadyState();
    integration.alias(TestCases.alias());
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
