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
import android.content.Context;
import com.segment.android.models.Alias;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;

public interface IIntegration {

  /**
   * Called by the Android system when the activity is created.
   *
   * @param context Android application context
   */
  void onCreate(Context context);

  /**
   * Called by the Android system when the activity starts.
   *
   * @param activity Android activity context
   */
  void onActivityStart(Activity activity);

  /**
   * Called by the Android system when the activity resumes.
   *
   * @param activity Android activity context
   */
  void onActivityResume(Activity activity);

  /**
   * Called by the Android system when the activity pauses.
   *
   * @param activity Android activity context
   */
  void onActivityPause(Activity activity);

  /**
   * Called when the Android system tells the Activity to stop
   *
   * @param activity Android activity context
   */
  void onActivityStop(Activity activity);

  /**
   * Called when the user identifies a user.
   *
   * @param identify An identify action
   */
  void identify(Identify identify);

  /**
   * Called when the user identifies a group.
   *
   * @param group A group action
   */
  void group(Group group);

  /**
   * Called when the user tracks an action.
   *
   * @param track : A track action
   */
  void track(Track track);

  /**
   * Called when a user aliases an action.
   *
   * @param alias An alias action
   */
  void alias(Alias alias);

  /**
   * Called when a user opens up a new screen
   *
   * @param screen Screen action
   */
  void screen(Screen screen);

  /**
   * Resets the identified user in the library. Can be used
   * when the user logs out.
   */
  void reset();

  /**
   * Opt out of analytics.
   *
   * @param optedOut TODO
   */
  void toggleOptOut(boolean optedOut);

  /**
   * If possible, will flush all the messages from this provider
   * to their respective server endpoints.
   */
  void flush();
}
