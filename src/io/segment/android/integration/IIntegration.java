package io.segment.android.integration;

import io.segment.android.models.Alias;
import io.segment.android.models.Group;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import android.app.Activity;
import android.content.Context;

public interface IIntegration {
		
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
	 * Called by the Android system when the activity resumes. 
	 * @param context Android application context
	 */
	public void onActivityResume(Activity activity);

	/**
	 * Called by the Android system when the activity pauses. 
	 * @param context Android application context
	 */
	public void onActivityPause(Activity activity);
	
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
	 * Called when the user identifies a group.
	 * @param group A group action
	 */
	public void group(Group group);
	
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
	 * Called when a user opens up a new screen
	 * @param screen Screen action
	 */
	public void screen(Screen screen);

	/**
	 * Resets the identified user in the library. Can be used
	 * when the user logs out.
	 */
	public void reset();
	
	/**
	 * Opt out of analytics.
	 * @param optedOut TODO
	 */
	public void toggleOptOut(boolean optedOut);
	
	/**
	 * If possible, will flush all the messages from this provider 
	 * to their respective server endpoints.
	 */
	public void flush();

}
