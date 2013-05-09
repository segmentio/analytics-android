package io.segment.android.provider;

import io.segment.android.models.Alias;
import io.segment.android.models.Identify;
import io.segment.android.models.Track;
import android.app.Activity;

/**
 * A provider override that doesn't require that you implement every provider
 * method, except for the essential ones.
 *
 */
public abstract class SimpleProvider extends Provider {
	
	@Override
	public void onActivityStart(Activity activity) {
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
	public void track(Track track) {
		// do nothing
	}

	@Override
	public void alias(Alias alias) {
		// do nothing
	}


	@Override
	public void flush() {
		// do nothing	
	}
	
}
