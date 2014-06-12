package com.segment.android.integration;

import com.segment.android.models.Alias;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;

import android.app.Activity;

/**
 * A provider override that doesn't require that you implement every provider
 * method, except for the essential ones.
 *
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
