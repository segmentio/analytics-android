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
import com.segment.android.models.Alias;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;

/**
 * A provider override that doesn't require that you implement every provider
 * method, except for the essential ones.
 */
public abstract class SimpleIntegration extends Integration {

  @Override
  public String[] getRequiredPermissions() {
    return new String[0];
  }

  @Override
  public void onActivityStart(Activity activity) {
    // do nothing
  }

  @Override
  public void onActivityPause(Activity activity) {
    // do nothing
  }

  @Override
  public void onActivityResume(Activity activity) {
    // do nothing
  }

  @Override
  public void onActivityStop(Activity activity) {
    // do nothing
  }

  @Override
  public void identify(Identify identify) {
    // do nothing
  }

  @Override
  public void group(Group group) {
    // do nothing
  }

  @Override
  public void track(Track track) {
    // do nothing
  }

  @Override
  public void screen(Screen screen) {
    // do nothing
  }

  @Override
  public void alias(Alias alias) {
    // do nothing
  }

  @Override
  public void toggleOptOut(boolean optedOut) {
    // do nothing
  }

  @Override
  public void reset() {
    // do nothing
  }

  @Override
  public void flush() {
    // do nothing
  }
}
