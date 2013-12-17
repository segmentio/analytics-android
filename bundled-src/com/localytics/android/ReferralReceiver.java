// @formatter:off
/*
 * ReferralReceiver.java Copyright (C) 2013 Char Software Inc., DBA Localytics. This code is provided under the Localytics
 * Modified BSD License. A copy of this license has been distributed in a file called LICENSE with this source code. Please visit
 * www.localytics.com for more information.
 */
// @formatter:on

package com.localytics.android;

import com.localytics.android.LocalyticsProvider.InfoDbColumns;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Implements a BroadcastReceiver for Google Play Campaign Tracking. The following must be included in your AndroidManifest.xml:
 * 
 * <receiver android:name="com.localytics.android.ReferralReceiver" android:exported="true">
 *     <intent-filter>
 *         <action android:name="com.android.vending.INSTALL_REFERRER" />
 *     </intent-filter>
 * </receiver>
 *  
 */
public class ReferralReceiver extends BroadcastReceiver
{
	protected String appKey = null;
			
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Workaround for Android security issue: http://code.google.com/p/android/issues/detail?id=16006
        try
        {
            final Bundle extras = intent.getExtras();
            if (extras != null) {
                extras.containsKey(null);
            }
        }
        catch (final Exception e) {
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
        provider.runBatchTransaction(new Runnable()
        {
        	public void run()
        	{
                final ContentValues values = new ContentValues();
                values.put(InfoDbColumns.PLAY_ATTRIBUTION, referrer);        
                provider.update(InfoDbColumns.TABLE_NAME, values, null, null);        
        	}
    	});
    }
}