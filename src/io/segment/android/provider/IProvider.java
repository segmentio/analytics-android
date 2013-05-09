package io.segment.android.provider;

import io.segment.android.models.Alias;
import io.segment.android.models.Identify;
import io.segment.android.models.Track;
import android.app.Activity;
import android.content.Context;

public interface IProvider {
		
	/**
	 * Called by the Android system when the activity is created. 
	 * @param context Android application context
	 */
	public void onCreate(Context context);
	
	/**
	 * Called by the Android system when the activity starts. 
	 * @param context Android application context
	 */
	public void onActivityStart(Activity activity);

	/**
	 * Called when the Android system tells the Activity to stop
	 * @param context The Android application context
	 */
	public void onActivityStop(Activity activity);
	
	/**
	 * Called when the user identifies a user.
	 * @param identify An identify action
	 */
	public void identify(Identify identify);
	
	/**
	 * Called when the user tracks an action.
	 * @param A track action
	 */
	public void track(Track track);
	
	/**
	 * Called when a user aliases an action.
	 * @param alias An alias action
	 */
	public void alias(Alias alias);
	
	
	/**
	 * If possible, will flush all the messages from this provider 
	 * to their respective server endpoints.
	 */
	public void flush();
}
