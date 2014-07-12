//@formatter:off
/**
 * PushReceiver.java Copyright (C) 2013 Char Software Inc., DBA Localytics. This code is provided under the Localytics Modified
 * BSD License. A copy of this license has been distributed in a file called LICENSE with this source code. Please visit
 * www.localytics.com for more information.
 */
//@formatter:on

package com.localytics.android;

import org.json.JSONException;
import org.json.JSONObject;

import com.localytics.android.LocalyticsProvider.InfoDbColumns;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.Log;

public class PushReceiver extends BroadcastReceiver 
{
	public void onReceive(Context context, Intent intent) 
	{
		// Registration complete
		if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) 
		{
			handleRegistration(context, intent);
		} 
		// Notification received
		else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) 
		{
			handleNotificationReceived(context, intent);
		}
	}

	private void handleRegistration(Context context, Intent intent) 
	{
		String registrationId = intent.getStringExtra("registration_id");
		
		// Failed?
		if (intent.getStringExtra("error") != null) 
		{
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, "GCM registration failed"); //$NON-NLS-1$
            }
	    } 
		// Unregistered?
	    else if (intent.getStringExtra("unregistered") != null) 
	    {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, "GCM unregistered: removing id"); //$NON-NLS-1$
            }
            
	    	setRegistrationId(context, null);
	    } 
		// Success
	    else if (registrationId != null) 
	    {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, String.format("GCM registered, new id: %s", registrationId)); //$NON-NLS-1$
            }	    	
            
	    	setRegistrationId(context, registrationId);
	    }
	}
	
	private void handleNotificationReceived(Context context, Intent intent) 
	{
		// Ignore messages that aren't from Localytics
		String llString = intent.getExtras().getString("ll");
		if (TextUtils.isEmpty(llString)) return;
		    	
		// Try to parse the campaign id from the payload
		int campaignId = 0;
		
		try 
		{
			JSONObject llObject = new JSONObject(llString);
			campaignId = llObject.getInt("ca");
		}
		catch (JSONException e)
		{
			if (Constants.IS_LOGGABLE)
			{
				Log.w(Constants.LOG_TAG, "Failed to get campaign id from payload, ignoring message"); //$NON-NLS-1$
			}    		
			
			return;
		}
    	
		// Get the notification message
		String message = intent.getExtras().getString("message");
		if (TextUtils.isEmpty(message)) return;
		
		// Get the app name, icon, and launch intent
		CharSequence appName = "";
		int appIcon = android.R.drawable.sym_def_app_icon;
		Intent launchIntent = null;
		try 
		{
			ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
			appIcon = applicationInfo.icon;
			appName = context.getPackageManager().getApplicationLabel(applicationInfo);
			launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
	    } 
		catch (NameNotFoundException e) 
		{
            if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "Failed to get application name, icon, or launch intent"); //$NON-NLS-1$
            }	    	
	    }
						
		// Create the notification
		Notification notification = new Notification(appIcon, message, System.currentTimeMillis());
		
		// Set the intent to perform when tapped
		if (launchIntent != null)
		{
			launchIntent.putExtras(intent);
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setLatestEventInfo(context, appName, message, contentIntent);
		}
		
		// Auto dismiss when tapped
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        
        // Show the notification (use the campaign id as the notification id to prevents duplicates)
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(campaignId, notification);
	}
	
	private void setRegistrationId(final Context context, final String registrationId)
	{
		// Get the app key
    	String appKey = DatapointHelper.getLocalyticsAppKeyOrNull(context);
		
		// Return if there's no app key in the manifest
		if (appKey == null || appKey.length() == 0) {
			return;
		}
    	 
		// Persist the registration id and current app version
        final LocalyticsProvider provider = LocalyticsProvider.getInstance(context, appKey);
        provider.runBatchTransaction(new Runnable()
        {
        	public void run()
        	{
                final ContentValues values = new ContentValues();
                values.put(InfoDbColumns.REGISTRATION_ID, registrationId == null ? "" : registrationId);
                values.put(InfoDbColumns.REGISTRATION_VERSION, DatapointHelper.getAppVersion(context));
                provider.update(InfoDbColumns.TABLE_NAME, values, null, null);
        	}
    	});
	}
}
