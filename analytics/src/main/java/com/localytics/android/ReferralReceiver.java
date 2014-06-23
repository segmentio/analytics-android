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

// @formatter:off
// @formatter:on

package com.localytics.android;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.localytics.android.LocalyticsProvider.InfoDbColumns;

/**
 * Implements a BroadcastReceiver for Google Play Campaign Tracking. The following must be included
 * in your AndroidManifest.xml:
 *
 * <receiver android:name="com.localytics.android.ReferralReceiver" android:exported="true">
 * <intent-filter>
 * <action android:name="com.android.vending.INSTALL_REFERRER" />
 * </intent-filter>
 * </receiver>
 */
public class ReferralReceiver extends BroadcastReceiver {
  protected String appKey = null;

  @Override
  public void onReceive(Context context, Intent intent) {
    // Workaround for Android security issue: http://code.google.com/p/android/issues/detail?id=16006
    try {
      final Bundle extras = intent.getExtras();
      if (extras != null) {
        extras.containsKey(null);
      }
    } catch (final Exception e) {
      return;
    }

    // Return if this is not the right intent
    if (!intent.getAction().equals("com.android.vending.INSTALL_REFERRER")) { //$NON-NLS-1$
      return;
    }

    // Try to get the app key from the manifest
    if (appKey == null || appKey.length() == 0) {
      appKey = DatapointHelper.getLocalyticsAppKeyOrNull(context);
    }

    // Return if there's still no app key found
    if (appKey == null || appKey.length() == 0) {
      return;
    }

    // Get the referrer from the intent
    final String referrer = intent.getStringExtra("referrer"); //$NON-NLS-1$
    if (referrer == null || referrer.length() == 0) {
      return;
    }

    // Store referrer
    final LocalyticsProvider provider = LocalyticsProvider.getInstance(context, appKey);
    provider.runBatchTransaction(new Runnable() {
      public void run() {
        final ContentValues values = new ContentValues();
        values.put(InfoDbColumns.PLAY_ATTRIBUTION, referrer);
        provider.update(InfoDbColumns.TABLE_NAME, values, null, null);
      }
    });
  }
}