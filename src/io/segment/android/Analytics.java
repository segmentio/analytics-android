package io.segment.android;

import io.segment.android.cache.ISettingsLayer;
import io.segment.android.cache.SessionIdCache;
import io.segment.android.cache.SettingsCache;
import io.segment.android.cache.SettingsThread;
import io.segment.android.cache.SimpleStringCache;
import io.segment.android.db.IPayloadDatabaseLayer;
import io.segment.android.db.IPayloadDatabaseLayer.EnqueueCallback;
import io.segment.android.db.PayloadDatabase;
import io.segment.android.db.PayloadDatabaseThread;
import io.segment.android.flush.FlushThread;
import io.segment.android.flush.FlushThread.BatchFactory;
import io.segment.android.flush.IFlushLayer;
import io.segment.android.flush.IFlushLayer.FlushCallback;
import io.segment.android.info.InfoManager;
import io.segment.android.models.Alias;
import io.segment.android.models.BasePayload;
import io.segment.android.models.Batch;
import io.segment.android.models.Context;
import io.segment.android.models.EasyJSONObject;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Identify;
import io.segment.android.models.Screen;
import io.segment.android.models.Track;
import io.segment.android.models.Traits;
import io.segment.android.provider.Provider;
import io.segment.android.provider.ProviderManager;
import io.segment.android.request.BasicRequester;
import io.segment.android.request.IRequester;
import io.segment.android.stats.AnalyticsStatistics;
import io.segment.android.utils.HandlerTimer;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.app.Service;
import android.text.TextUtils;

public class Analytics {
	
	public static final String VERSION = "0.4.2";
	
	private static AnalyticsStatistics statistics;
	
	private static String secret;
	private static Options options;
	
	private static InfoManager infoManager;
	
	private static ProviderManager providerManager;
	private static HandlerTimer flushTimer;
	private static HandlerTimer refreshSettingsTimer;
	private static PayloadDatabase database;
	private static IPayloadDatabaseLayer databaseLayer;
	private static IFlushLayer flushLayer;
	private static ISettingsLayer settingsLayer;
	
	private static Context globalContext;
	
	private static boolean initialized;
	private static boolean optedOut;

	private static SimpleStringCache sessionIdCache;
	private static SimpleStringCache userIdCache;
	private static SettingsCache settingsCache;


	/**
	 * Initializes the Segment.io Android client, and tells the client that this 
	 * activity has been created. 
	 * 
	 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
	 * you to conveniently consume the API without making any HTTP requests
	 * yourself.
	 * 
	 * This client is also designed to be thread-safe and to not block each of
	 * your calls to make a HTTP request. It uses batching to efficiently send
	 * your requests on a separate resource-constrained thread pool.
	 * 
	 * @param activity
	 *            Your Android Activity
	 * 
	 */
	public static void onCreate (android.content.Context context) {
		Analytics.initialize(context);
		providerManager.onCreate(context);
	}
	
	/**
	 * Initializes the Segment.io Android client, and tells the client that this 
	 * activity has been created. 
	 * 
	 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
	 * you to conveniently consume the API without making any HTTP requests
	 * yourself.
	 * 
	 * This client is also designed to be thread-safe and to not block each of
	 * your calls to make a HTTP request. It uses batching to efficiently send
	 * your requests on a separate resource-constrained thread pool.
	 * 
	 * @param activity
	 *            Your Android Activity
	 * 
	 * @param secret
	 *            Your segment.io secret. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * 
	 */
	public static void onCreate (android.content.Context context, String secret) {
		Analytics.initialize(context, secret);
		providerManager.onCreate(context);
	}
	
	/**
	 * Initializes the Segment.io Android client, and tells the client that this 
	 * activity has been created. 
	 * 
	 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
	 * you to conveniently consume the API without making any HTTP requests
	 * yourself.
	 * 
	 * This client is also designed to be thread-safe and to not block each of
	 * your calls to make a HTTP request. It uses batching to efficiently send
	 * your requests on a separate resource-constrained thread pool.
	 * 
	 * @param activity
	 *            Your Android Activity
	 * 
	 * @param secret
	 *            Your segment.io secret. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * @param options
	 *            Options to configure the behavior of the Segment.io client
	 * 
	 * 
	 */
	public static void onCreate (android.content.Context context, String secret, Options options) {
		Analytics.initialize(context, secret, options);
		providerManager.onCreate(context);
	}
	
	/**
	 * Initializes the Segment.io Android client, and tells the client that this 
	 * activity has been started. 
	 * 
	 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
	 * you to conveniently consume the API without making any HTTP requests
	 * yourself.
	 * 
	 * This client is also designed to be thread-safe and to not block each of
	 * your calls to make a HTTP request. It uses batching to efficiently send
	 * your requests on a separate resource-constrained thread pool.
	 * 
	 * @param activity
	 *            Your Android Activity
	 * 
	 */
	public static void activityStart (Activity activity) {
		Analytics.initialize(activity);
		providerManager.onActivityStart(activity);
	}
	
	/**
	 * Initializes the Segment.io Android client, and tells the client that this 
	 * activity has been started. 
	 * 
	 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
	 * you to conveniently consume the API without making any HTTP requests
	 * yourself.
	 * 
	 * This client is also designed to be thread-safe and to not block each of
	 * your calls to make a HTTP request. It uses batching to efficiently send
	 * your requests on a separate resource-constrained thread pool.
	 * 
	 * @param activity
	 *            Your Android Activity
	 * 
	 * @param secret
	 *            Your segment.io secret. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * 
	 */
	public static void activityStart (Activity activity, String secret) {
		Analytics.initialize(activity, secret);
		providerManager.onActivityStart(activity);
	}
	
	/**
	 * Initializes the Segment.io Android client, and tells the client that this 
	 * activity has been started. 
	 * 
	 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
	 * you to conveniently consume the API without making any HTTP requests
	 * yourself.
	 * 
	 * This client is also designed to be thread-safe and to not block each of
	 * your calls to make a HTTP request. It uses batching to efficiently send
	 * your requests on a separate resource-constrained thread pool.
	 * 
	 * @param activity
	 *            Your Android Activity
	 * 
	 * @param secret
	 *            Your segment.io secret. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * @param options
	 *            Options to configure the behavior of the Segment.io client
	 * 
	 * 
	 */
	public static void activityStart (Activity activity, String secret, Options options) {
		Analytics.initialize(activity, secret, options);
		if (optedOut) return;
		providerManager.onActivityStart(activity);
	}
	
	/**
	 * Called when the activity has been stopped
	 * @param activity
	 *            Your Android Activity
	 */
	public static void activityStop (Activity activity) {
		Analytics.initialize(activity);
		if (optedOut) return;
		providerManager.onActivityStop(activity);
	}
	

	/**
	 * Initializes the Segment.io Android client.
	 * 
	 * You don't need to call this if you've called {@link Analytics}.activityStart from
	 * your Android activity.
	 * 
	 * You can use this method from a non-activity, such as a {@link Service}.
	 * 
	 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
	 * you to conveniently consume the API without making any HTTP requests
	 * yourself.
	 * 
	 * This client is also designed to be thread-safe and to not block each of
	 * your calls to make a HTTP request. It uses batching to efficiently send
	 * your requests on a separate resource-constrained thread pool.
	 * 
	 * @param activity
	 *            Your Android Activity
	 *
	 * 
	 */
	public static void initialize(android.content.Context context) {

		if (initialized) return;

		// read both secret and options from analytics.xml
		String secret = Configuration.getSecret(context);
		Options options = Configuration.getOptions(context);
		
		initialize(context, secret,options);
	}

	
	/**
	 * Initializes the Segment.io Android client.
	 * 
	 * You don't need to call this if you've called {@link Analytics}.activityStart from
	 * your Android activity.
	 * 
	 * You can use this method from a non-activity, such as a {@link Service}.
	 * 
	 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
	 * you to conveniently consume the API without making any HTTP requests
	 * yourself.
	 * 
	 * This client is also designed to be thread-safe and to not block each of
	 * your calls to make a HTTP request. It uses batching to efficiently send
	 * your requests on a separate resource-constrained thread pool.
	 * 
	 * @param context
	 *            Your Android android.content.Content (like your activity).
	 * 
	 * @param secret
	 *            Your segment.io secret. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 */
	public static void initialize(android.content.Context context, String secret) {

		if (initialized) return;

		// read options from analytics.xml
		Options options = Configuration.getOptions(context);
		
		initialize(context, secret, options);
	}

	/**
	 * Initializes the Segment.io Android client.
	 * 
	 * You don't need to call this if you've called {@link Analytics}.activityStart from
	 * your Android activity.
	 * 
	 * You can use this method from a non-activity, such as a {@link Service}.
	 * 
	 * The client is an HTTP wrapper over the Segment.io REST API. It will allow
	 * you to conveniently consume the API without making any HTTP requests
	 * yourself.
	 * 
	 * This client is also designed to be thread-safe and to not block each of
	 * your calls to make a HTTP request. It uses batching to efficiently send
	 * your requests on a separate resource-constrained thread pool.
	 * 
	 * @param context
	 *            Your Android android.content.Content (like your activity).
	 * 
	 * @param secret
	 *            Your segment.io secret. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * @param options
	 *            Options to configure the behavior of the Segment.io client
	 * 
	 * 
	 */
	public static void initialize(android.content.Context context, String secret, Options options) {
		
		String errorPrefix = "analytics-android client must be initialized with a valid ";

		if (context == null)
			throw new IllegalArgumentException(errorPrefix + "android context."); 
		
		if (secret == null || secret.length() == 0)
			throw new IllegalArgumentException(errorPrefix + "secret.");

		if (options == null)
			throw new IllegalArgumentException(errorPrefix + "options.");

		if (initialized) return;
		
		Analytics.statistics = new AnalyticsStatistics();
		
		Analytics.secret = secret;
		Analytics.options = options;
		
		// set logging based on the debug mode
		Logger.setLog(options.isDebug());

		// create the database using the activity context
		database = PayloadDatabase.getInstance(context);

		// knows how to create global context about this android device
		infoManager = new InfoManager();
		
		sessionIdCache = new SessionIdCache(context);
		userIdCache = new SimpleStringCache(context, Constants.SharedPreferences.USER_ID_KEY);
		
		// add a global context
		globalContext = new Context(infoManager.build(context));
		
		IRequester requester = new BasicRequester();
		
		// now we need to create our singleton thread-safe database thread
		Analytics.databaseLayer = new PayloadDatabaseThread(database);
		Analytics.databaseLayer.start();
		
		// start the flush thread
		Analytics.flushLayer = new FlushThread(requester, 
											  batchFactory,
											  Analytics.databaseLayer);
		
		Analytics.flushTimer = new HandlerTimer(
				options.getFlushAfter(), flushClock);
		
		Analytics.refreshSettingsTimer = new HandlerTimer(
				options.getSettingsCacheExpiry() + 1000, refreshSettingsClock);
		
		Analytics.settingsLayer = new SettingsThread(requester);
		
		settingsCache = new SettingsCache(context, settingsLayer, options.getSettingsCacheExpiry());
		
		providerManager = new ProviderManager(settingsCache);
	
		
		// important: disable Segment.io server-side processing of
		// the bundled providers that we'll evaluate on the mobile
		// device
		EasyJSONObject providerContext = new EasyJSONObject();
		for (Provider provider : providerManager.getProviders()) {
			providerContext.put(provider.getKey(), false);
		}
		globalContext.put("providers", providerContext);
		
		initialized = true;
		
		// start the other threads
		Analytics.flushTimer.start();
		Analytics.refreshSettingsTimer.start();
		Analytics.flushLayer.start();
		Analytics.settingsLayer.start();
		
		// tell the server to look for settings right now
		Analytics.refreshSettingsTimer.scheduleNow();
	}
	
	/**
	 * Factory that creates batches from payloads.
	 * 
	 * Inserts system information into global batches
	 */
	private static BatchFactory batchFactory = new BatchFactory() {
		
		@Override
		public Batch create(List<BasePayload> payloads) {
			
			Batch batch = new Batch(secret, payloads); 
					
			// add global batch settings from system information
			batch.setContext(globalContext);
			
			return batch;
		}
	};
	
	/**
	 * Flushes on a clock timer
	 */
	private static Runnable flushClock = new Runnable() {
		@Override
		public void run() {
			Analytics.flush(true);
		}
	};
	
	/**
	 * Refreshes the Segment.io integration settings from the server
	 */
	private static Runnable refreshSettingsClock = new Runnable() {
		@Override
		public void run() {
			providerManager.refresh();
		}
	};

	
	//
	// API Calls
	//

	//
	// Identify
	//



	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * The library will cache the userId, so that future calls to identify(...) and
	 * track(..) don't need to repeat the userId. 
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
	 * 
	 */
	public static void identify(String userId) {

		identify(userId, null, null, null);
	}
	
	
	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param traits
	 *            a dictionary with keys like email, name, subscriptionPlan or
	 *            age. You only need to record a trait once, no need to send it
	 *            again.
	 */
	public static void identify(Traits traits) {

		identify(null, traits, null, null);
	}
	
	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
	 * 
	 * @param traits
	 *            a dictionary with keys like email, name, subscriptionPlan or
	 *            age. You only need to record a trait once, no need to send it
	 *            again.
	 */
	public static void identify(String userId, Traits traits) {

		identify(userId, traits, null, null);
	}


	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param traits
	 *            a dictionary with keys like subscriptionPlan or age. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 * @param context
	 *            an object that describes anything that doesn't fit into this
	 *            event's properties (such as the user's IP)
	 * 
	 */
	public static void identify(Traits traits, Context context) {

		identify(null, traits, null, context);
	}
	
	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
	 * 
	 * @param traits
	 *            a dictionary with keys like subscriptionPlan or age. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 * @param context
	 *            an object that describes anything that doesn't fit into this
	 *            event's properties (such as the user's IP)
	 * 
	 */
	public static void identify(String userId, Traits traits, Context context) {

		identify(userId, traits, null, context);
	}


	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param traits
	 *            a dictionary with keys like subscriptionPlan or age. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 * @param timestamp
	 *            a {@link Calendar} representing when the identify took place.
	 *            If the identify just happened, leave it blank and we'll use
	 *            the server's time. If you are importing data from the past,
	 *            make sure you provide this argument.
	 * 
	 */
	public static void identify(Traits traits, Calendar timestamp) {

		identify(null, traits, timestamp, null);
	}

	
	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
	 * 
	 * @param traits
	 *            a dictionary with keys like subscriptionPlan or age. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 * @param timestamp
	 *            a {@link Calendar} representing when the identify took place.
	 *            If the identify just happened, leave it blank and we'll use
	 *            the server's time. If you are importing data from the past,
	 *            make sure you provide this argument.
	 * 
	 */
	public static void identify(String userId, Traits traits, Calendar timestamp) {

		identify(userId, traits, timestamp, null);
	}

	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param traits
	 *            a dictionary with keys like subscriptionPlan or age. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 * @param timestamp
	 *            a {@link Calendar} representing when the identify took place.
	 *            If the identify just happened, leave it blank and we'll use
	 *            the server's time. If you are importing data from the past,
	 *            make sure you provide this argument.
	 * 
	 * @param context
	 *            an object that describes anything that doesn't fit into this
	 *            event's properties (such as the user's IP)
	 */
	public static void identify(Traits traits, Calendar timestamp,
			Context context) {
		
		identify(null, traits, timestamp, context);
	}
	
	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
	 * 
	 * @param traits
	 *            a dictionary with keys like subscriptionPlan or age. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 * @param timestamp
	 *            a {@link Calendar} representing when the identify took place.
	 *            If the identify just happened, leave it blank and we'll use
	 *            the server's time. If you are importing data from the past,
	 *            make sure you provide this argument.
	 * 
	 * @param context
	 *            an object that describes anything that doesn't fit into this
	 *            event's properties (such as the user's IP)
	 */
	public static void identify(String userId, Traits traits, Calendar timestamp,
			Context context) {
		
		checkInitialized();
		if (optedOut) return;

		userId = getOrSetUserId(userId);
		
		if (userId == null || userId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #identify must be initialized with a valid user id.");
		}
		
		if (context == null)
			context = new Context();
		if (traits == null)
			traits = new Traits();

		Identify identify = new Identify(userId, traits, timestamp, context);

		enqueue(identify);
		
		providerManager.identify(identify);
		
		statistics.updateIdentifies(1);
	}
	
	//
	// Track
	//

	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * Track will use an automatically generated userId unless one has been
	 * provided by identify(..).
	 * 
	 * @param event
	 *            describes what this user just did. It's a human readable
	 *            description like "Played a Song", "Printed a Report" or
	 *            "Updated Status".
	 * 
	 */
	public static void track(String event) {

		track(event, null, null, null);
	}
	

	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * Track will use an automatically generated userId unless one has been
	 * provided by identify(..).
	 * 
	 * @param event
	 *            describes what this user just did. It's a human readable
	 *            description like "Played a Song", "Printed a Report" or
	 *            "Updated Status".
	 * 
	 * @param properties
	 *            a dictionary with items that describe the event in more
	 *            detail. This argument is optional, but highly
	 *            recommended—you’ll find these properties extremely useful
	 *            later.
	 */
	public static void track(String event, EventProperties properties) {

		track(event, properties, null, null);
	}
	

	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * Track will use an automatically generated userId unless one has been
	 * provided by identify(..).
	 * 
	 * @param event
	 *            describes what this user just did. It's a human readable
	 *            description like "Played a Song", "Printed a Report" or
	 *            "Updated Status".
	 * 
	 * @param properties
	 *            a dictionary with items that describe the event in more
	 *            detail. This argument is optional, but highly
	 *            recommended—you’ll find these properties extremely useful
	 *            later.
	 * 
	 * @param timestamp
	 *            a {@link DateTime} object representing when the track took
	 *            place. If the event just happened, leave it blank and we'll
	 *            use the server's time. If you are importing data from the
	 *            past, make sure you provide this argument.
	 * 
	 */
	public static void track(String event, EventProperties properties,
			Calendar timestamp) {

		track(event, properties, timestamp, null);
	}


	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * Track will use an automatically generated userId unless one has been
	 * provided by identify(..).
	 * 
	 * @param event
	 *            describes what this user just did. It's a human readable
	 *            description like "Played a Song", "Printed a Report" or
	 *            "Updated Status".
	 * 
	 * @param properties
	 *            a dictionary with items that describe the event in more
	 *            detail. This argument is optional, but highly
	 *            recommended—you’ll find these properties extremely useful
	 *            later.
	 * 
	 * @param context
	 *            an object that describes anything that doesn't fit into this
	 *            event's properties (such as the user's IP)
	 * 
	 */
	public static void track(String event, EventProperties properties,
			 Context context) {

		track(event, properties, null, context);
	}
	
	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * Track will use an automatically generated userId unless one has been
	 * provided by identify(..).
	 * 
	 * @param event
	 *            describes what this user just did. It's a human readable
	 *            description like "Played a Song", "Printed a Report" or
	 *            "Updated Status".
	 * 
	 * @param properties
	 *            a dictionary with items that describe the event in more
	 *            detail. This argument is optional, but highly
	 *            recommended—you’ll find these properties extremely useful
	 *            later.
	 * 
	 * @param timestamp
	 *            a {@link Calendar} object representing when the track took
	 *            place. If the event just happened, leave it blank and we'll
	 *            use the server's time. If you are importing data from the
	 *            past, make sure you provide this argument.
	 * 
	 * @param context
	 *            an object that describes anything that doesn't fit into this
	 *            event's properties (such as the user's IP)
	 * 
	 */
	public static void track(String event, EventProperties properties,
			Calendar timestamp, Context context) {
		
		checkInitialized();
		if (optedOut) return;
		
		// get the user ID from the cache
		String userId = getOrSetUserId(null);
		
		if (userId == null || userId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #track must be initialized with a valid user id.");
		}
		
		if (event == null || event.length() == 0) {
			throw new IllegalArgumentException("analytics-android #track must be initialized with a valid event name.");
		}
		
		if (context == null)
			context = new Context();
		if (properties == null)
			properties = new EventProperties();

		
		Track track = new Track(userId, event, properties, timestamp, context);

		enqueue(track);
		
		providerManager.track(track);
		
		statistics.updateTracks(1);
	}

	//
	// Screen
	//
	

	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login Page");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param screen
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 */
	public static void screen(String screen) {
		
		screen(screen, null, null, null);
	}
	
	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login Page");
	 *
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param screen
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * @param properties
	 *            a dictionary with items that describe the screen in more
	 *            detail. This argument is optional, but highly
	 *            recommended—you’ll find these properties extremely useful
	 *            later.
	 * 
	 */
	public static void screen(String screen, EventProperties properties) {
		
		screen(screen, properties, null, null);
	}
	
	
	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login Page");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param screen
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * @param properties
	 *            a dictionary with items that describe the screen in more
	 *            detail. This argument is optional, but highly
	 *            recommended—you’ll find these properties extremely useful
	 *            later.
	 * 
	 * @param timestamp
	 *            a {@link Calendar} object representing when the track took
	 *            place. If the event just happened, leave it blank and we'll
	 *            use the server's time. If you are importing data from the
	 *            past, make sure you provide this argument.
	 * 
	 */
	public static void screen(String screen, EventProperties properties,
							  Calendar timestamp) {
		
		screen(screen, properties, timestamp, null);
	}
	
	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login Page");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param screen
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * @param properties
	 *            a dictionary with items that describe the screen in more
	 *            detail. This argument is optional, but highly
	 *            recommended—you’ll find these properties extremely useful
	 *            later.
	 * 
	 * @param timestamp
	 *            a {@link Calendar} object representing when the track took
	 *            place. If the event just happened, leave it blank and we'll
	 *            use the server's time. If you are importing data from the
	 *            past, make sure you provide this argument.
	 * 
	 * @param context
	 *            an object that describes anything that doesn't fit into this
	 *            event's properties (such as the user's IP)
	 * 
	 */
	public static void screen(String screen, EventProperties properties,
							  Calendar timestamp, Context context) {
		
		
		checkInitialized();
		if (optedOut) return;

		String userId = getOrSetUserId(null);
		
		if (userId == null || userId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #track must be initialized with a valid user id.");
		}
		
		if (screen == null || screen.length() == 0) {
			throw new IllegalArgumentException("analytics-android #screen must be initialized with a valid screen name.");
		}
		
		if (context == null)
			context = new Context();
		if (properties == null)
			properties = new EventProperties();

		
		Screen screenAction = new Screen(userId, screen, properties, timestamp, context);
		
		// just call internally into the provider manager
		providerManager.screen(screenAction);
		
		statistics.updateScreens(1);
	}
	
	//
	// Alias
	//

	/**
	 * Aliases an anonymous user into an identified user.
	 * 
	 * @param from
	 *            the anonymous user's id before they are logged in.
	 * 
	 * @param to
	 *            the identified user's id after they're logged in.
	 *           
	 */
	public static void alias(String from, String to) {
		alias(from, to, null, null);
	}

	/**
	 * Aliases an anonymous user into an identified user.
	 * 
	 * @param from
	 *            the anonymous user's id before they are logged in.
	 * 
	 * @param to
	 *            the identified user's id after they're logged in.
	 * 
	 * 
	 * @param timestamp
	 *            a {@link Calendar} object representing when the track took
	 *            place. If the event just happened, leave it blank and we'll
	 *            use the server's time. If you are importing data from the
	 *            past, make sure you provide this argument.
	 * 
	 *           
	 */
	public static void alias(String from, String to, Calendar timestamp) {
		alias(from, to, timestamp, null);
	}

	/**
	 * Aliases an anonymous user into an identified user.
	 * 
	 * @param from
	 *            the anonymous user's id before they are logged in.
	 * 
	 * @param to
	 *            the identified user's id after they're logged in.
	 * 
	 * 
	 * @param context
	 *            an object that describes anything that doesn't fit into this
	 *            event's properties (such as the user's IP)
	 *           
	 */
	public static void alias(String from, String to, Context context) {
		alias(from, to, null, context);
	}
	

	/**
	 * Aliases an anonymous user into an identified user.
	 * 
	 * @param from
	 *            the anonymous user's id before they are logged in.
	 * 
	 * @param to
	 *            the identified user's id after they're logged in.
	 * 
	 * 
	 * @param timestamp
	 *            a {@link Calendar} object representing when the track took
	 *            place. If the event just happened, leave it blank and we'll
	 *            use the server's time. If you are importing data from the
	 *            past, make sure you provide this argument.
	 * 
	 * @param context
	 *            an object that describes anything that doesn't fit into this
	 *            event's properties (such as the user's IP)
	 *
	 *             
	 */
	public static void alias(String from, String to, Calendar timestamp, Context context) {
		
		checkInitialized();
		if (optedOut) return;
		
		if (from == null || from.length() == 0) {
			throw new IllegalArgumentException("analytics-android #alias must be initialized with a valid from id.");
		}
		
		if (to == null || to.length() == 0) {
			throw new IllegalArgumentException("analytics-android #alias must be initialized with a valid to id.");
		}
		
		if (context == null)
			context = new Context();
		
		Alias alias = new Alias(from, to, timestamp, context);
		
		enqueue(alias);
		
		providerManager.alias(alias);
		
		statistics.updateAlias(1);
	}

	
	//
	// Internal
	//

	/**
	 * Gets or sets the current userId. If the provided userId
	 * is null, then we'll send the sessionId. If the userId
	 * is not null, then it will be set in the userId cache and will
	 * be returned.
	 * @param userId
	 * @return
	 */
	private static String getOrSetUserId(String userId) {
		
		if (TextUtils.isEmpty(userId)) {
			// no user id provided, lets try to see if we have it saved
			userId = userIdCache.get();
			if (TextUtils.isEmpty(userId))
				// we have no user Id, let's use the sessionId
				userId = sessionIdCache.get();
		} else {
			// we were passed a user Id so let's save it
			userIdCache.set(userId);
		}
		
		return userId;
	}
	
	/**
	 * Enqueues an {@link Identify}, {@link Track}, {@link Alias},
	 * or any action of type {@link BasePayload}
	 * @param payload
	 */
	public static void enqueue(final BasePayload payload) {
		
		statistics.updateInsertAttempts(1);
		
		final long start = System.currentTimeMillis(); 
		
		databaseLayer.enqueue(payload, new EnqueueCallback() {
			
			@Override
			public void onEnqueue(boolean success, long rowCount) {
				
				long duration = System.currentTimeMillis() - start;
				statistics.updateInsertTime(duration);
				
				if (rowCount >= options.getFlushAt()) {
					Analytics.flush(true);
				}
			}
		});
	}
	
	private static void checkInitialized() {
		if (!initialized)
			throw new IllegalStateException("Please call Analytics.initialize before using the library.");
	}


	//
	// Opt out
	//
	
	/**
	 * Turns on opt out, opting out of any analytics sent from this point forward.
	 * 
	 * 
	 */
	public static void optOut() {
		optOut(true);
	}
	
	/**
	 * Toggle opt out
	 * 
	 * @param optOut
	 *            true to stop sending any more analytics.
	 */
	public static void optOut(boolean optOut) {
		boolean toggled = Analytics.optedOut != optOut;
		Analytics.optedOut = optOut;
		if (toggled) providerManager.toggleOptOut(optOut);
	}
	
	//
	// Actions
	//
	
	
	/**
	 * Blocks until the queue is flushed
	 */
	public static void flush(boolean async) {
		checkInitialized();
		
		statistics.updateFlushAttempts(1);
		
		final long start = System.currentTimeMillis(); 
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		flushLayer.flush(new FlushCallback() {

			@Override
			public void onFlushCompleted(boolean success) {
				latch.countDown();
				
				if (success) {
					long duration = System.currentTimeMillis() - start;
					statistics.updateFlushTime(duration);
				}
			}

		});

		// flush all the providers as well
		providerManager.flush();
		
		if (!async) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				Logger.e("Interrupted while waiting for a blocking flush.");
			}
		}
	}

	/**
	 * Resets the cached userId. Should be used when the user logs out.
	 */
	public static void reset() {
		if (initialized) {
			userIdCache.reset();
			
			// reset all the providers
			providerManager.reset();
		}
	}
	
	/**
	 * Triggers a download of Segment.io integration settings
	 * from the server, and update of all the bundled providers.
	 */
	public static void refreshSettings() {
		if (initialized) {
			providerManager.refresh();
		}
	}
	
	/**
	 * Stops the analytics client threads, and resets the client
	 */
	public static void close() {
		checkInitialized();
		// stops the looper on the timer, flush, and database thread
		flushTimer.quit();
		refreshSettingsTimer.quit();
		flushLayer.quit();
		databaseLayer.quit();
		settingsLayer.quit();

		// closes the database
		database.close();
		
		options = null;
		secret = null;
		
		initialized = false;
	}

	//
	// Getters and Setters
	//

	/**
	 * Gets the unique session ID generated for this user
	 * until a userId is provided.
	 * 
	 * Use this ID as the "from" ID to alias your new
	 * userId if the user was just created.
	 * 
	 * @return
	 */
	public static String getSessionId() {
		checkInitialized();
		return sessionIdCache.get();
	}
	
	/**
	 * Allows you to set your own sessionId. Used mostly for testing.
	 * @param sessionId
	 */
	public static void setSessionId(String sessionId) {
		checkInitialized();
		sessionIdCache.set(sessionId);
	}
	
	/**
	 * Gets the userId thats currently saved for this
	 * application. If none has been entered yet,
	 * this will return null.
	 * @return
	 */
	public static String getUserId() {
		 checkInitialized();
		return userIdCache.get();
	}
	
	/**
	 * Returns whether the client is initialized
	 * @return
	 */
	public static boolean isInitialized() {
		return initialized;
	}
	
	/**
	 * Gets the current Segment.io API secret
	 * @return
	 */
	public static String getSecret() {
		if (secret == null) checkInitialized();
		return secret;
	}

	public static void setSecret(String secret) {
		Analytics.secret = secret;
	}

	public static ProviderManager getProviderManager() {
		return providerManager;
	}
	
	/**
	 * Gets the Segment.io client options
	 * @return
	 */
	public static Options getOptions() {
		if (options == null) checkInitialized();
		return options;
	}

	/**
	 * Gets the client statistics
	 * @return
	 */
	public static AnalyticsStatistics getStatistics() {
		if (statistics == null) checkInitialized();
		return statistics;
	}
}
