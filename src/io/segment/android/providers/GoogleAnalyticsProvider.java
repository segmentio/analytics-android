package io.segment.android.providers;

import io.segment.android.errors.InvalidSettingsException;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Track;
import io.segment.android.provider.SimpleProvider;

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

public class GoogleAnalyticsProvider extends SimpleProvider {

	private static class SettingKey { 
		
		private static final String TRACKING_ID = "mobileTrackingId";
		
		private static final String DISPATCH_PERIOD = "dispatchPeriod";
		private static final String SAMPLING_FREQUENCY = "sampleFrequency";
		private static final String ANONYMIZE_IP_TRACKING = "anonymizeIp";
		private static final String REPORT_UNCAUGHT_EXCEPTIONS = "reportUncaughtExceptions";
		private static final String USE_HTTPS = "useHttps";
	}

	private Tracker tracker;
	
	@Override
	public String getKey() {
		return "Google Analytics";
	}
	
	@Override
	public void validate(EasyJSONObject settings) throws InvalidSettingsException {
		
		if (TextUtils.isEmpty(settings.getString(SettingKey.TRACKING_ID))) {
			throw new InvalidSettingsException(SettingKey.TRACKING_ID, "Google Analytics requires the trackingId (UA-XXXXXXXX-XX) setting.");
		}
	}
	
	private void initialize(Context context) {
		
		EasyJSONObject settings = this.getSettings();
		
		// docs: https://developers.google.com/analytics/devguides/collection/android/v2/parameters
		
		// The Google Analytics tracking ID to which to send your data. Dashes in the ID must be unencoded. 
		// You can disable your tracking by not providing this value.
		String trackingId = settings.getString(SettingKey.TRACKING_ID);
		// The dispatch period in seconds. Defaults to 30 minutes.
		int dispatchPeriod = settings.getInt(SettingKey.DISPATCH_PERIOD, 30);
		// The sample rate to use. Default is 100.0. It can be any value between 0.0 and 100.0
		Double sampleFrequency = settings.getDouble(SettingKey.SAMPLING_FREQUENCY, Double.valueOf(100));
		// Tells Google Analytics to anonymize the information sent by the tracker objects by 
		// removing the last octet of the IP address prior to its storage. Note that this will slightly 
		// reduce the accuracy of geographic reporting. false by default.
		boolean anonymizeIp = settings.getBoolean(SettingKey.ANONYMIZE_IP_TRACKING, false);
		// Automatically track an Exception each time an uncaught exception is thrown 
		// in your application. false by default.
		boolean reportUncaughtExceptions = settings.getBoolean(SettingKey.REPORT_UNCAUGHT_EXCEPTIONS, false);
		// Log to the server using https
		boolean useHttps = settings.getBoolean(SettingKey.USE_HTTPS, false);
		
		GoogleAnalytics gaInstance = GoogleAnalytics.getInstance(context);
		
		GAServiceManager.getInstance().setDispatchPeriod(dispatchPeriod);
		
		gaInstance.setDebug(true);
		
		tracker = gaInstance.getTracker(trackingId);
		tracker.setSampleRate(sampleFrequency);
		tracker.setAnonymizeIp(anonymizeIp);
		tracker.setUseSecure(useHttps);
		
		if (reportUncaughtExceptions) enableAutomaticExceptionTracking(tracker, context);
		
		gaInstance.setDefaultTracker(tracker);
		
		ready();
	}
	
	private void enableAutomaticExceptionTracking(Tracker tracker, Context context) {
		UncaughtExceptionHandler myHandler = new ExceptionReporter(
			    tracker,
			    GAServiceManager.getInstance(),
			    Thread.getDefaultUncaughtExceptionHandler(), context);

		Thread.setDefaultUncaughtExceptionHandler(myHandler);
	}
	
	@Override
	public void onCreate(Context context) {
		initialize(context);
	}
	
	@Override
	public void onActivityStart(Activity activity) {
		EasyTracker.getInstance().activityStart(activity);
	}
	
	@Override
	public void onActivityStop(Activity activity) {
		EasyTracker.getInstance().activityStop(activity);
	}
	
	@Override
	public void track(Track track) {
		EventProperties properties = track.getProperties();
		
		String category = properties.getString("category", "All");
		String action = track.getEvent();
		String label = properties.getString("label", null);
		int value = properties.getInt("value", 0);
		
		tracker.sendEvent(category, action, label, (long)value);
	}
	
	@Override
	public void flush() {
		GAServiceManager.getInstance().dispatch();
	}

}
