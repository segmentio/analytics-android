package com.segment.android;


import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.app.Service;
import android.text.TextUtils;

import com.segment.android.cache.AnonymousIdCache;
import com.segment.android.cache.ISettingsLayer;
import com.segment.android.cache.ISettingsLayer.SettingsCallback;
import com.segment.android.cache.SettingsCache;
import com.segment.android.cache.SettingsThread;
import com.segment.android.cache.SimpleStringCache;
import com.segment.android.db.IPayloadDatabaseLayer;
import com.segment.android.db.IPayloadDatabaseLayer.EnqueueCallback;
import com.segment.android.db.PayloadDatabase;
import com.segment.android.db.PayloadDatabaseThread;
import com.segment.android.flush.FlushThread;
import com.segment.android.flush.FlushThread.BatchFactory;
import com.segment.android.flush.IFlushLayer;
import com.segment.android.flush.IFlushLayer.FlushCallback;
import com.segment.android.info.InfoManager;
import com.segment.android.integration.Integration;
import com.segment.android.integration.IntegrationManager;
import com.segment.android.models.Alias;
import com.segment.android.models.BasePayload;
import com.segment.android.models.Batch;
import com.segment.android.models.Context;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Options;
import com.segment.android.models.Props;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.models.Traits;
import com.segment.android.request.BasicRequester;
import com.segment.android.request.IRequestLayer;
import com.segment.android.request.IRequester;
import com.segment.android.request.RequestThread;
import com.segment.android.stats.AnalyticsStatistics;
import com.segment.android.utils.DeviceId;
import com.segment.android.utils.HandlerTimer;

public class Analytics {
	
	public static final String VERSION = "0.6.13";
	
	private static AnalyticsStatistics statistics;
	
	private static Context globalContext;
	
	private static String writeKey;
	private static Config options;
	
	private static InfoManager infoManager;
	
	private static IntegrationManager integrationManager;
	private static HandlerTimer flushTimer;
	private static HandlerTimer refreshSettingsTimer;
	private static PayloadDatabase database;
	private static IPayloadDatabaseLayer databaseLayer;
	private static IRequestLayer requestLayer;
	private static IFlushLayer flushLayer;
	private static ISettingsLayer settingsLayer;
	
	private static volatile boolean initialized;
	private static volatile boolean optedOut;

	private static SimpleStringCache anonymousIdCache;
	private static SimpleStringCache userIdCache;
	private static SimpleStringCache groupIdCache;
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
		integrationManager.onCreate(context);
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
	 * @param writeKey
	 *            Your segment.io writeKey. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * 
	 */
	public static void onCreate (android.content.Context context, String writeKey) {
		Analytics.initialize(context, writeKey);
		integrationManager.onCreate(context);
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
	 * @param writeKey
	 *            Your segment.io writeKey. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * @param options
	 *            Options to configure the behavior of the Segment.io client
	 * 
	 * 
	 */
	public static void onCreate (android.content.Context context, String writeKey, Config options) {
		Analytics.initialize(context, writeKey, options);
		integrationManager.onCreate(context);
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
		integrationManager.onActivityStart(activity);
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
	 * @param writeKey
	 *            Your segment.io writeKey. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * 
	 */
	public static void activityStart (Activity activity, String writeKey) {
		Analytics.initialize(activity, writeKey);
		integrationManager.onActivityStart(activity);
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
	 * @param writeKey
	 *            Your segment.io writeKey. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * @param options
	 *            Options to configure the behavior of the Segment.io client
	 * 
	 * 
	 */
	public static void activityStart (Activity activity, String writeKey, Config options) {
		Analytics.initialize(activity, writeKey, options);
		if (optedOut) return;
		integrationManager.onActivityStart(activity);
	}

	/**
	 * Called when the activity has been resumed
	 * @param activity
	 *            Your Android Activity
	 */
	public static void activityResume (Activity activity) {
		Analytics.initialize(activity);
		if (optedOut) return;
		integrationManager.onActivityResume(activity);
	}
	
	/**
	 * Called when the activity has been stopped
	 * @param activity
	 *            Your Android Activity
	 */
	public static void activityStop (Activity activity) {
		Analytics.initialize(activity);
		if (optedOut) return;
		integrationManager.onActivityStop(activity);
	}

	/**
	 * Called when the activity has been paused
	 * @param activity
	 *            Your Android Activity
	 */
	public static void activityPause (Activity activity) {
		Analytics.initialize(activity);
		if (optedOut) return;
		integrationManager.onActivityPause(activity);
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

		// read both writeKey and options from analytics.xml
		String writeKey = ResourceConfig.getWriteKey(context);
		Config options = ResourceConfig.getOptions(context);
		
		initialize(context, writeKey, options);
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
	 * @param writeKey
	 *            Your segment.io writeKey. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 */
	public static void initialize(android.content.Context context, String writeKey) {

		if (initialized) return;

		// read options from analytics.xml
		Config options = ResourceConfig.getOptions(context);
		
		initialize(context, writeKey, options);
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
	 * @param writeKey
	 *            Your segment.io writeKey. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 * @param options
	 *            Options to configure the behavior of the Segment.io client
	 * 
	 * 
	 */
	public static void initialize(android.content.Context context, String writeKey, Config options) {
		
		String errorPrefix = "analytics-android client must be initialized with a valid ";

		if (context == null)
			throw new IllegalArgumentException(errorPrefix + "android context."); 
		
		if (writeKey == null || writeKey.length() == 0)
			throw new IllegalArgumentException(errorPrefix + "writeKey.");

		if (options == null)
			throw new IllegalArgumentException(errorPrefix + "options.");

		if (initialized) return;
		
		Analytics.statistics = new AnalyticsStatistics();
		
		Analytics.writeKey = writeKey;
		Analytics.options = options;
		
		// set logging based on the debug mode
		Logger.setLog(options.isDebug());

		// create the database using the activity context
		database = PayloadDatabase.getInstance(context);

		// knows how to create global context about this android device
		infoManager = new InfoManager(options);
		
		anonymousIdCache = new AnonymousIdCache(context);
		groupIdCache = new SimpleStringCache(context, Constants.SharedPreferences.GROUP_ID_KEY);
		userIdCache = new SimpleStringCache(context, Constants.SharedPreferences.USER_ID_KEY);
		
		// set the sessionId initially
		anonymousIdCache.set(DeviceId.get(context));
		
		// now we need to create our singleton thread-safe database thread
		Analytics.databaseLayer = new PayloadDatabaseThread(database);
		Analytics.databaseLayer.start();

		IRequester requester = new BasicRequester();
		
		// and a single request thread
		Analytics.requestLayer = new RequestThread(requester);
		Analytics.requestLayer.start();
		
		// start the flush thread
		Analytics.flushLayer = new FlushThread(Analytics.databaseLayer, 
											  batchFactory,
											  Analytics.requestLayer);
		
		Analytics.flushTimer = new HandlerTimer(
				options.getFlushAfter(), flushClock);
		
		Analytics.refreshSettingsTimer = new HandlerTimer(
				options.getSettingsCacheExpiry() + 1000, refreshSettingsClock);
		
		Analytics.settingsLayer = new SettingsThread(requester);
		
		settingsCache = new SettingsCache(context, settingsLayer, options.getSettingsCacheExpiry());
		
		integrationManager = new IntegrationManager(settingsCache);
		
		globalContext = generateContext(context);
		
		initialized = true;
		
		// start the other threads
		Analytics.flushTimer.start();
		Analytics.refreshSettingsTimer.start();
		Analytics.flushLayer.start();
		Analytics.settingsLayer.start();
		
		// tell the server to look for settings right now
		Analytics.refreshSettingsTimer.scheduleNow();
	}
	
	private static Context generateContext(android.content.Context context) {
		Context ctx = new Context(infoManager.build(context));

		// important: disable Segment.io server-side processing of
		// the bundled integrations that we'll evaluate on the mobile
		// device
		EasyJSONObject integrationContext = new EasyJSONObject();
		for (Integration integration : integrationManager.getIntegrations()) {
			integrationContext.put(integration.getKey(), false);
		}
		ctx.put("integrations", integrationContext);
		
		return ctx;
	}
	
	/**
	 * Factory that creates batches from action payloads.
	 * 
	 * Inserts system information into global batches
	 */
	private static BatchFactory batchFactory = new BatchFactory() {
		
		@Override
		public Batch create(List<BasePayload> payloads) {
			Batch batch = new Batch(writeKey, payloads);
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
			settingsCache.load(new SettingsCallback() {
				@Override
				public void onSettingsLoaded(boolean success, EasyJSONObject object) {
					integrationManager.refresh();	
				}
			});
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
		identify(userId, null, null);
	}
	

	/**
	 * Identifying a user ties all of their actions to an id, and associates
	 * user traits to that id.
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
	 *            
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 */
	public static void identify(String userId, Options options) {
		identify(userId, null, options);
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
		identify(null, traits, null);
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
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 */
	public static void identify(Traits traits, Options options) {
		identify(null, traits, options);
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
	 */
	public static void identify(String userId, Traits traits) {
		identify(userId, traits, null);
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
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 */
	public static void identify(String userId, Traits traits, Options options) {
		checkInitialized();
		if (optedOut) return;

		userId = getOrSetUserId(userId);
		
		if (userId == null || userId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #identify must be initialized with a valid user id.");
		}
		
		if (traits == null) traits = new Traits();
		if (options == null) options = new Options();

		Identify identify = new Identify(userId, traits, options);
		enqueue(identify);
		integrationManager.identify(identify);
		statistics.updateIdentifies(1);
	}
	
	//
	// Group
	//
	
	/**
	 * Identifying a group ties all of the group's actions to an id, and associates
	 * group traits to that id.
	 * 
	 * @param groupId
	 *            the group's id. It's the same id as
	 *            which you would recognize a user's company in your database.
	 * 
	 */
	public static void group(String groupId) {
		group(groupId, null, null);
	}
	
	/**
	 * Identifying a group ties all of the group's actions to an id, and associates
	 * group traits to that id.
	 * 
	 * @param groupId
	 *            the group's id. It's the same id as
	 *            which you would recognize a user's company in your database.
	 *
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations. 
	 */
	public static void group(String groupId, Options options) {
		group(groupId, null, options);
	}	

	
	/**
	 * Identifying a group ties all of the group's actions to an id, and associates
	 * group traits to that id.
	 * 
	 * @param traits
	 *            a dictionary with keys like plan or name. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 */
	public static void group(Traits traits) {
		group(null, traits, null);
	}
	
	/**
	 * Identifying a group ties all of the group's actions to an id, and associates
	 * group traits to that id.
	 * 
	 * @param traits
	 *            a dictionary with keys like plan or name. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations. 
	 */
	public static void group(Traits traits, Options options) {
		group(null, traits, options);
	}
	
	/**
	 * Identifying a group ties all of the group's actions to an id, and associates
	 * group traits to that id.
	 * 
	 * @param groupId
	 *            the group's id. It's the same id as
	 *            which you would recognize a user's company in your database.
	 * 
	 * @param traits
	 *            a dictionary with keys like plan or name. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 */
	public static void group(String groupId, Traits traits) {
		group(groupId, traits, null);
	}

	/**
	 * Identifying a group ties all of the group's actions to an id, and associates
	 * group traits to that id.
	 * 
	 * @param groupId
	 *            the group's id. It's the same id as
	 *            which you would recognize a user's company in your database.
	 * 
	 * @param traits
	 *            a dictionary with keys like plan or name. You only
	 *            need to record a trait once, no need to send it again.
	 * 
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 */
	public static void group(String groupId, Traits traits, Options options) {
		checkInitialized();
		if (optedOut) return;

		String userId = getUserId();
		groupId = getOrSetGroupId(groupId);
		
		if (groupId == null || groupId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #group must be called with a valid group id.");
		}
		
		if (traits == null) traits = new Traits();
		if (options == null) options = new Options();

		Group group = new Group(userId, groupId, traits, options);
		enqueue(group);
		integrationManager.group(group);
		statistics.updateGroups(1);
	}
	
	//
	// Track
	//

	/**
	 * Whenever a user performs an event, you'll want to track it.
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
		track(event, null, null);
	}
	
	/**
	 * Whenever a user performs an event, you'll want to track it.
	 * 
	 * Track will use an automatically generated userId unless one has been
	 * provided by identify(..).
	 * 
	 * @param event
	 *            describes what this user just did. It's a human readable
	 *            description like "Played a Song", "Printed a Report" or
	 *            "Updated Status".
	 *            
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 * 
	 */
	public static void track(String event, Options options) {
		track(event, null, options);
	}
	

	/**
	 * Whenever a user performs an event, you'll want to track it.
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
	 *            recommended. You'll find these properties extremely useful
	 *            later.
	 * 
	 */
	public static void track(String event, Props properties) {
		track(event, properties, null);
	}
	
	/**
	 * Whenever a user performs an action, you'll want to track it.
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
	 *            recommended. You'll find these properties extremely useful
	 *            later.
	 * 
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 * 
	 */
	public static void track(String event, Props properties, Options options) {
		checkInitialized();
		if (optedOut) return;
		
		String userId = getOrSetUserId(null);
		
		if (userId == null || userId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #track must be initialized with a valid user id.");
		}
		
		if (event == null || event.length() == 0) {
			throw new IllegalArgumentException("analytics-android #track must be initialized with a valid event name.");
		}
		
		if (properties == null) properties = new Props();
		if (options == null) options = new Options();

		Track track = new Track(userId, event, properties, options);
		enqueue(track);
		integrationManager.track(track);
		statistics.updateTracks(1);
	}

	//
	// Screen
	//
	
	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param name
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * 
	 */
	public static void screen(String name) {	
		screen(name, null, null, null);
	}

	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param name
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 * 
	 */
	public static void screen(String name, Options options) {	
		screen(name, null, null, options);
	}
	
	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param name
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * @param category
	 *            The (optional) category of the screen view, like "Sports" or "Authentication"
	 * 
	 */
	public static void screen(String name, String category) {	
		screen(name, category, null, null);
	}

	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param name
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * @param category
	 *            The (optional) category of the screen view, like "Sports" or "Authentication"
	 * 
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 */
	public static void screen(String name, String category, Options options) {	
		screen(name, category, null, options);
	}

	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param name
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * @param properties
	 *            a dictionary with items that describe the screen in more
	 *            detail. This argument is optional, but highly recommended. 
	 *            You'll find these properties extremely useful later.
	 * 
	 */
	public static void screen(String name, Props properties) {	
		screen(name, null, properties, null);
	}
	
	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param name
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * @param properties
	 *            a dictionary with items that describe the screen in more
	 *            detail. This argument is optional, but highly recommended. 
	 *            You'll find these properties extremely useful later.
	 * 
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 */
	public static void screen(String name, Props properties, Options options) {	
		screen(name, null, properties, options);
	}
	
	/**
	 * Whenever a user opens a new screen (or activity), track its screen view.
	 * Example:
	 * 
	 * 	Analytics.screen("Login");
	 * 
	 * You don't need to provide a userId to this method, because this library uses the most recent userId. 
	 * If identify(userId) or track(userId) was never called, a randomly generated session 
	 * id will be used. Otherwise, the most recent cached userId is used.  
	 * 
	 * @param name
	 *            describes the screen name of the activity that the user just
	 *            opened. We don't recommend to name each screen dynamically. For 
	 *            example, if a screen shows a new article, you should call it
	 *            "News Screen" instead of the name of the news article.
	 * 
	 * @param category
	 *            The (optional) category of the screen view, like "Sports" or "Authentication"
	 *            
	 * @param properties
	 *            a dictionary with items that describe the screen in more
	 *            detail. This argument is optional, but highly recommended. 
	 *            You'll find these properties extremely useful later.
	 * 
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 * 
	 */
	public static void screen(String name, String category, Props properties, Options options) {		
		checkInitialized();
		if (optedOut) return;
		
		String userId = getOrSetUserId(null);
		
		if (userId == null || userId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #screen must be initialized with a valid user id.");
		}
		
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException("analytics-android #screen must be initialized with a valid screen name.");
		}
		
		if (properties == null) properties = new Props();
		if (options == null) options = new Options();

		Screen screenAction = new Screen(userId, name, category, properties, options);		
		enqueue(screenAction);
		integrationManager.screen(screenAction);
		statistics.updateScreens(1);
	}
	
	//
	// Alias
	//

	/**
	 * Aliases an anonymous user into an identified user. 
	 * 
	 * This method is only needed for specific integrations.
	 * Check https://segment.io/docs for integration information. 
	 * 
	 * @param previousId
	 *            the anonymous user's id before they are logged in.
	 * 
	 * @param userId
	 *            the identified user's id after they're logged in.
	 *
	 */
	public static void alias(String previousId, String userId) {
		alias(previousId, userId, null);
	}
	
	/**
	 * Aliases an anonymous user into an identified user. 
	 * 
	 * This method is only needed for specific integrations.
	 * Check https://segment.io/docs for integration information. 
	 * 
	 * @param previousId
	 *            the anonymous user's id before they are logged in.
	 * 
	 * @param userId
	 *            the identified user's id after they're logged in.
	 * 
	 * @param options
	 *            options allows you to set a timestamp, an anonymousId, a context, 
	 *            and select specific integrations.
	 *
	 */
	public static void alias(String previousId, String userId, Options options) {
		checkInitialized();		
		if (optedOut) return;
		
		if (previousId == null || previousId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #alias must be initialized with a valid from id.");
		}
		
		if (userId == null || userId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #alias must be initialized with a valid to id.");
		}
		
		if (options == null) options = new Options();
		
		Alias alias = new Alias(previousId, userId, options);
		enqueue(alias);
		integrationManager.alias(alias);
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
				userId = anonymousIdCache.get();
		} else {
			// we were passed a user Id so let's save it
			userIdCache.set(userId);
		}
		
		return userId;
	}

	/**
	 * Gets or sets the current groupId. If the groupId
	 * is not null, then it will be set in the groupId cache and will
	 * be returned.
	 * @param groupId
	 * @return
	 */
	private static String getOrSetGroupId(String groupId) {
		
		if (TextUtils.isEmpty(groupId)) {
			// no group id provided, lets try to see if we have it saved
			groupId = groupIdCache.get();
		} else {
			// we were passed a user Id so let's save it
			groupIdCache.set(groupId);
		}
		
		return groupId;
	}
	
	/**
	 * Enqueues an {@link Identify}, {@link Track}, {@link Alias},
	 * or any action of type {@link BasePayload}
	 * @param payload
	 */
	public static void enqueue(final BasePayload payload) {
		statistics.updateInsertAttempts(1);
		
		// merge the global context into this payload's context
		payload.getContext().merge(globalContext);
		
		final long start = System.currentTimeMillis(); 
		
		databaseLayer.enqueue(payload, new EnqueueCallback() {
			
			@Override
			public void onEnqueue(boolean success, long rowCount) {
				
				long duration = System.currentTimeMillis() - start;
				statistics.updateInsertTime(duration);
				
				if (success) {
					Logger.i("Item " + payload.toDescription() + " successfully enqueued.");
				} else {
					Logger.w("Item " + payload.toDescription() + " failed to be enqueued.");
				}
				
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
		if (toggled) integrationManager.toggleOptOut(optOut);
	}
	
	//
	// Actions
	//

	/**
	 * Triggers a flush of data to the server.
	 * @param async True to block until the data is flushed
	 */
	public static void flush(boolean async) {
		checkInitialized();
		
		statistics.updateFlushAttempts(1);
		final long start = System.currentTimeMillis(); 
		final CountDownLatch latch = new CountDownLatch(1);
		
		flushLayer.flush(new FlushCallback() {
			@Override
			public void onFlushCompleted(boolean success, Batch batch) {
				latch.countDown();
				if (success) {
					long duration = System.currentTimeMillis() - start;
					statistics.updateFlushTime(duration);
				}
			}
		});

		// flush all the integrations as well
		integrationManager.flush();
		
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
			groupIdCache.reset();
			
			// reset all the integrations
			integrationManager.reset();
		}
	}
	
	/**
	 * Triggers a download of Segment.io integration settings
	 * from the server, and update of all the bundled integrations.
	 */
	public static void refreshSettings() {
		if (initialized) {
			integrationManager.refresh();
		}
	}
	
	/**
	 * Stops the analytics client threads, and resets the client
	 */
	public static void close() {
		checkInitialized();
		
		// stops the looper on the timer, flush, request, and database thread
		flushTimer.quit();
		refreshSettingsTimer.quit();
		flushLayer.quit();
		databaseLayer.quit();
		requestLayer.quit();
		settingsLayer.quit();

		// closes the database
		database.close();
		
		options = null;
		writeKey = null;
		
		initialized = false;
	}

	//
	// Getters and Setters
	//

	/**
	 * Gets the unique anonymous ID generated for this user
	 * until a userId is provided.
	 * 
	 * Use this ID as the "from" ID to alias your new
	 * userId if the user was just created.
	 * 
	 * @return
	 */
	public static String getAnonymousId() {
		checkInitialized();
		return anonymousIdCache.get();
	}
	
	/**
	 * Allows you to set your own anonymousId. Used mostly for testing.
	 * @param anonymousId
	 */
	public static void setAnonymousId(String anonymousId) {
		checkInitialized();
		anonymousIdCache.set(anonymousId);
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
	 * Gets the current Segment.io API writeKey
	 * @return
	 */
	public static String getWriteKey() {
		if (writeKey == null) checkInitialized();
		return writeKey;
	}

	public static void setWriteKey(String writeKey) {
		Analytics.writeKey = writeKey;
	}

	public static IntegrationManager getIntegrationManager() {
		return integrationManager;
	}
	
	/**
	 * Gets the Segment.io client options
	 * @return
	 */
	public static Config getOptions() {
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

	/**
	 * Allow custom {link {@link IRequester} for counting requests, bandwidth, or testing.
	 * @param requester
	 */
	public static void setRequester(IRequester requester) {
		if (requestLayer instanceof RequestThread) 
			((RequestThread) requestLayer).setRequester(requester);
	}
	
	
}
