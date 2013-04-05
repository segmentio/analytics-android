package io.segment.android;

import io.segment.android.db.IPayloadDatabaseLayer;
import io.segment.android.db.IPayloadDatabaseLayer.EnqueueCallback;
import io.segment.android.db.PayloadDatabase;
import io.segment.android.db.PayloadDatabaseThread;
import io.segment.android.flush.FlushThread;
import io.segment.android.flush.FlushThread.BatchFactory;
import io.segment.android.flush.IFlushLayer;
import io.segment.android.flush.IFlushLayer.FlushCallback;
import io.segment.android.models.Alias;
import io.segment.android.models.BasePayload;
import io.segment.android.models.Batch;
import io.segment.android.models.Context;
import io.segment.android.models.EventProperties;
import io.segment.android.models.Identify;
import io.segment.android.models.Track;
import io.segment.android.models.Traits;
import io.segment.android.request.BasicRequester;
import io.segment.android.request.IRequester;
import io.segment.android.utils.HandlerTimer;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.util.Log;

public class Analytics {

	private static final String TAG = Analytics.class.getName(); 
	
	private static String secret;
	private static Options options;
	
	private static HandlerTimer flushTimer;
	private static PayloadDatabase database;
	private static IPayloadDatabaseLayer databaseLayer;
	private static IFlushLayer flushLayer;
	
	private static boolean initialized;
	

	/**
	 * Initializes the Segment.io Android client.
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
	 *            Your Android application's context passed into your activities.
	 * 
	 * @param secret
	 *            Your segment.io secret. You can get one of these by
	 *            registering for a project at https://segment.io
	 * 
	 */
	public static void initialize(android.content.Context context, String secret) {

		initialize(context, secret, new Options());
	}

	/**
	 * Initializes the Segment.io Android client.
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
	 *            Your Android application's context passed into your activities.
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
		
		Analytics.secret = secret;
		Analytics.options = options;

	
		// create the database using the activity context
		database = PayloadDatabase.getInstance(context);
		
		IRequester requester = new BasicRequester();
		
		// now we need to create our singleton thread-safe database thread
		Analytics.databaseLayer = new PayloadDatabaseThread(database);
		Analytics.databaseLayer.start();
		
		// start the flush thread
		Analytics.flushLayer = new FlushThread(requester, 
											  batchFactory,
											  Analytics.databaseLayer);
		Analytics.flushLayer.start();
		
		Analytics.flushTimer = new HandlerTimer(options.getFlushAfter(), flushClock);
		Analytics.flushTimer.start();
		
		initialized = true;
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
					
			// TODO: add global batch settings from system information
			
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

		if (userId == null || userId.length() == 0) {
			throw new IllegalArgumentException("analytics-android #identify must be initialized with a valid user id.");
		}
		
		if (context == null)
			context = new Context();
		if (traits == null)
			traits = new Traits();

		Identify identify = new Identify(userId, traits, timestamp, context);

		enqueue(identify);
	}

	//
	// Track
	//

	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
	 * 
	 * @param event
	 *            describes what this user just did. It's a human readable
	 *            description like "Played a Song", "Printed a Report" or
	 *            "Updated Status".
	 * 
	 */
	public static void track(String userId, String event) {

		track(userId, event, null, null, null);
	}

	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
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
	public static void track(String userId, String event, EventProperties properties) {

		track(userId, event, properties, null, null);
	}

	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
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
	public static void track(String userId, String event, EventProperties properties,
			Calendar timestamp) {

		track(userId, event, properties, timestamp, null);
	}

	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
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
	public static void track(String userId, String event, EventProperties properties,
			 Context context) {

		track(userId, event, properties, null, context);
	}

	/**
	 * Whenever a user triggers an event, you’ll want to track it.
	 * 
	 * @param userId
	 *            the user's id after they are logged in. It's the same id as
	 *            which you would recognize a signed-in user in your system.
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
	public static void track(String userId, String event, EventProperties properties,
			Calendar timestamp, Context context) {

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
	}

	//
	// Internal
	//

	
	private static void enqueue(final BasePayload payload) {
		
		databaseLayer.enqueue(payload, new EnqueueCallback() {
			
			@Override
			public void onEnqueue(boolean success, long rowCount) {
				
				if (rowCount >= options.getFlushAt()) {
					Analytics.flush(true);
				}
			}
		});
	}
	
	//
	// Actions
	//
	
	/**
	 * Blocks until the queue is flushed
	 */
	public static void flush(boolean async) {
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		flushLayer.flush(new FlushCallback() {

			@Override
			public void onFullyFlushed() {
				latch.countDown();
			}
		});

		
		if (!async) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				Log.e(TAG, "Interrupted while waiting for a blocking flush.");
			}
		}
	}

	/**
	 * Stops the the database and flush thread
	 */
	public static  void close() {
		// stops the looper on the timer, flush, and database thread
		flushTimer.quit();
		flushLayer.quit();
		databaseLayer.quit();

		// closes the database
		database.close();
	}

	//
	// Getters and Setters
	//

	public static String getSecret() {
		return secret;
	}

	public static void setSecret(String secret) {
		Analytics.secret = secret;
	}

	public static Options getOptions() {
		return options;
	}

	/*
	public static Statistics getStatistics() {
		return operation.statistics;
	}
	*/
}
