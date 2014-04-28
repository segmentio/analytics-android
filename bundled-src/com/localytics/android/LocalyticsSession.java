// @formatter:off
/*
 * LocalyticsSession.java Copyright (C) 2013 Char Software Inc., DBA Localytics. This code is provided under the Localytics
 * Modified BSD License. A copy of this license has been distributed in a file called LICENSE with this source code. Please visit
 * www.localytics.com for more information.
 */
// @formatter:on

package com.localytics.android;

import android.Manifest.permission;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.location.Location;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.localytics.android.JsonObjects.BlobHeader;
import com.localytics.android.LocalyticsProvider.ApiKeysDbColumns;
import com.localytics.android.LocalyticsProvider.AttributesDbColumns;
import com.localytics.android.LocalyticsProvider.EventHistoryDbColumns;
import com.localytics.android.LocalyticsProvider.EventsDbColumns;
import com.localytics.android.LocalyticsProvider.IdentifiersDbColumns;
import com.localytics.android.LocalyticsProvider.InfoDbColumns;
import com.localytics.android.LocalyticsProvider.SessionsDbColumns;
import com.localytics.android.LocalyticsProvider.UploadBlobEventsDbColumns;
import com.localytics.android.LocalyticsProvider.UploadBlobsDbColumns;

/**
 * This class manages creating, collecting, and uploading a Localytics session. Please see the following guides for information on
 * how to best use this library, sample code, and other useful information:
 * <ul>
 * <li><a href="http://wiki.localytics.com/index.php?title=Developer's_Integration_Guide">Main Developer's Integration Guide</a></li>
 * <li><a href="http://wiki.localytics.com/index.php?title=Android_2_Minute_Integration">Android 2 minute integration Guide</a></li>
 * <li><a href="http://wiki.localytics.com/index.php?title=Android_Integration_Guide">Android Integration Guide</a></li>
 * </ul>
 * <p>
 * Permissions required:
 * <ul>
 * <li>{@link permission#INTERNET}</li> - Necessary to upload data to the webservice.</li>
 * </ul>
 * Permissions recommended:
 * <ul>
 * <li>{@link permission#ACCESS_WIFI_STATE}</li> - Necessary to identify the type of network connection the user has. Without this
 * permission, users connecting via Wi-Fi will be reported as having a connection type of 'unknown.'</li>
 * </ul>
 * <strong>Basic Usage</strong>
 * <ol>
 * <li>In {@code Activity#onCreate(Bundle)}, instantiate a {@link LocalyticsSession} object and assign it to a global variable in
 * the Activity (e.g. {@code mLocalyticsSession}).</li>
 * <li>In {@code Activity#onResume()}, call {@link #open()} or {@link #open(List)}.</li>
 * <li>In {@code Activity#onResume()}, consider calling {@link #upload()}. Because the session was just opened, this upload will
 * submit that open to the server and allow you to capture real-time usage of your application.</li>
 * <li>In {@code Activity#onResume()}, consider calling {@link #tagScreen(String)} to note that the user entered the Activity.
 * Assuming your application uses multiple Activities for navigation (rather than a single Activity with multiple Fragments}, this
 * will capture the flow of users as they move from Activity to Activity. Don't worry about Activity re-entrance. Because
 * {@code Activity#onResume()} can be called multiple times for different reasons, the Localytics library manages duplicate
 * {@link #tagScreen(String)} calls for you.</li>
 * <li>As the user interacts with your Activity, call {@link #tagEvent(String)}, {@link #tagEvent(String, Map)} or
 * {@link #tagEvent(String, Map, List)} to collect usage data.</li>
 * <li>In {@code Activity#onPause()}, call {@link #close()} or {@link #close(List)}.</li>
 * </ol>
 * <strong>Notes</strong>
 * <ul>
 * <li>Do not call any {@link LocalyticsSession} methods inside a loop. Instead, calls such as {@link #tagEvent(String)} should
 * follow user actions. This limits the amount of data which is stored and uploaded.</li>
 * <li>This library will create a database called "com.android.localytics.sqlite" within the host application's
 * {@link Context#getDatabasePath(String)} directory. For security, this file directory will be created
 * {@link Context#MODE_PRIVATE}. The host application must not modify this database file. If the host application implements a
 * backup/restore mechanism, such as {@code android.app.backup.BackupManager}, the host application should not worry about backing
 * up the data in the Localytics database.</li>
 * <li>This library is thread-safe but is not multi-process safe. Unless the application explicitly uses different process
 * attributes in the Android Manifest, this is not an issue. If you need to use multiple processes, then each process should have
 * its own Localytics API key in order to make data processing thread-safe.</li>
 * </ul>
 *
 * @version 2.0
 */
public class LocalyticsSession
{
    /*
     * DESIGN NOTES
     *
     * The LocalyticsSession stores all of its state as a SQLite database in the parent application's private database storage
     * directory.
     *
     * Every action performed within (open, close, opt-in, opt-out, customer events) are all treated as events by the library.
     * Events are given a package prefix to ensure a namespace without collisions. Events internal to the library are flagged with
     * the Localytics package name, while events from the customer's code are flagged with the customer's package name. There's no
     * need to worry about the customer changing the package name and disrupting the naming convention, as changing the package
     * name means that a new user is created in Android and the app with a new package name gets its own storage directory.
     *
     *
     * MULTI-THREADING
     *
     * The LocalyticsSession stores all of its state as a SQLite database in the parent application's private database storage
     * directory. Disk access is slow and can block the UI in Android, so the LocalyticsSession object is a wrapper around a pair
     * of Handler objects, with each Handler object running on its own separate thread.
     *
     * All requests made of the LocalyticsSession are passed along to the mSessionHandler object, which does most of the work. The
     * mSessionHandler will pass off upload requests to the mUploadHandler, to prevent the mSessionHandler from being blocked by
     * network traffic.
     *
     * If an upload request is made, the mSessionHandler will set a flag that an upload is in progress (this flag is important for
     * thread-safety of the session data stored on disk). Then the upload request is passed to the mUploadHandler's queue. If a
     * second upload request is made while the first one is underway, the mSessionHandler notifies the mUploadHandler, which will
     * notify the mSessionHandler to retry that upload request when the first upload is completed.
     *
     * Although each LocalyticsSession object will have its own unique instance of mSessionHandler, thread-safety is handled by
     * using a single sSessionHandlerThread.
     */

    /**
     * Format string for events
     */
    /* package */static final String EVENT_FORMAT = "%s:%s"; //$NON-NLS-1$

    /**
     * Open event
     */
    /* package */static final String OPEN_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "open"); //$NON-NLS-1$

    /**
     * Close event
     */
    /* package */static final String CLOSE_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "close"); //$NON-NLS-1$

    /**
     * Opt-in event
     */
    /* package */static final String OPT_IN_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "opt_in"); //$NON-NLS-1$

    /**
     * Opt-out event
     */
    /* package */static final String OPT_OUT_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "opt_out"); //$NON-NLS-1$

    /**
     * Flow event
     */
    /* package */static final String FLOW_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "flow"); //$NON-NLS-1$
    
    /**
     * Push Opened event
     */
    /* package */static final String PUSH_OPENED_EVENT = "Localytics Push Opened"; //$NON-NLS-1$    

    /**
     * Campaign ID attribute
     */
    /* package */static final String CAMPAIGN_ID_ATTRIBUTE = "Campaign ID"; //$NON-NLS-1$    

    /**
     * Creative ID attribute
     */
    /* package */static final String CREATIVE_ID_ATTRIBUTE = "Creative ID"; //$NON-NLS-1$    
    
    /**
     * Background thread used for all Localytics session processing. This thread is shared across all instances of
     * LocalyticsSession within a process.
     */
    /*
     * By using the class name for the HandlerThread, obfuscation through Proguard is more effective: if Proguard changes the
     * class name, the thread name also changes.
     */
    private static final HandlerThread sSessionHandlerThread = getHandlerThread(SessionHandler.class.getSimpleName());

    /**
     * Background thread used for all Localytics upload processing. This thread is shared across all instances of
     * LocalyticsSession within a process.
     */
    /*
     * By using the class name for the HandlerThread, obfuscation through Proguard is more effective: if Proguard changes the
     * class name, the thread name also changes.
     */
    protected static final HandlerThread sUploadHandlerThread = getHandlerThread(UploadHandler.class.getSimpleName());

    /**
     * Helper to obtain a new {@link HandlerThread}.
     *
     * @param name to give to the HandlerThread. Useful for debugging, as the thread name is shown in DDMS.
     * @return HandlerThread whose {@link HandlerThread#start()} method has already been called.
     */
    private static HandlerThread getHandlerThread(final String name)
    {
        final HandlerThread thread = new HandlerThread(name, android.os.Process.THREAD_PRIORITY_BACKGROUND);

        thread.start();

        /*
         * Note: we tried setting an uncaught exception handler here. But for some reason it causes looper initialization to fail
         * randomly.
         */

        return thread;
    }

    /**
     * Maps an API key to a singleton instance of the {@link SessionHandler}. Lazily initialized during construction of the
     * {@link LocalyticsSession} object.
     */
    private static final Map<String, SessionHandler> sLocalyticsSessionHandlerMap = new HashMap<String, SessionHandler>();

    /**
     * Intrinsic lock for synchronizing the initialization of {@link #sLocalyticsSessionHandlerMap}.
     */
    private static final Object[] sLocalyticsSessionIntrinsicLock = new Object[0];

    /**
     * Handler object where all session requests of this instance of LocalyticsSession are handed off to.
     * <p>
     * This Handler is the key thread synchronization point for all work inside the LocalyticsSession.
     * <p>
     * This handler runs on {@link #sSessionHandlerThread}.
     */
    private final Handler mSessionHandler;

    /**
     * Application context
     */
    private final Context mContext;
    
    /**
     * The most recent location. If non-null it will be included in sessions and events
     */
    private static Location lastLocation = null;

    /**
     * Keeps track of which Localytics clients are currently uploading, in order to allow only one upload for a given key at a
     * time.
     * <p>
     * This field can only be read/written to from the {@link #sSessionHandlerThread}. This invariant is maintained by only
     * accessing this field from within the {@link #mSessionHandler}.
     */
    protected static final Map<String, Boolean> sIsUploadingMap = new HashMap<String, Boolean>();
    
    /**
     * Constructs a new {@link LocalyticsSession} object.
     *
     * @param context The context used to access resources on behalf of the app. It is recommended to use
     *            {@link Context#getApplicationContext()} to avoid the potential memory leak incurred by maintaining references to
     *            {@code Activity} instances. Cannot be null.
     * @throws IllegalArgumentException if {@code context} is null
     * @throws IllegalArgumentException if LOCALYTICS_APP_KEY in AndroidManifest.xml is null or empty
     */
    public LocalyticsSession(final Context context)
    {
    	this(context, null);
    }
    
    /**
     * Constructs a new {@link LocalyticsSession} object.
     *
     * @param context The context used to access resources on behalf of the app. It is recommended to use
     *            {@link Context#getApplicationContext()} to avoid the potential memory leak incurred by maintaining references to
     *            {@code Activity} instances. Cannot be null.
     * @param key The key unique for each application generated at www.localytics.com. Cannot be null or empty.
     * @throws IllegalArgumentException if {@code context} is null
     * @throws IllegalArgumentException if {@code key} is null or empty
     */
    public LocalyticsSession(final Context context, final String key)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
        }
        
        String appKey = key;
        if (TextUtils.isEmpty(appKey))
        {
        	appKey = DatapointHelper.getLocalyticsAppKeyOrNull(context);
        }
        
        if (TextUtils.isEmpty(appKey))
        {
        	throw new IllegalArgumentException("key cannot be null or empty"); //$NON-NLS-1$
        }

        /*
         * Prevent the client from providing a subclass of Context that returns the Localytics package name.
         *
         * Note that because getPackageName() is a method and could theoretically return different results with each invocation,
         * this check doesn't guarantee that a nefarious caller will be detected.
         */
        if (Constants.LOCALYTICS_PACKAGE_NAME.equals(context.getPackageName())
                && !context.getClass().getName().equals("android.test.IsolatedContext") && !context.getClass().getName().equals("android.test.RenamingDelegatingContext")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            throw new IllegalArgumentException(String.format("context.getPackageName() returned %s", context.getPackageName())); //$NON-NLS-1$
        }

        /*
         * Get the application context to avoid having the Localytics object holding onto an Activity object. Using application
         * context is very important to prevent the customer from giving the library multiple different contexts with different
         * package names, which would corrupt the events in the database.
         *
         * Although RenamingDelegatingContext is part of the Android SDK, the class isn't present in the ClassLoader unless the
         * process is being run as a unit test. For that reason, comparing class names is necessary instead of doing instanceof.
         *
         * Note that getting the application context may have unpredictable results for apps sharing a process running Android 2.1
         * and earlier. See <http://code.google.com/p/android/issues/detail?id=4469> for details.
         */
        mContext = !(context.getClass().getName().equals("android.test.RenamingDelegatingContext")) && Constants.CURRENT_API_LEVEL >= 8 ? context.getApplicationContext() : context; //$NON-NLS-1$

        synchronized (sLocalyticsSessionIntrinsicLock)
        {
            SessionHandler handler = sLocalyticsSessionHandlerMap.get(appKey);

            if (null == handler)
            {
                handler = new SessionHandler(mContext, appKey, sSessionHandlerThread.getLooper());
                sLocalyticsSessionHandlerMap.put(appKey, handler);

                /*
                 * Complete Handler initialization on a background thread. Note that this is not generally a good best practice,
                 * as the LocalyticsSession object (and its child objects) should be fully initialized by the time the constructor
                 * returns. However this implementation is safe, as the Handler will process this initialization message before
                 * any other message.
                 */
                handler.sendMessage(handler.obtainMessage(SessionHandler.MESSAGE_INIT));
            }

            mSessionHandler = handler;
        }
    }

    /**
     * Sets the Localytics opt-out state for this application. This call is not necessary and is provided for people who wish to
     * allow their users the ability to opt out of data collection. It can be called at any time. Passing true causes all further
     * data collection to stop, and an opt-out event to be sent to the server so the user's data is removed from the charts. <br>
     * There are very serious implications to the quality of your data when providing an opt out option. For example, users who
     * have opted out will appear as never returning, causing your new/returning chart to skew. <br>
     * If two instances of the same application are running, and one is opted in and the second opts out, the first will also
     * become opted out, and neither will collect any more data. <br>
     * If a session was started while the app was opted out, the session open event has already been lost. For this reason, all
     * sessions started while opted out will not collect data even after the user opts back in or else it taints the comparisons
     * of session lengths and other metrics.
     *
     * @param isOptedOut True if the user should be be opted out and have all his Localytics data deleted.
     */
    public void setOptOut(final boolean isOptedOut)
    {
        mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_OPT_OUT, isOptedOut ? 1 : 0, 0));
    }

    /**
     * Behaves identically to calling {@code open(null)}.
     *
     * @see #open(List)
     */
    public void open()
    {
        open(null);
    }

    /**
     * Opens the Localytics session. The session should be opened before {@link #tagEvent(String)}, {@link #tagEvent(String, Map)}
     * , {@link #tagEvent(String, Map, List)}, or {@link #tagScreen(String)} are called.
     * <p>
     * If a new session is opened shortly--within a few seconds--after an earlier session is closed, Localytics will reconnect to
     * the previous session (effectively causing the previous close to be ignored). This ensures that as a user moves from
     * Activity to Activity in an app, that is considered a single session. When a session is reconnected, the
     * {@code customDimensions} for the initial open are kept and dimensions for the second open are ignored.
     * <p>
     * If for any reason open is called more than once without an intervening call to {@link #close()} or {@link #close(List)},
     * subsequent calls to open will be ignored.
     *
     * @param customDimensions A set of custom reporting dimensions. If this parameter is null or empty, then no custom dimensions
     *            are recorded and the behavior with respect to custom dimensions is like simply calling {@link #open()}. The
     *            number of dimensions is capped at four. If there are more than four elements, the extra elements are ignored.
     *            This parameter may not contain null or empty elements. This parameter is only used for enterprise level
     *            accounts. For non-enterprise accounts, custom dimensions will be uploaded but will not be accessible in reports
     *            until the account is upgraded to enterprise status.
     * @throws IllegalArgumentException if {@code customDimensions} contains null or empty elements.
     */
    public void open(final List<String> customDimensions)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (null != customDimensions)
            {
                /*
                 * Calling this with empty dimensions is a smell that indicates a possible programming error on the part of the
                 * caller
                 */
                if (customDimensions.isEmpty())
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "customDimensions is empty.  Did the caller make an error?"); //$NON-NLS-1$
                    }
                }

                if (customDimensions.size() > Constants.MAX_CUSTOM_DIMENSIONS)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, String.format("customDimensions size is %d, exceeding the maximum size of %d.  Did the caller make an error?", Integer.valueOf(customDimensions.size()), Integer.valueOf(Constants.MAX_CUSTOM_DIMENSIONS))); //$NON-NLS-1$
                    }
                }

                for (final String element : customDimensions)
                {
                    if (null == element)
                    {
                        throw new IllegalArgumentException("customDimensions cannot contain null elements"); //$NON-NLS-1$
                    }
                    if (0 == element.length())
                    {
                        throw new IllegalArgumentException("customDimensions cannot contain empty elements"); //$NON-NLS-1$
                    }
                }
            }
        }

        if (null == customDimensions || customDimensions.isEmpty())
        {
            mSessionHandler.sendEmptyMessage(SessionHandler.MESSAGE_OPEN);
        }
        else
        {
            mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_OPEN, new TreeMap<String, String>(convertDimensionsToAttributes(customDimensions))));
        }
    }

    /**
     * Behaves identically to calling {@code close(null)}.
     *
     * @see #close(List)
     */
    public void close()
    {
        close(null);
    }

    /**
     * Closes the Localytics session. Once a session has been opened via {@link #open()} or {@link #open(List)}, close the session
     * when data collection is complete.
     * <p>
     * If close is called without open having ever been called, the close has no effect. Similarly, once a session is closed,
     * subsequent calls to close will be ignored.
     *
     * @param customDimensions A set of custom reporting dimensions. If this parameter is null or empty, then no custom dimensions
     *            are recorded and the behavior with respect to custom dimensions is like simply calling {@link #close()}. The
     *            number of dimensions is capped at four. If there are more than four elements, the extra elements are ignored.
     *            This parameter may not contain null or empty elements. This parameter is only used for enterprise level
     *            accounts. For non-enterprise accounts, custom dimensions will be uploaded but will not be accessible in reports
     *            until the account is upgraded to enterprise status.
     * @throws IllegalArgumentException if {@code customDimensions} contains null or empty elements.
     */
    public void close(final List<String> customDimensions)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (null != customDimensions)
            {
                /*
                 * Calling this with empty dimensions is a smell that indicates a possible programming error on the part of the
                 * caller
                 */
                if (customDimensions.isEmpty())
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "customDimensions is empty.  Did the caller make an error?"); //$NON-NLS-1$
                    }
                }

                if (customDimensions.size() > Constants.MAX_CUSTOM_DIMENSIONS)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, String.format("customDimensions size is %d, exceeding the maximum size of %d.  Did the caller make an error?", Integer.valueOf(customDimensions.size()), Integer.valueOf(Constants.MAX_CUSTOM_DIMENSIONS))); //$NON-NLS-1$
                    }
                }

                for (final String element : customDimensions)
                {
                    if (null == element)
                    {
                        throw new IllegalArgumentException("customDimensions cannot contain null elements"); //$NON-NLS-1$
                    }
                    if (0 == element.length())
                    {
                        throw new IllegalArgumentException("customDimensions cannot contain empty elements"); //$NON-NLS-1$
                    }
                }
            }
        }

        if (null == customDimensions || customDimensions.isEmpty())
        {
            mSessionHandler.sendEmptyMessage(SessionHandler.MESSAGE_CLOSE);
        }
        else
        {
            mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_CLOSE, new TreeMap<String, String>(convertDimensionsToAttributes(customDimensions))));
        }
    }

    /**
     * Behaves identically to calling {@code tagEvent(event, null, null, 0)}.
     *
     * @see #tagEvent(String, Map, List, long)
     * @param event The name of the event which occurred. Cannot be null or empty string.
     * @throws IllegalArgumentException if {@code event} is null.
     * @throws IllegalArgumentException if {@code event} is empty.
     */
    public void tagEvent(final String event)
    {
        tagEvent(event, null);
    }

    /**
     * Behaves identically to calling {@code tagEvent(event, attributes, null, 0)}.
     *
     * @see #tagEvent(String, Map, List, long)
     * @param event The name of the event which occurred. Cannot be null or empty string.
     * @param attributes The collection of attributes for this particular event. If this parameter is null or empty, then calling
     *            this method has the same effect as calling {@link #tagEvent(String)}. This parameter may not contain null or
     *            empty keys or values.
     * @throws IllegalArgumentException if {@code event} is null.
     * @throws IllegalArgumentException if {@code event} is empty.
     * @throws IllegalArgumentException if {@code attributes} contains null keys, empty keys, null values, or empty values.
     */
    public void tagEvent(final String event, final Map<String, String> attributes)
    {
        tagEvent(event, attributes, null);
    }

    /**
     * Behaves identically to calling {@code tagEvent(event, attributes, customDimensions, 0)}.
     *
     * @see #tagEvent(String, Map, List, long)
     * @param event The name of the event which occurred. Cannot be null or empty string.
     * @param attributes The collection of attributes for this particular event. If this parameter is null or empty, then calling
     *            this method has the same effect as calling {@link #tagEvent(String)}. This parameter may not contain null or
     *            empty keys or values.
     * @param customDimensions A set of custom reporting dimensions. If this parameter is null or empty, then no custom dimensions
     *            are recorded and the behavior with respect to custom dimensions is like simply calling {@link #tagEvent(String)}
     *            . The number of dimensions is capped at four. If there are more than four elements, the extra elements are
     *            ignored. This parameter may not contain null or empty elements. This parameter is only used for enterprise level
     *            accounts. For non-enterprise accounts, custom dimensions will be uploaded but will not be accessible in reports
     *            until the account is upgraded to enterprise status.
     * @throws IllegalArgumentException if {@code event} is null.
     * @throws IllegalArgumentException if {@code event} is empty.
     * @throws IllegalArgumentException if {@code attributes} contains null keys, empty keys, null values, or empty values.
     * @throws IllegalArgumentException if {@code customDimensions} contains null or empty elements.
     */
    public void tagEvent(final String event, final Map<String, String> attributes, final List<String> customDimensions)
    {
        tagEvent(event, attributes, customDimensions, 0);
    }
    
    /**
     * <p>
     * Within the currently open session, tags that {@code event} occurred (with optionally included attributes and dimensions).
     * </p>
     * <p>
     * Attributes: Additional key/value pairs with data related to an event. For example, let's say your app displays a dialog
     * with two buttons: OK and Cancel. When the user clicks on one of the buttons, the event might be "button clicked." The
     * attribute key might be "button_label" and the value would either be "OK" or "Cancel" depending on which button was clicked.
     * </p>
     * <p>
	 * Custom dimensions:
	 * (PREMIUM ONLY) Sets the value of a custom dimension. Custom dimensions are dimensions
	 * which contain user defined data unlike the predefined dimensions such as carrier, model, and country.
	 * The proper use of custom dimensions involves defining a dimension with less than ten distinct possible
	 * values and assigning it to one of the fogur available custom dimensions. Once assigned this definition should
	 * never be changed without changing the App Key otherwise old installs of the application will pollute new data.
	 * </p>
     * <strong>Best Practices</strong>
     * <ul>
     * <li>DO NOT use events, attributes, or dimensions to record personally identifiable information.</li>
     * <li>The best way to use events is to create all the event strings as predefined constants and only use those. This is more
     * efficient and removes the risk of collecting personal information.</li>
     * <li>Do not tag events inside loops or any other place which gets called frequently. This can cause a lot of data to be
     * stored and uploaded.</li>
     * </ul>
     *
     * @param event The name of the event which occurred. Cannot be null or empty string.
     * @param attributes The collection of attributes for this particular event. If this parameter is null or empty, then no
     *            attributes are recorded and the behavior with respect to attributes is like simply calling
     *            {@link #tagEvent(String)}. This parameter may not contain null or empty keys or values.
     * @param customDimensions A set of custom reporting dimensions. If this parameter is null or empty, then no custom dimensions
     *            are recorded and the behavior with respect to custom dimensions is like simply calling {@link #tagEvent(String)}
     *            . The number of dimensions is capped at four. If there are more than four elements, the extra elements are
     *            ignored. This parameter may not contain null or empty elements. This parameter is only used for enterprise level
     *            accounts. For non-enterprise accounts, custom dimensions will be uploaded but will not be accessible in reports
     *            until the account is upgraded to enterprise status.
     * @param customerValueIncrease Added to customer lifetime value. Try to use lowest possible unit, such as cents for US currency. 
     * @throws IllegalArgumentException if {@code event} is null.
     * @throws IllegalArgumentException if {@code event} is empty.
     * @throws IllegalArgumentException if {@code attributes} contains null keys, empty keys, null values, or empty values.
     * @throws IllegalArgumentException if {@code customDimensions} contains null or empty elements.
     */
    public void tagEvent(final String event, final Map<String, String> attributes, final List<String> customDimensions, final long customerValueIncrease)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (null == event)
            {
                throw new IllegalArgumentException("event cannot be null"); //$NON-NLS-1$
            }

            if (0 == event.length())
            {
                throw new IllegalArgumentException("event cannot be empty"); //$NON-NLS-1$
            }

            if (null != attributes)
            {
                /*
                 * Calling this with empty attributes is a smell that indicates a possible programming error on the part of the
                 * caller
                 */
                if (attributes.isEmpty())
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "attributes is empty.  Did the caller make an error?"); //$NON-NLS-1$
                    }
                }

                if (attributes.size() > Constants.MAX_NUM_ATTRIBUTES)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, String.format("attributes size is %d, exceeding the maximum size of %d.  Did the caller make an error?", Integer.valueOf(attributes.size()), Integer.valueOf(Constants.MAX_NUM_ATTRIBUTES))); //$NON-NLS-1$
                    }
                }

                for (final Entry<String, String> entry : attributes.entrySet())
                {
                    final String key = entry.getKey();
                    final String value = entry.getValue();

                    if (null == key)
                    {
                        throw new IllegalArgumentException("attributes cannot contain null keys"); //$NON-NLS-1$
                    }
                    if (null == value)
                    {
                        throw new IllegalArgumentException("attributes cannot contain null values"); //$NON-NLS-1$
                    }
                    if (0 == key.length())
                    {
                        throw new IllegalArgumentException("attributes cannot contain empty keys"); //$NON-NLS-1$
                    }
                    if (0 == value.length())
                    {
                        throw new IllegalArgumentException("attributes cannot contain empty values"); //$NON-NLS-1$
                    }
                }
            }

            if (null != customDimensions)
            {
                /*
                 * Calling this with empty dimensions is a smell that indicates a possible programming error on the part of the
                 * caller
                 */
                if (customDimensions.isEmpty())
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "customDimensions is empty.  Did the caller make an error?"); //$NON-NLS-1$
                    }
                }

                if (customDimensions.size() > Constants.MAX_CUSTOM_DIMENSIONS)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, String.format("customDimensions size is %d, exceeding the maximum size of %d.  Did the caller make an error?", Integer.valueOf(customDimensions.size()), Integer.valueOf(Constants.MAX_CUSTOM_DIMENSIONS))); //$NON-NLS-1$
                    }
                }

                for (final String element : customDimensions)
                {
                    if (null == element)
                    {
                        throw new IllegalArgumentException("customDimensions cannot contain null elements"); //$NON-NLS-1$
                    }
                    if (0 == element.length())
                    {
                        throw new IllegalArgumentException("customDimensions cannot contain empty elements"); //$NON-NLS-1$
                    }
                }
            }
        }

        final String eventString = String.format(EVENT_FORMAT, mContext.getPackageName(), event);

        if (null == attributes && null == customDimensions)
        {
            mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_TAG_EVENT, new Triple<String, Map<String, String>, Long>(eventString, null, customerValueIncrease)));
        }
        else
        {
            /*
             * Convert the attributes and custom dimensions into the internal representation of packagename:key
             */

            final TreeMap<String, String> remappedAttributes = new TreeMap<String, String>();

            if (null != attributes)
            {
                final String packageName = mContext.getPackageName();
                for (final Entry<String, String> entry : attributes.entrySet())
                {
                    remappedAttributes.put(String.format(AttributesDbColumns.ATTRIBUTE_FORMAT, packageName, entry.getKey()), entry.getValue());
                }
            }

            if (null != customDimensions)
            {
                remappedAttributes.putAll(convertDimensionsToAttributes(customDimensions));
            }

            /*
             * Copying the map is very important to ensure that a client can't modify the map after this method is called. This is
             * especially important because the map is subsequently processed on a background thread.
             *
             * A TreeMap is used to ensure that the order that the attributes are written is deterministic. For example, if the
             * maximum number of attributes is exceeded the entries that occur later alphabetically will be skipped consistently.
             */

            mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_TAG_EVENT, new Triple<String, Map<String, String>, Long>(eventString, new TreeMap<String, String>(remappedAttributes), customerValueIncrease)));
        }
    }

    /**
     * Note: This implementation will perform duplicate suppression on two identical screen events that occur in a row within a
     * single session. For example, in the set of screens {"Screen 1", "Screen 1"} the second screen would be suppressed. However
     * in the set {"Screen 1", "Screen 2", "Screen 1"}, no duplicate suppression would occur.
     *
     * @param screen Name of the screen that was entered. Cannot be null or the empty string.
     * @throws IllegalArgumentException if {@code event} is null.
     * @throws IllegalArgumentException if {@code event} is empty.
     */
    public void tagScreen(final String screen)
    {
        if (null == screen)
        {
            throw new IllegalArgumentException("event cannot be null"); //$NON-NLS-1$
        }

        if (0 == screen.length())
        {
            throw new IllegalArgumentException("event cannot be empty"); //$NON-NLS-1$
        }

        mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_TAG_SCREEN, screen));
    }
    
    public void setCustomerEmail(final String email)
    {
    	setCustomerData("email", email);
    }
    
    public void setCustomerName(final String name)
    {
    	setCustomerData("customer_name", name);
    }
    
    public void setCustomerId(final String customerId)
    {
    	setCustomerData("customer_id", customerId);
    }
    
    public void setCustomerData(final String key, final String value)
    {
    	if(null == key)
    	{
    		throw new IllegalArgumentException("key cannot be null"); //$NON-NLS-1$
    	}
    	
    	mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_SET_IDENTIFIER, new Pair<String, String>(key, value)));
    }

    public void registerPush(final String senderId)
    {
    	if (DatapointHelper.getApiLevel() < 8)
    	{
    		if (Constants.IS_LOGGABLE)
    		{
    			Log.w(Constants.LOG_TAG, "GCM requires API level 8 or higher"); //$NON-NLS-1$
    		}
    	}

    	mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_REGISTER_PUSH, senderId));
    }

    public void handlePushReceived(final Intent intent)
    {
    	handlePushReceived(intent, null);
    }    
    
    public void handlePushReceived(final Intent intent, final List<String> customDimensions)
    {
        if (intent == null || intent.getExtras() == null) return;
        
        // Tag an event indicating the push was opened
        String llString = intent.getExtras().getString("ll");        
        if (llString != null)
        {
        	try 
        	{
        		JSONObject llObject = new JSONObject(llString);
        		String campaignId = llObject.getString("ca");
        		String creativeId = llObject.getString("cr");
        		
        		if (campaignId != null && creativeId != null)
        		{
        			HashMap<String, String> attributes = new HashMap<String, String>();
        			attributes.put(CAMPAIGN_ID_ATTRIBUTE, campaignId);
        			attributes.put(CREATIVE_ID_ATTRIBUTE, creativeId);
        			tagEvent(PUSH_OPENED_EVENT, attributes, customDimensions);
        		}
        		
        		// Remove the extra so we don't tag the same event a second time
        		intent.removeExtra("ll");
        	}
        	catch (JSONException e)
        	{
        		if (Constants.IS_LOGGABLE)
        		{
        			Log.w(Constants.LOG_TAG, "Failed to get campaign id or creatve id from payload"); //$NON-NLS-1$
        		}
        	}
        }        
    }

    public void setPushRegistrationId(final String pushRegId)
    {
    	mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_SET_PUSH_REGID, pushRegId));
    }
    
    public void setLocation(Location location)
    {
    	LocalyticsSession.lastLocation = location;
    }
    
    /**
     * Initiates an upload of any Localytics data for this session's API key. This should be done early in the process life in
     * order to guarantee as much time as possible for slow connections to complete. It is necessary to do this even if the user
     * has opted out because this is how the opt out is transported to the webservice.
     */
    public void upload()
    {
        mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, null));
    }
    

    /*
     * This is useful, but not necessarily needed for the public API. If so desired, someone can uncomment this out.
     */
    // /**
    // * Initiates an upload of any Localytics data for this session's API key. This should be done early in the process life in
    // * order to guarantee as much time as possible for slow connections to complete. It is necessary to do this even if the user
    // * has opted out because this is how the opt out is transported to the webservice.
    // *
    // * @param callback a Runnable to execute when the upload completes. A typical use case would be to notify the caller that
    // the
    // * upload has completed. This runnable will be executed on an undefined thread, so the caller should anticipate
    // * this runnable NOT executing on the main thread or the thread that calls {@link #upload}. This parameter may be
    // * null.
    // */
    // public void upload(final Runnable callback)
    // {
    // mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, callback));
    // }

    /**
     * Sorts an int value into a set of regular intervals as defined by the minimum, maximum, and step size. Both the min and max
     * values are inclusive, and in the instance where (max - min + 1) is not evenly divisible by step size, the method guarantees
     * only the minimum and the step size to be accurate to specification, with the new maximum will be moved to the next regular
     * step.
     *
     * @param actualValue The int value to be sorted.
     * @param minValue The int value representing the inclusive minimum interval.
     * @param maxValue The int value representing the inclusive maximum interval.
     * @param step The int value representing the increment of each interval.
     * @return a ranged attribute suitable for passing as the argument to {@link #tagEvent(String)} or
     *         {@link #tagEvent(String, Map)}.
     */
    public static String createRangedAttribute(final int actualValue, final int minValue, final int maxValue, final int step)
    {
        // Confirm there is at least one bucket
        if (step < 1)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, "Step must not be less than zero.  Returning null."); //$NON-NLS-1$
            }
            return null;
        }
        if (minValue >= maxValue)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, "maxValue must not be less than minValue.  Returning null."); //$NON-NLS-1$
            }
            return null;
        }

        // Determine the number of steps, rounding up using int math
        final int stepQuantity = (maxValue - minValue + step) / step;
        final int[] steps = new int[stepQuantity + 1];
        for (int currentStep = 0; currentStep <= stepQuantity; currentStep++)
        {
            steps[currentStep] = minValue + (currentStep) * step;
        }
        return createRangedAttribute(actualValue, steps);
    }

    /**
     * Sorts an int value into a predefined, pre-sorted set of intervals, returning a string representing the new expected value.
     * The array must be sorted in ascending order, with the first element representing the inclusive lower bound and the last
     * element representing the exclusive upper bound. For instance, the array [0,1,3,10] will provide the following buckets: less
     * than 0, 0, 1-2, 3-9, 10 or greater.
     *
     * @param actualValue The int value to be bucketed.
     * @param steps The sorted int array representing the bucketing intervals.
     * @return String representation of {@code actualValue} that has been bucketed into the range provided by {@code steps}.
     * @throws IllegalArgumentException if {@code steps} is null.
     * @throws IllegalArgumentException if {@code steps} has length 0.
     */
    public static String createRangedAttribute(final int actualValue, final int[] steps)
    {
        if (null == steps)
        {
            throw new IllegalArgumentException("steps cannot be null"); //$NON-NLS-1$
        }

        if (steps.length == 0)
        {
            throw new IllegalArgumentException("steps length must be greater than 0"); //$NON-NLS-1$
        }

        String bucket = null;

        // if less than smallest value
        if (actualValue < steps[0])
        {
            bucket = "less than " + steps[0];
        }
        // if greater than largest value
        else if (actualValue >= steps[steps.length - 1])
        {
            bucket = steps[steps.length - 1] + " and above";
        }
        else
        {
            // binarySearch returns the index of the value, or (-(insertion point) - 1) if not found
            int bucketIndex = Arrays.binarySearch(steps, actualValue);
            if (bucketIndex < 0)
            {
                // if the index wasn't found, then we want the value before the insertion point as the lower end
                // the special case where the insertion point is 0 is covered above, so we don't have to worry about it here
                bucketIndex = (-bucketIndex) - 2;
            }
            if (steps[bucketIndex] == (steps[bucketIndex + 1] - 1))
            {
                bucket = Integer.toString(steps[bucketIndex]);
            }
            else
            {
                bucket = steps[bucketIndex] + "-" + (steps[bucketIndex + 1] - 1); //$NON-NLS-1$
            }
        }
        return bucket;
    }

    /**
     * Helper to convert a list of dimensions into a set of attributes.
     * <p>
     * The number of dimensions is capped at 4. If there are more than 4 elements in {@code customDimensions}, all elements after
     * 4 are ignored.
     *
     * @param customDimensions List of dimensions to convert.
     * @return Attributes map for the set of dimensions.
     */
    private static Map<String, String> convertDimensionsToAttributes(final List<String> customDimensions)
    {
        final TreeMap<String, String> attributes = new TreeMap<String, String>();

        if (null != customDimensions)
        {
            int index = 0;
            for (final String element : customDimensions)
            {
                if (0 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1, element);
                }
                else if (1 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2, element);
                }
                else if (2 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3, element);
                }
                else if (3 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4, element);
                }
                else if (4 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5, element);
                }
                else if (5 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6, element);
                }
                else if (6 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7, element);
                }
                else if (7 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8, element);
                }
                else if (8 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9, element);
                }
                else if (9 == index)
                {
                    attributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10, element);
                }

                index++;
            }
        }

        return attributes;
    }

    /**
     * Helper class to handle session-related work on the {@link LocalyticsSession#sSessionHandlerThread}.
     */
    /* package */static final class SessionHandler extends Handler
    {
        /**
         * Empty handler message to initialize the callback.
         * <p>
         * This message must be sent before any other messages.
         */
        public static final int MESSAGE_INIT = 0;

        /**
         * Handler message to open a session.
         * <p>
         * {@link Message#obj} is either null or a {@code Map<String, String>} containing attributes for the open.
         */
        public static final int MESSAGE_OPEN = 1;

        /**
         * Handler message to close a session.
         * <p>
         * {@link Message#obj} is either null or a {@code Map<String, String>} containing attributes for the close.
         */
        public static final int MESSAGE_CLOSE = 2;

        /**
         * Handler message to tag an event.
         * <p>
         * {@link Message#obj} is a {@link Pair} instance. This object cannot be null.
         */
        public static final int MESSAGE_TAG_EVENT = 3;

        /**
         * Handler message to upload all data collected so far
         * <p>
         * {@link Message#obj} is a {@code Runnable} to execute when upload is complete. The thread that this runnable will
         * executed on is undefined.
         */
        public static final int MESSAGE_UPLOAD = 4;

        /**
         * Empty Handler message indicating that a previously requested upload attempt was completed. This does not mean the
         * attempt was successful. A callback occurs regardless of whether upload succeeded.
         */
        public static final int MESSAGE_UPLOAD_CALLBACK = 5;

        /**
         * Handler message indicating an opt-out choice.
         * <p>
         * {@link Message#arg1} == 1 for true (opt out). 0 means opt-in.
         */
        public static final int MESSAGE_OPT_OUT = 6;

        /**
         * Handler message indicating a tag screen event
         * <p>
         * {@link Message#obj} is a string representing the screen visited.
         */
        public static final int MESSAGE_TAG_SCREEN = 7;
        
        /**
         * Handler message indicating a set identifier action
         * <p>
         * {@link Message#obj} is a string representing the screen visited.
         */
        public static final int MESSAGE_SET_IDENTIFIER = 8;

        /**
         * Handler message to register with GCM
         * <p>
         * {@link Message#obj} is a string representing the sender id.
         */
        public static final int MESSAGE_REGISTER_PUSH = 9;

        /**
         * Handler message to set the GCM registration id
         * <p>
         * {@link Message#obj} is a string representing the push registration id.
         */
        public static final int MESSAGE_SET_PUSH_REGID = 10;
        
        /**
         * Sort order for the upload blobs.
         * <p>
         * This is a workaround for Android bug 3707 <http://code.google.com/p/android/issues/detail?id=3707>.
         */
        private static final String UPLOAD_BLOBS_EVENTS_SORT_ORDER = String.format("CAST(%s AS TEXT)", UploadBlobEventsDbColumns.EVENTS_KEY_REF); //$NON-NLS-1$

        /**
         * Sort order for the events.
         * <p>
         * This is a workaround for Android bug 3707 <http://code.google.com/p/android/issues/detail?id=3707>.
         */
        private static final String EVENTS_SORT_ORDER = String.format("CAST(%s as TEXT)", EventsDbColumns._ID); //$NON-NLS-1$

        /**
         * Application context
         */
        private final Context mContext;

        /**
         * Localytics database
         */
        protected LocalyticsProvider mProvider;

        /**
         * The Localytics API key for the session.
         */
        private final String mApiKey;

        /**
         * {@link ApiKeysDbColumns#_ID} for the API key used by this Localytics session handler.
         */
        private long mApiKeyId;

        /**
         * Handler object where all upload of this instance of LocalyticsSession are handed off to.
         * <p>
         * This handler runs on {@link #sUploadHandlerThread}.
         */
        private Handler mUploadHandler;

        /**
         * Constructs a new Handler that runs on the given looper.
         *
         * @param context The context used to access resources on behalf of the app. It is recommended to use
         *            {@link Context#getApplicationContext()} to avoid the potential memory leak incurred by maintaining
         *            references to {@code Activity} instances. Cannot be null.
         * @param key The key unique for each application generated at www.localytics.com. Cannot be null or empty.
         * @param looper to run the Handler on. Cannot be null.
         * @throws IllegalArgumentException if {@code context} is null
         * @throws IllegalArgumentException if {@code key} is null or empty
         */
        public SessionHandler(final Context context, final String key, final Looper looper)
        {
            super(looper);

            if (Constants.IS_PARAMETER_CHECKING_ENABLED)
            {
                if (null == context)
                {
                    throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
                }
                if (TextUtils.isEmpty(key))
                {
                    throw new IllegalArgumentException("key cannot be null or empty"); //$NON-NLS-1$
                }
            }

            mContext = context;
            mApiKey = key;
        }

        @Override
        public void handleMessage(final Message msg)
        {
            try
            {
                super.handleMessage(msg);

                if (Constants.IS_LOGGABLE)
                {
                    Log.v(Constants.LOG_TAG, String.format("Handler received %s", msg)); //$NON-NLS-1$
                }

                switch (msg.what)
                {
                    case MESSAGE_INIT:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.v(Constants.LOG_TAG, "Handler received MESSAGE_INIT"); //$NON-NLS-1$
                        }

                        SessionHandler.this.init();

                        break;
                    }
                    case MESSAGE_OPT_OUT:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.v(Constants.LOG_TAG, "Handler received MESSAGE_OPT_OUT"); //$NON-NLS-1$
                        }

                        final boolean isOptingOut = msg.arg1 == 0 ? false : true;

                        mProvider.runBatchTransaction(new Runnable()
                        {
                            public void run()
                            {
                                SessionHandler.this.optOut(isOptingOut);
                            }
                        });

                        break;
                    }
                    case MESSAGE_OPEN:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.v(Constants.LOG_TAG, "Handler received MESSAGE_OPEN"); //$NON-NLS-1$
                        }

                        mProvider.runBatchTransaction(new Runnable()
                        {
                            @SuppressWarnings("unchecked")
                            public void run()
                            {
                                SessionHandler.this.open(false, (Map<String, String>) msg.obj);
                            }
                        });

                        break;
                    }
                    case MESSAGE_CLOSE:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "Handler received MESSAGE_CLOSE"); //$NON-NLS-1$
                        }

                        mProvider.runBatchTransaction(new Runnable()
                        {
                            @SuppressWarnings("unchecked")
                            public void run()
                            {
                                SessionHandler.this.close((Map<String, String>) msg.obj);
                            }
                        });

                        break;
                    }
                    case MESSAGE_TAG_EVENT:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "Handler received MESSAGE_TAG_EVENT"); //$NON-NLS-1$
                        }

                        @SuppressWarnings("unchecked")
                        final Triple<String, Map<String, String>, Long> triple = (Triple<String, Map<String, String>, Long>) msg.obj;
 
                        final String event = triple.first;
                        final Map<String, String> attributes = triple.second;
                        final Long clv = triple.third;

                        mProvider.runBatchTransaction(new Runnable()
                        {
                            public void run()
                            {
                                if (null != getOpenSessionId(mProvider))
                                {
                                    tagEvent(event, attributes, clv);
                                }
                                else
                                {
                                    /*
                                     * The open and close only care about custom dimensions
                                     */
                                    final Map<String, String> openCloseAttributes;
                                    if (null == attributes)
                                    {
                                        openCloseAttributes = null;
                                    }
                                    else if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1)
                                            || attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2)
                                            || attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3)
                                            || attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4)
                                            || attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5)
                                            || attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6)
                                            || attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7)
                                            || attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8)
                                            || attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9)
                                            || attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10))
                                    {
                                        openCloseAttributes = new TreeMap<String, String>();
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1));
                                        }
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2));
                                        }
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3));
                                        }
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4));
                                        }
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5));
                                        }
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6));
                                        }
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7));
                                        }
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8));
                                        }
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9));
                                        }
                                        if (attributes.containsKey(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10))
                                        {
                                            openCloseAttributes.put(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10, attributes.get(AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10));
                                        }
                                    }
                                    else
                                    {
                                        openCloseAttributes = null;
                                    }

                                    open(false, openCloseAttributes);
                                    tagEvent(event, attributes, clv);
                                    close(openCloseAttributes);
                                }
                            }
                        });

                        break;
                    }
                    case MESSAGE_TAG_SCREEN:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "Handler received MESSAGE_TAG_SCREEN"); //$NON-NLS-1$
                        }

                        final String screen = (String) msg.obj;

                        mProvider.runBatchTransaction(new Runnable()
                        {
                            public void run()
                            {
                                SessionHandler.this.tagScreen(screen);
                            }
                        });

                        break;
                    }
                    case MESSAGE_SET_IDENTIFIER:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "Handler received MESSAGE_SET_IDENTIFIER"); //$NON-NLS-1$
                        }

                        @SuppressWarnings("unchecked")
                        final Pair<String, String> pair = (Pair<String, String>) msg.obj;
                        final String key = pair.first;
                        final String value = pair.second;
                        
                        mProvider.runBatchTransaction(new Runnable()
                        {
                            public void run()
                            {
                                SessionHandler.this.setIdentifier(key, value);
                            }
                        });

                        break;
                    }
                    case MESSAGE_REGISTER_PUSH:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "Handler received MESSAGE_REGISTER_PUSH"); //$NON-NLS-1$
                        }

                        @SuppressWarnings("unchecked")
                        final String senderId = (String) msg.obj;
                        
                        mProvider.runBatchTransaction(new Runnable()
                        {
                        	public void run()
                        	{
		                        Cursor cursor = null;
		                        
		                        String pushRegId = null;
		                        String pushRegVersion = null;
		                        try
		                        {
		                            cursor = mProvider.query(InfoDbColumns.TABLE_NAME, null, null, null, null); //$NON-NLS-1$
		
		                            if (cursor.moveToFirst())
		                            {           
		                            	pushRegVersion = cursor.getString(cursor.getColumnIndexOrThrow(InfoDbColumns.REGISTRATION_VERSION));
		                            	pushRegId = cursor.getString(cursor.getColumnIndexOrThrow(InfoDbColumns.REGISTRATION_ID));
		                            }
		                        }
		                        finally
		                        {
		                            if (null != cursor)
		                            {
		                                cursor.close();
		                                cursor = null;
		                            }
		                        }
		                        
		                        final String appVersion = DatapointHelper.getAppVersion(mContext);
		                        		                        
		                        // Only register if we don't have a registration id or if the app version has changed
		                        if (pushRegId == null || TextUtils.isEmpty(pushRegId) || !appVersion.equals(pushRegVersion))
		                        {
			                        Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
			                        registrationIntent.putExtra("app", PendingIntent.getBroadcast(mContext, 0, new Intent(), 0));
			                        registrationIntent.putExtra("sender", senderId);
			                        mContext.startService(registrationIntent);
		                        }
                        	}
                        });
                        
                        break;
                    }                                        
                    case MESSAGE_SET_PUSH_REGID:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "Handler received MESSAGE_SET_PUSH_REGID"); //$NON-NLS-1$
                        }

                        @SuppressWarnings("unchecked")
                        final String pushRegId = (String) msg.obj;
                        
                        mProvider.runBatchTransaction(new Runnable()
                        {
                            public void run()
                            {
                                SessionHandler.this.setPushRegistrationId(pushRegId);
                            }
                        });

                        break;
                    }     
                    case MESSAGE_UPLOAD:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "SessionHandler received MESSAGE_UPLOAD"); //$NON-NLS-1$
                        }

                        /*
                         * Note that callback may be null
                         */
                        final Runnable callback = (Runnable) msg.obj;

                        mProvider.runBatchTransaction(new Runnable()
                        {
                            public void run()
                            {
                                SessionHandler.this.upload(callback);
                            }
                        });

                        break;
                    }
                    case MESSAGE_UPLOAD_CALLBACK:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "Handler received MESSAGE_UPLOAD_CALLBACK"); //$NON-NLS-1$
                        }

                        sIsUploadingMap.put(mApiKey, Boolean.FALSE);

                        break;
                    }
                    default:
                    {
                        /*
                         * This should never happen
                         */
                        throw new RuntimeException("Fell through switch statement"); //$NON-NLS-1$
                    }
                }
            }
            catch (final Exception e)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.e(Constants.LOG_TAG, "Localytics library threw an uncaught exception", e); //$NON-NLS-1$
                }

                if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * Projection for querying details of the current API key
         */
        private static final String[] PROJECTION_INIT_API_KEY = new String[]
            {
                ApiKeysDbColumns._ID,
                ApiKeysDbColumns.OPT_OUT,
                ApiKeysDbColumns.UUID };

        /**
         * Selection for a specific API key ID
         */
        private static final String SELECTION_INIT_API_KEY = String.format("%s = ?", ApiKeysDbColumns.API_KEY); //$NON-NLS-1$

        /**
         * Initialize the handler post construction.
         * <p>
         * This method must only be called once.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_INIT} to the Handler.
         *
         * @see #MESSAGE_INIT
         */
        /* package */void init()
        {
            mProvider = LocalyticsProvider.getInstance(mContext, mApiKey);

            Cursor cursor = null;
            try
            {
                cursor = mProvider.query(ApiKeysDbColumns.TABLE_NAME, PROJECTION_INIT_API_KEY, SELECTION_INIT_API_KEY, new String[]
                    { mApiKey }, null);

                if (cursor.moveToFirst())
                {
                    // API key was previously created
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, String.format("Loading details for API key %s", mApiKey)); //$NON-NLS-1$
                    }

                    mApiKeyId = cursor.getLong(cursor.getColumnIndexOrThrow(ApiKeysDbColumns._ID));
                }
                else
                {
                    // perform first-time initialization of API key
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, String.format("Performing first-time initialization for new API key %s", mApiKey)); //$NON-NLS-1$
                    }

                    final ContentValues values = new ContentValues();
                    values.put(ApiKeysDbColumns.API_KEY, mApiKey);
                    values.put(ApiKeysDbColumns.UUID, UUID.randomUUID().toString());
                    values.put(ApiKeysDbColumns.OPT_OUT, Boolean.FALSE);
                    values.put(ApiKeysDbColumns.CREATED_TIME, Long.valueOf(System.currentTimeMillis()));
                    
                    mApiKeyId = mProvider.insert(ApiKeysDbColumns.TABLE_NAME, values);
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            if (!sIsUploadingMap.containsKey(mApiKey))
            {
                sIsUploadingMap.put(mApiKey, Boolean.FALSE);
            }

            /*
             * Perform lazy initialization of the UploadHandler
             */
            mUploadHandler = new UploadHandler(mContext, this, mApiKey, getInstallationId(mProvider, mApiKey), sUploadHandlerThread.getLooper());
        }

        /**
         * Selection for {@link #optOut(boolean)}.
         */
        private static final String SELECTION_OPT_IN_OUT = String.format("%s = ?", ApiKeysDbColumns._ID); //$NON-NLS-1$

        /**
         * Set the opt-in/out-out state for all sessions using the current API key.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_OPT_OUT} to the Handler.
         * <p>
         * If a session is already open when an opt-out request is made, then data for the remainder of that session will be
         * collected. For example, calls to {@link #tagEvent(String, Map)} and {@link #tagScreen(String)} will be recorded until
         * {@link #close(Map)} is called.
         * <p>
         * If a session is not already open when an opt-out request is made, a new session is opened and closed by this method in
         * order to cause the opt-out event to be uploaded.
         *
         * @param isOptingOut true if the user is opting out. False if the user is opting back in.
         * @see #MESSAGE_OPT_OUT
         */
		/* package */void optOut(final boolean isOptingOut)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, String.format("Requested opt-out state is %b", Boolean.valueOf(isOptingOut))); //$NON-NLS-1$
            }

            // Do nothing if opt-out is unchanged
            if (isOptedOut(mProvider, mApiKey) == isOptingOut)
            {
                return;
            }

            if (null == getOpenSessionId(mProvider))
            {
                /*
                 * Force a session to contain the opt event
                 */
                open(true, null);
                tagEvent(isOptingOut ? OPT_OUT_EVENT : OPT_IN_EVENT, null);
                close(null);
            }
            else
            {
                tagEvent(isOptingOut ? OPT_OUT_EVENT : OPT_IN_EVENT, null);
            }

            final ContentValues values = new ContentValues();
            values.put(ApiKeysDbColumns.OPT_OUT, Boolean.valueOf(isOptingOut));
            mProvider.update(ApiKeysDbColumns.TABLE_NAME, values, SELECTION_OPT_IN_OUT, new String[]
                { Long.toString(mApiKeyId) });
        }

        /**
         * Projection for {@link #getOpenSessionId(LocalyticsProvider)}.
         */
        private static final String[] PROJECTION_GET_OPEN_SESSION_ID_SESSION_ID = new String[]
            { SessionsDbColumns._ID };

        /**
         * Projection for getting the event count in {@link #getOpenSessionId(LocalyticsProvider)}.
         */
        private static final String[] PROJECTION_GET_OPEN_SESSION_ID_EVENT_COUNT = new String[]
            { EventsDbColumns._COUNT };

        /**
         * Selection for {@link #getOpenSessionId(LocalyticsProvider)}.
         */
        private static final String SELECTION_GET_OPEN_SESSION_ID_EVENT_COUNT = String.format("%s = ? AND %s = ?", EventsDbColumns.SESSION_KEY_REF, EventsDbColumns.EVENT_NAME);

        /**
         * @param provider The database to query. Cannot be null.
         * @return The {@link SessionsDbColumns#_ID} of the currently open session or {@code null} if no session is open. The
         *         definition of "open" is whether a session has been opened without a corresponding close event.
         */
        /* package */static Long getOpenSessionId(final LocalyticsProvider provider)
        {
            /*
             * Get the ID of the last session
             */
            final Long sessionId;
            {
                Cursor sessionsCursor = null;
                try
                {

                    /*
                     * Query all sessions sorted by session ID, which guarantees to obtain the last session regardless of whether
                     * the system clock changed.
                     */
                    sessionsCursor = provider.query(SessionsDbColumns.TABLE_NAME, PROJECTION_GET_OPEN_SESSION_ID_SESSION_ID, null, null, SessionsDbColumns._ID);

                    if (sessionsCursor.moveToLast())
                    {
                        sessionId = Long.valueOf(sessionsCursor.getLong(sessionsCursor.getColumnIndexOrThrow(SessionsDbColumns._ID)));
                    }
                    else
                    {
                        return null;
                    }
                }
                finally
                {
                    if (null != sessionsCursor)
                    {
                        sessionsCursor.close();
                        sessionsCursor = null;
                    }
                }
            }

            /*
             * See if the session has a close event.
             */
            Cursor eventsCursor = null;
            try
            {
                eventsCursor = provider.query(EventsDbColumns.TABLE_NAME, PROJECTION_GET_OPEN_SESSION_ID_EVENT_COUNT, SELECTION_GET_OPEN_SESSION_ID_EVENT_COUNT, new String[]
                    {
                        sessionId.toString(),
                        CLOSE_EVENT }, null);

                if (eventsCursor.moveToFirst())
                {
                    if (0 == eventsCursor.getInt(0))
                    {
                        return sessionId;
                    }
                }
            }
            finally
            {
                if (null != eventsCursor)
                {
                    eventsCursor.close();
                    eventsCursor = null;
                }
            }

            return null;
        }

        /**
         * Projection for {@link #open(boolean, Map)}.
         */
        private static final String[] PROJECTION_OPEN_EVENT_ID = new String[]
            { EventsDbColumns._ID };

        /**
         * Selection for {@link #open(boolean, Map)}.
         */
        private static final String SELECTION_OPEN = String.format("%s = ? AND %s >= ?", EventsDbColumns.EVENT_NAME, EventsDbColumns.WALL_TIME); //$NON-NLS-1$

        /**
         * Projection for {@link #open(boolean, Map)}.
         */
        private static final String[] PROJECTION_OPEN_BLOB_EVENTS = new String[]
            { UploadBlobEventsDbColumns.EVENTS_KEY_REF };

        /**
         * Projection for {@link #open(boolean, Map)}.
         */
        private static final String[] PROJECTION_OPEN_SESSIONS = new String[]
            {
                SessionsDbColumns._ID,
                SessionsDbColumns.SESSION_START_WALL_TIME };

        /**
         * Selection for {@link #openNewSession(Map)}.
         */
        private static final String SELECTION_OPEN_NEW_SESSION = String.format("%s = ?", ApiKeysDbColumns.API_KEY); //$NON-NLS-1$

        /**
         * Selection for {@link #open(boolean, Map)}.
         */
        private static final String SELECTION_OPEN_DELETE_EMPTIES_EVENT_HISTORY_SESSION_KEY_REF = String.format("%s = ?", EventHistoryDbColumns.SESSION_KEY_REF); //$NON-NLS-1$

        /**
         * Selection for {@link #open(boolean, Map)}.
         */
        private static final String SELECTION_OPEN_DELETE_EMPTIES_EVENTS_SESSION_KEY_REF = String.format("%s = ?", EventsDbColumns.SESSION_KEY_REF); //$NON-NLS-1$

        /**
         * Projection for {@link #open(boolean, Map)}.
         */
        private static final String[] PROJECTION_OPEN_DELETE_EMPTIES_EVENT_ID = new String[]
            { EventsDbColumns._ID };

        /**
         * Projection for {@link #open(boolean, Map)}.
         */
        private static final String[] PROJECTION_OPEN_DELETE_EMPTIES_PROCESSED_IN_BLOB = new String[]
            { EventHistoryDbColumns.PROCESSED_IN_BLOB };

        /**
         * Selection for {@link #open(boolean, Map)}.
         */
        private static final String SELECTION_OPEN_DELETE_EMPTIES_UPLOAD_BLOBS_ID = String.format("%s = ?", UploadBlobsDbColumns._ID); //$NON-NLS-1$

        /**
         * Selection for {@link #open(boolean, Map)}.
         */
        private static final String SELECTION_OPEN_DELETE_EMPTIES_SESSIONS_ID = String.format("%s = ?", SessionsDbColumns._ID); //$NON-NLS-1$

        /**
         * Open a session. While this method should only be called once without an intervening call to {@link #close(Map)},
         * nothing bad will happen if it is called multiple times.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_OPEN} to the Handler.
         *
         * @param ignoreLimits true to ignore limits on the number of sessions. False to enforce limits.
         * @param attributes Attributes to attach to the open. May be null indicating no attributes. Cannot contain null or empty
         *            keys or values.
         * @see #MESSAGE_OPEN
         */
        /* package */void open(final boolean ignoreLimits, final Map<String, String> attributes)
        {
        	if (null != getOpenSessionId(mProvider))
        	{
        		if (Constants.IS_LOGGABLE)
        		{
        			Log.w(Constants.LOG_TAG, "Session was already open"); //$NON-NLS-1$
        		}

        		return;
        	}

            if (isOptedOut(mProvider, mApiKey))
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.d(Constants.LOG_TAG, "Data collection is opted out"); //$NON-NLS-1$
                }
                return;
            }

            /*
             * There are two cases: 1. New session and 2. Re-connect to old session. There are two ways to reconnect to an old
             * session. One is by the age of the close event, and the other is by the age of the open event.
             */

            long closeEventId = -1; // sentinel value

            {
                Cursor eventsCursor = null;
                Cursor blob_eventsCursor = null;
                try
                {
                    eventsCursor = mProvider.query(EventsDbColumns.TABLE_NAME, PROJECTION_OPEN_EVENT_ID, SELECTION_OPEN, new String[]
                        {
                            CLOSE_EVENT,
                            Long.toString(System.currentTimeMillis() - Constants.SESSION_EXPIRATION) }, EVENTS_SORT_ORDER);
                    blob_eventsCursor = mProvider.query(UploadBlobEventsDbColumns.TABLE_NAME, PROJECTION_OPEN_BLOB_EVENTS, null, null, UPLOAD_BLOBS_EVENTS_SORT_ORDER);

                    final int idColumn = eventsCursor.getColumnIndexOrThrow(EventsDbColumns._ID);
                    final CursorJoiner joiner = new CursorJoiner(eventsCursor, PROJECTION_OPEN_EVENT_ID, blob_eventsCursor, PROJECTION_OPEN_BLOB_EVENTS);

                    for (final CursorJoiner.Result joinerResult : joiner)
                    {
                        switch (joinerResult)
                        {
                            case LEFT:
                            {

                                if (-1 != closeEventId)
                                {
                                    /*
                                     * This should never happen
                                     */
                                    if (Constants.IS_LOGGABLE)
                                    {
                                        Log.w(Constants.LOG_TAG, "There were multiple close events within SESSION_EXPIRATION"); //$NON-NLS-1$
                                    }

                                    final long newClose = eventsCursor.getLong(eventsCursor.getColumnIndexOrThrow(EventsDbColumns._ID));
                                    if (newClose > closeEventId)
                                    {
                                        closeEventId = newClose;
                                    }
                                }

                                if (-1 == closeEventId)
                                {
                                    closeEventId = eventsCursor.getLong(idColumn);
                                }

                                break;
                            }
                            case BOTH:
                                break;
                            case RIGHT:
                                break;
                        }
                    }
                    /*
                     * Verify that the session hasn't already been flagged for upload. That could happen if
                     */
                }
                finally
                {
                    if (null != eventsCursor)
                    {
                        eventsCursor.close();
                        eventsCursor = null;
                    }
                    if (null != blob_eventsCursor)
                    {
                        blob_eventsCursor.close();
                        blob_eventsCursor = null;
                    }
                }
            }

            if (-1 != closeEventId)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.v(Constants.LOG_TAG, "Opening old closed session and reconnecting"); //$NON-NLS-1$
                }

                openClosedSession(closeEventId);
            }
            else
            {
                Cursor sessionsCursor = null;
                try
                {
                    sessionsCursor = mProvider.query(SessionsDbColumns.TABLE_NAME, PROJECTION_OPEN_SESSIONS, null, null, SessionsDbColumns._ID);

                    if (sessionsCursor.moveToLast())
                    {
                        if (sessionsCursor.getLong(sessionsCursor.getColumnIndexOrThrow(SessionsDbColumns.SESSION_START_WALL_TIME)) >= System.currentTimeMillis()
                                - Constants.SESSION_EXPIRATION)
                        {
                            // reconnect
                            if (Constants.IS_LOGGABLE)
                            {
                                Log.v(Constants.LOG_TAG, "Opening old unclosed session and reconnecting"); //$NON-NLS-1$
                            }
                            return;
                        }

                        // delete empties
                        Cursor eventsCursor = null;
                        try
                        {
                            final String sessionId = Long.toString(sessionsCursor.getLong(sessionsCursor.getColumnIndexOrThrow(SessionsDbColumns._ID)));
                            final String[] sessionIdSelection = new String[]
                                { sessionId };
                            eventsCursor = mProvider.query(EventsDbColumns.TABLE_NAME, PROJECTION_OPEN_DELETE_EMPTIES_EVENT_ID, SELECTION_OPEN_DELETE_EMPTIES_EVENTS_SESSION_KEY_REF, sessionIdSelection, null);

                            if (eventsCursor.getCount() == 0)
                            {
                                final List<Long> blobsToDelete = new LinkedList<Long>();

                                // delete all event history and the upload blob
                                Cursor eventHistory = null;
                                try
                                {
                                    eventHistory = mProvider.query(EventHistoryDbColumns.TABLE_NAME, PROJECTION_OPEN_DELETE_EMPTIES_PROCESSED_IN_BLOB, SELECTION_OPEN_DELETE_EMPTIES_EVENT_HISTORY_SESSION_KEY_REF, sessionIdSelection, null);
                                    while (eventHistory.moveToNext())
                                    {
                                        blobsToDelete.add(Long.valueOf(eventHistory.getLong(eventHistory.getColumnIndexOrThrow(EventHistoryDbColumns.PROCESSED_IN_BLOB))));
                                    }
                                }
                                finally
                                {
                                    if (null != eventHistory)
                                    {
                                        eventHistory.close();
                                        eventHistory = null;
                                    }
                                }

                                mProvider.remove(EventHistoryDbColumns.TABLE_NAME, SELECTION_OPEN_DELETE_EMPTIES_EVENT_HISTORY_SESSION_KEY_REF, sessionIdSelection);
                                for (final long blobId : blobsToDelete)
                                {
                                    mProvider.remove(UploadBlobsDbColumns.TABLE_NAME, SELECTION_OPEN_DELETE_EMPTIES_UPLOAD_BLOBS_ID, new String[]
                                        { Long.toString(blobId) });
                                }
                                // mProvider.delete(AttributesDbColumns.TABLE_NAME, String.format("%s = ?",
                                // AttributesDbColumns.EVENTS_KEY_REF), selectionArgs)
                                mProvider.remove(SessionsDbColumns.TABLE_NAME, SELECTION_OPEN_DELETE_EMPTIES_SESSIONS_ID, sessionIdSelection);
                            }
                        }
                        finally
                        {
                            if (null != eventsCursor)
                            {
                                eventsCursor.close();
                                eventsCursor = null;
                            }
                        }
                    }
                }
                finally
                {
                    if (null != sessionsCursor)
                    {
                        sessionsCursor.close();
                        sessionsCursor = null;
                    }
                }

                /*
                 * Check that the maximum number of sessions hasn't been exceeded
                 */
                if (!ignoreLimits && getNumberOfSessions(mProvider) >= Constants.MAX_NUM_SESSIONS)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "Maximum number of sessions are already on disk--not writing any new sessions until old sessions are cleared out.  Try calling upload() to store more sessions."); //$NON-NLS-1$
                    }
                }
                else
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, "Opening new session"); //$NON-NLS-1$
                    }

                    openNewSession(attributes);
                }
            }
        }

        /**
         * Opens a new session. This is a helper method to {@link #open(boolean, Map)}.
         *
         * @effects Updates the database by creating a new entry in the {@link SessionsDbColumns} table.
         * @param attributes Attributes to attach to the session. May be null. Cannot contain null or empty keys or values.
         */
        private void openNewSession(final Map<String, String> attributes)
        {
            final TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

            final ContentValues values = new ContentValues();
            values.put(SessionsDbColumns.API_KEY_REF, Long.valueOf(mApiKeyId));
            values.put(SessionsDbColumns.SESSION_START_WALL_TIME, Long.valueOf(System.currentTimeMillis()));
            values.put(SessionsDbColumns.UUID, UUID.randomUUID().toString());
            values.put(SessionsDbColumns.APP_VERSION, DatapointHelper.getAppVersion(mContext));
            values.put(SessionsDbColumns.ANDROID_SDK, Integer.valueOf(Constants.CURRENT_API_LEVEL));
            values.put(SessionsDbColumns.ANDROID_VERSION, VERSION.RELEASE);

            // Try and get the deviceId. If it is unavailable (or invalid) use the installation ID instead.
            String deviceId = DatapointHelper.getAndroidIdHashOrNull(mContext);
            if (null == deviceId)
            {
                Cursor cursor = null;
                try
                {
                    cursor = mProvider.query(ApiKeysDbColumns.TABLE_NAME, null, SELECTION_OPEN_NEW_SESSION, new String[]
                        { mApiKey }, null);
                    if (cursor.moveToFirst())
                    {
                        deviceId = cursor.getString(cursor.getColumnIndexOrThrow(ApiKeysDbColumns.UUID));
                    }
                }
                finally
                {
                    if (null != cursor)
                    {
                        cursor.close();
                        cursor = null;
                    }
                }
            }

            values.put(SessionsDbColumns.DEVICE_ANDROID_ID_HASH, deviceId);
            values.put(SessionsDbColumns.DEVICE_ANDROID_ID, DatapointHelper.getAndroidIdOrNull(mContext));
            values.put(SessionsDbColumns.DEVICE_COUNTRY, telephonyManager.getSimCountryIso());
            values.put(SessionsDbColumns.DEVICE_MANUFACTURER, DatapointHelper.getManufacturer());
            values.put(SessionsDbColumns.DEVICE_MODEL, Build.MODEL);
            values.put(SessionsDbColumns.DEVICE_SERIAL_NUMBER_HASH, DatapointHelper.getSerialNumberHashOrNull());
            values.put(SessionsDbColumns.DEVICE_TELEPHONY_ID, DatapointHelper.getTelephonyDeviceIdOrNull(mContext));
            values.putNull(SessionsDbColumns.DEVICE_TELEPHONY_ID_HASH);
            values.putNull(SessionsDbColumns.DEVICE_WIFI_MAC_HASH);
            values.put(SessionsDbColumns.LOCALE_COUNTRY, Locale.getDefault().getCountry());
            values.put(SessionsDbColumns.LOCALE_LANGUAGE, Locale.getDefault().getLanguage());
            values.put(SessionsDbColumns.LOCALYTICS_LIBRARY_VERSION, Constants.LOCALYTICS_CLIENT_LIBRARY_VERSION);
            values.put(SessionsDbColumns.LOCALYTICS_INSTALLATION_ID, getInstallationId(mProvider, mApiKey));

            values.putNull(SessionsDbColumns.LATITUDE);
            values.putNull(SessionsDbColumns.LONGITUDE);
            values.put(SessionsDbColumns.NETWORK_CARRIER, telephonyManager.getNetworkOperatorName());
            values.put(SessionsDbColumns.NETWORK_COUNTRY, telephonyManager.getNetworkCountryIso());
            values.put(SessionsDbColumns.NETWORK_TYPE, DatapointHelper.getNetworkType(mContext, telephonyManager));

            long sessionId = mProvider.insert(SessionsDbColumns.TABLE_NAME, values);
            if (sessionId == -1)
            {
                throw new AssertionError("session insert failed"); //$NON-NLS-1$
            }

            tagEvent(OPEN_EVENT, attributes);

            /*
             * This is placed here so that the DatapointHelper has a chance to retrieve the old UUID before it is deleted.
             */
            LocalyticsProvider.deleteOldFiles(mContext);
        }

        /**
         * Projection for getting the installation ID. Used by {@link #getInstallationId(LocalyticsProvider, String)}.
         */
        private static final String[] PROJECTION_GET_INSTALLATION_ID = new String[]
            { ApiKeysDbColumns.UUID };

        /**
         * Selection for a specific API key ID. Used by {@link #getInstallationId(LocalyticsProvider, String)}.
         */
        private static final String SELECTION_GET_INSTALLATION_ID = String.format("%s = ?", ApiKeysDbColumns.API_KEY); //$NON-NLS-1$

        /**
         * Gets the installation ID of the API key.
         */
        /* package */ static String getInstallationId(final LocalyticsProvider provider, final String apiKey)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(ApiKeysDbColumns.TABLE_NAME, PROJECTION_GET_INSTALLATION_ID, SELECTION_GET_INSTALLATION_ID, new String[]
                    { apiKey }, null);

                if (cursor.moveToFirst())
                {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ApiKeysDbColumns.UUID));
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            /*
             * This error case shouldn't normally happen
             */
            if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "Installation ID couldn't be found"); //$NON-NLS-1$
            }
            return null;
        }
        
        /**
         * Gets Facebook attributon cookie for an app key
         *
         * @param provider Localytics database provider. Cannot be null.
         * @return The FB attribution cookie.
         */
        /* package */static String getFBAttribution(final LocalyticsProvider provider)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(InfoDbColumns.TABLE_NAME, null, null, null, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    return cursor.getString(cursor.getColumnIndexOrThrow(InfoDbColumns.FB_ATTRIBUTION));
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
            
            return null;
        }

        /**
         * Projection for {@link #openClosedSession(long)}.
         */
        private static final String[] PROJECTION_OPEN_CLOSED_SESSION = new String[]
            { EventsDbColumns.SESSION_KEY_REF };

        /**
         * Selection for {@link #openClosedSession(long)}.
         */
        private static final String SELECTION_OPEN_CLOSED_SESSION = String.format("%s = ?", EventsDbColumns._ID); //$NON-NLS-1$

        /**
         * Selection for {@link #openClosedSession(long)}.
         */
        private static final String SELECTION_OPEN_CLOSED_SESSION_ATTRIBUTES = String.format("%s = ?", AttributesDbColumns.EVENTS_KEY_REF); //$NON-NLS-1$

        /**
         * Reopens a previous session. This is a helper method to {@link #open(boolean, Map)}.
         *
         * @param closeEventId The last close event which is to be deleted so that the old session can be reopened
         * @effects Updates the database by deleting the last close event.
         */
        private void openClosedSession(final long closeEventId)
        {
            final String[] selectionArgs = new String[]
                { Long.toString(closeEventId) };

            Cursor cursor = null;
            try
            {
                cursor = mProvider.query(EventsDbColumns.TABLE_NAME, PROJECTION_OPEN_CLOSED_SESSION, SELECTION_OPEN_CLOSED_SESSION, selectionArgs, null);

                if (cursor.moveToFirst())
                {
                    mProvider.remove(AttributesDbColumns.TABLE_NAME, SELECTION_OPEN_CLOSED_SESSION_ATTRIBUTES, selectionArgs);
                    mProvider.remove(EventsDbColumns.TABLE_NAME, SELECTION_OPEN_CLOSED_SESSION, selectionArgs);
                }
                else
                {
                    /*
                     * This should never happen
                     */

                    if (Constants.IS_LOGGABLE)
                    {
                        Log.e(Constants.LOG_TAG, "Event no longer exists"); //$NON-NLS-1$
                    }

                    openNewSession(null);
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        /**
         * Projection for {@link #getNumberOfSessions(LocalyticsProvider)}.
         */
        private static final String[] PROJECTION_GET_NUMBER_OF_SESSIONS = new String[]
            { SessionsDbColumns._ID };

        /**
         * Helper method to get the number of sessions currently in the database.
         *
         * @param provider Instance of {@link LocalyticsProvider}. Cannot be null.
         * @return The number of sessions on disk.
         */
        /* package */static long getNumberOfSessions(final LocalyticsProvider provider)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(SessionsDbColumns.TABLE_NAME, PROJECTION_GET_NUMBER_OF_SESSIONS, null, null, null);

                return cursor.getCount();
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        /**
         * Close a session. While this method should only be called after {@link #open(boolean, Map)}, nothing bad will happen if
         * it is called and {@link #open(boolean, Map)} wasn't called. Similarly, nothing bad will happen if close is called
         * multiple times.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_CLOSE} to the Handler.
         *
         * @param attributes Set of attributes to attach to the close. May be null indicating no attributes. Cannot contain null
         *            or empty keys or values.
         * @see #MESSAGE_OPEN
         */
        /* package */void close(final Map<String, String> attributes)
        {
            if (null == getOpenSessionId(mProvider)) // do nothing if session is not open
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Session was not open, so close is not possible."); //$NON-NLS-1$
                }
                return;
            }

            tagEvent(CLOSE_EVENT, attributes);
        }

        /**
         * Projection for {@link #tagEvent(String, Map)}.
         */
        private static final String[] PROJECTION_TAG_EVENT = new String[]
            { SessionsDbColumns.SESSION_START_WALL_TIME };

        /**
         * Selection for {@link #tagEvent(String, Map)}.
         */
        private static final String SELECTION_TAG_EVENT = String.format("%s = ?", SessionsDbColumns._ID); //$NON-NLS-1$

        /**
         * Tag an event in a session. Although this method SHOULD NOT be called unless a session is open, actually doing so will
         * have no effect.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_TAG_EVENT} to the Handler.
         *
         * @param event The name of the event which occurred. Cannot be null.
         * @param attributes The collection of attributes for this particular event. May be null.
         * @see #MESSAGE_TAG_EVENT
         */
        /* package */void tagEvent(final String event, final Map<String, String> attributes)
        {
        	tagEvent(event, attributes, null);
        }
        
        /**
         * Tag an event in a session. Although this method SHOULD NOT be called unless a session is open, actually doing so will
         * have no effect.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_TAG_EVENT} to the Handler.
         *
         * @param event The name of the event which occurred. Cannot be null.
         * @param attributes The collection of attributes for this particular event. May be null.
         * @param clv The customer value increase.
         * @see #MESSAGE_TAG_EVENT
         */
        /* package */void tagEvent(final String event, final Map<String, String> attributes, final Long clv)
        {
            final Long openSessionId = getOpenSessionId(mProvider);
            if (null == openSessionId)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Event not written because a session is not open"); //$NON-NLS-1$
                }
                return;
            }

            /*
             * Insert the event and get the event's database ID
             */
            final long eventId;
            {
                final ContentValues values = new ContentValues();
                values.put(EventsDbColumns.SESSION_KEY_REF, openSessionId);
                values.put(EventsDbColumns.UUID, UUID.randomUUID().toString());
                values.put(EventsDbColumns.EVENT_NAME, event);
                values.put(EventsDbColumns.REAL_TIME, Long.valueOf(SystemClock.elapsedRealtime()));
                values.put(EventsDbColumns.WALL_TIME, Long.valueOf(System.currentTimeMillis()));
                
                if (null != clv)
                {
                	values.put(EventsDbColumns.CLV_INCREASE, clv);
                }
                else
                {
                	values.put(EventsDbColumns.CLV_INCREASE, 0);
                }
                
                if (lastLocation != null)
                {
                	values.put(EventsDbColumns.LAT_NAME, lastLocation.getLatitude());
                	values.put(EventsDbColumns.LNG_NAME, lastLocation.getLongitude());
                }
                
                /*
                 * Special case for open event: keep the start time in sync with the start time put into the sessions table.
                 */
                if (OPEN_EVENT.equals(event))
                {
                    Cursor cursor = null;
                    try
                    {
                        cursor = mProvider.query(SessionsDbColumns.TABLE_NAME, PROJECTION_TAG_EVENT, SELECTION_TAG_EVENT, new String[]
                            { openSessionId.toString() }, null);

                        if (cursor.moveToFirst())
                        {
                            values.put(EventsDbColumns.WALL_TIME, Long.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(SessionsDbColumns.SESSION_START_WALL_TIME))));
                        }
                        else
                        {
                            // this should never happen
                            throw new AssertionError("During tag of open event, session didn't exist"); //$NON-NLS-1$
                        }
                    }
                    finally
                    {
                        if (null != cursor)
                        {
                            cursor.close();
                            cursor = null;
                        }
                    }
                }

                eventId = mProvider.insert(EventsDbColumns.TABLE_NAME, values);

                if (-1 == eventId)
                {
                    throw new RuntimeException("Inserting event failed"); //$NON-NLS-1$
                }
            }

            /*
             * If attributes exist, insert them as well
             */
            if (null != attributes)
            {
                // reusable object
                final ContentValues values = new ContentValues();

                final String applicationAttributePrefix = String.format(AttributesDbColumns.ATTRIBUTE_FORMAT, mContext.getPackageName(), ""); //$NON-NLS-1$
                int applicationAttributeCount = 0;

                for (final Entry<String, String> entry : attributes.entrySet())
                {
                    /*
                     * Detect excess application events
                     */
                    if (entry.getKey().startsWith(applicationAttributePrefix))
                    {
                        applicationAttributeCount++;
                        if (applicationAttributeCount > Constants.MAX_NUM_ATTRIBUTES)
                        {
                            continue;
                        }
                    }

                    values.put(AttributesDbColumns.EVENTS_KEY_REF, Long.valueOf(eventId));
                    values.put(AttributesDbColumns.ATTRIBUTE_KEY, entry.getKey());
                    values.put(AttributesDbColumns.ATTRIBUTE_VALUE, entry.getValue());

                    final long id = mProvider.insert(AttributesDbColumns.TABLE_NAME, values);

                    if (-1 == id)
                    {
                        throw new AssertionError("Inserting attribute failed"); //$NON-NLS-1$
                    }

                    values.clear();
                }
            }

            /*
             * Insert the event into the history, only for application events
             */
            if (!OPEN_EVENT.equals(event) && !CLOSE_EVENT.equals(event) && !OPT_IN_EVENT.equals(event) && !OPT_OUT_EVENT.equals(event) && !FLOW_EVENT.equals(event))
            {
                final ContentValues values = new ContentValues();
                values.put(EventHistoryDbColumns.NAME, event.substring(mContext.getPackageName().length() + 1, event.length()));
                values.put(EventHistoryDbColumns.TYPE, Integer.valueOf(EventHistoryDbColumns.TYPE_EVENT));
                values.put(EventHistoryDbColumns.SESSION_KEY_REF, openSessionId);
                values.putNull(EventHistoryDbColumns.PROCESSED_IN_BLOB);
                mProvider.insert(EventHistoryDbColumns.TABLE_NAME, values);

                conditionallyAddFlowEvent();
            }
        }

        /**
         * Projection for {@link #tagScreen(String)}.
         */
        private static final String[] PROJECTION_TAG_SCREEN = new String[]
            { EventHistoryDbColumns.NAME };

        /**
         * Selection for {@link #tagScreen(String)}.
         */
        private static final String SELECTION_TAG_SCREEN = String.format("%s = ? AND %s = ?", EventHistoryDbColumns.TYPE, EventHistoryDbColumns.SESSION_KEY_REF); //$NON-NLS-1$

        /**
         * Sort order for {@link #tagScreen(String)}.
         */
        private static final String SORT_ORDER_TAG_SCREEN = String.format("%s DESC", EventHistoryDbColumns._ID); //$NON-NLS-1$

        /**
         * Tag a screen in a session. While this method shouldn't be called unless {@link #open(boolean, Map)} is called first,
         * this method will simply do nothing if {@link #open(boolean, Map)} hasn't been called.
         * <p>
         * This method performs duplicate suppression, preventing multiple screens with the same value in a row within a given
         * session.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made public for unit testing purposes. The public
         * interface is to send {@link #MESSAGE_TAG_SCREEN} to the Handler.
         *
         * @param screen The name of the screen which occurred. Cannot be null or empty.
         * @see #MESSAGE_TAG_SCREEN
         */
        /* package */void tagScreen(final String screen)
        {
            final Long openSessionId = getOpenSessionId(mProvider);
            if (null == openSessionId)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Tag not written because the session was not open"); //$NON-NLS-1$
                }
                return;
            }

            /*
             * Do duplicate suppression
             */
            Cursor cursor = null;
            try
            {
                cursor = mProvider.query(EventHistoryDbColumns.TABLE_NAME, PROJECTION_TAG_SCREEN, SELECTION_TAG_SCREEN, new String[]
                    {
                        Integer.toString(EventHistoryDbColumns.TYPE_SCREEN),
                        openSessionId.toString() }, SORT_ORDER_TAG_SCREEN);

                if (cursor.moveToFirst())
                {
                    if (screen.equals(cursor.getString(cursor.getColumnIndexOrThrow(EventHistoryDbColumns.NAME))))
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.v(Constants.LOG_TAG, String.format("Suppressed duplicate screen %s", screen)); //$NON-NLS-1$
                        }
                        return;
                    }
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            /*
             * Write the screen to the database
             */
            final ContentValues values = new ContentValues();
            values.put(EventHistoryDbColumns.NAME, screen);
            values.put(EventHistoryDbColumns.TYPE, Integer.valueOf(EventHistoryDbColumns.TYPE_SCREEN));
            values.put(EventHistoryDbColumns.SESSION_KEY_REF, openSessionId);
            values.putNull(EventHistoryDbColumns.PROCESSED_IN_BLOB);
            mProvider.insert(EventHistoryDbColumns.TABLE_NAME, values);

            conditionallyAddFlowEvent();
        }
        
        /**
         * Projection for {@link #tagScreen(String)}.
         */
        private static final String[] PROJECTION_SET_IDENTIFIER = new String[]
            { IdentifiersDbColumns.VALUE };

        /**
         * Selection for {@link #tagScreen(String)}.
         */
        private static final String SELECTION_SET_IDENTIFIER = String.format("%s = ?", IdentifiersDbColumns.KEY); //$NON-NLS-1$

        
        /* package */void setIdentifier(final String key, final String value)
        {
            Cursor cursor = null;
            try
            {
                cursor = mProvider.query(IdentifiersDbColumns.TABLE_NAME, PROJECTION_SET_IDENTIFIER, SELECTION_SET_IDENTIFIER, new String[] { key }, null);

                if (cursor.moveToFirst())
                {
                	if (null == value)
                	{
                		mProvider.remove(IdentifiersDbColumns.TABLE_NAME, String.format("%s = ?", IdentifiersDbColumns.KEY), new String[] { cursor.getString(cursor.getColumnIndexOrThrow(IdentifiersDbColumns.KEY)) }); //$NON-NLS-1$
                	}
                	else
                	{
                    	final ContentValues values = new ContentValues();
                    	values.put(IdentifiersDbColumns.KEY, key);
                    	values.put(IdentifiersDbColumns.VALUE, value); 
                    	mProvider.update(IdentifiersDbColumns.TABLE_NAME, values, SELECTION_SET_IDENTIFIER, new String[] { key }); 
                	}
                }
                else
                {
                	if (value != null)
                	{
                		final ContentValues values = new ContentValues();
                		values.put(IdentifiersDbColumns.KEY, key);
                		values.put(IdentifiersDbColumns.VALUE, value);                       
                		mProvider.insert(IdentifiersDbColumns.TABLE_NAME, values);
                	}
                }
                
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        /* package */void setPushRegistrationId(final String pushRegId)
        {
            final ContentValues values = new ContentValues();
            values.put(InfoDbColumns.REGISTRATION_ID, pushRegId == null ? "" : pushRegId);
            values.put(InfoDbColumns.REGISTRATION_VERSION, DatapointHelper.getAppVersion(mContext));
            mProvider.update(InfoDbColumns.TABLE_NAME, values, null, null);
        }
        
        /**
         * Projection for {@link #conditionallyAddFlowEvent()}.
         */
        private static final String[] PROJECTION_FLOW_EVENTS = new String[]
            { EventsDbColumns._ID };

        /**
         * Selection for {@link #conditionallyAddFlowEvent()}.
         */
        private static final String SELECTION_FLOW_EVENTS = String.format("%s = ?", EventsDbColumns.EVENT_NAME); //$NON-NLS-1$

        /**
         * Selection arguments for {@link #SELECTION_FLOW_EVENTS} in {@link #conditionallyAddFlowEvent()}.
         */
        private static final String[] SELECTION_ARGS_FLOW_EVENTS = new String[]
            { FLOW_EVENT };

        /**
         * Projection for {@link #conditionallyAddFlowEvent()}.
         */
        private static final String[] PROJECTION_FLOW_BLOBS = new String[]
            { UploadBlobEventsDbColumns.EVENTS_KEY_REF };

        /**
         * Conditionally adds a flow event if no flow event exists in the current upload blob.
         */
        private void conditionallyAddFlowEvent()
        {
            /*
             * Creating a flow "event" is required to act as a placeholder so that the uploader will know that an upload needs to
             * occur. A flow event should only be created if there isn't already a flow event that hasn't been associated with an
             * upload blob.
             */
            boolean foundUnassociatedFlowEvent = false;

            Cursor eventsCursor = null;
            Cursor blob_eventsCursor = null;
            try
            {
                eventsCursor = mProvider.query(EventsDbColumns.TABLE_NAME, PROJECTION_FLOW_EVENTS, SELECTION_FLOW_EVENTS, SELECTION_ARGS_FLOW_EVENTS, EVENTS_SORT_ORDER);

                blob_eventsCursor = mProvider.query(UploadBlobEventsDbColumns.TABLE_NAME, PROJECTION_FLOW_BLOBS, null, null, UPLOAD_BLOBS_EVENTS_SORT_ORDER);

                final CursorJoiner joiner = new CursorJoiner(eventsCursor, PROJECTION_FLOW_EVENTS, blob_eventsCursor, PROJECTION_FLOW_BLOBS);
                for (final CursorJoiner.Result joinerResult : joiner)
                {
                    switch (joinerResult)
                    {
                        case LEFT:
                        {
                            foundUnassociatedFlowEvent = true;
                            break;
                        }
                        case BOTH:
                            break;
                        case RIGHT:
                            break;
                    }
                }
            }
            finally
            {
                if (null != eventsCursor)
                {
                    eventsCursor.close();
                    eventsCursor = null;
                }

                if (null != blob_eventsCursor)
                {
                    blob_eventsCursor.close();
                    blob_eventsCursor = null;
                }
            }

            if (!foundUnassociatedFlowEvent)
            {
                tagEvent(FLOW_EVENT, null);
            }
        }

        /**
         * Projection for {@link #preUploadBuildBlobs(LocalyticsProvider)}.
         */
        private static final String[] PROJECTION_UPLOAD_EVENTS = new String[]
            {
                EventsDbColumns._ID,
                EventsDbColumns.EVENT_NAME,
                EventsDbColumns.WALL_TIME };

        /**
         * Projection for {@link #preUploadBuildBlobs(LocalyticsProvider)}.
         */
        private static final String[] PROJECTION_UPLOAD_BLOBS = new String[]
            { UploadBlobEventsDbColumns.EVENTS_KEY_REF };

        /**
         * Projection for {@link #preUploadBuildBlobs(LocalyticsProvider)}.
         */
        private static final String SELECTION_UPLOAD_NULL_BLOBS = String.format("%s IS NULL", EventHistoryDbColumns.PROCESSED_IN_BLOB); //$NON-NLS-1$

        /**
         * Columns to join in {@link #preUploadBuildBlobs(LocalyticsProvider)}.
         */
        private static final String[] JOINER_ARG_UPLOAD_EVENTS_COLUMNS = new String[]
            { EventsDbColumns._ID };

        /**
         * Builds upload blobs for all events.
         *
         * @param provider Instance of {@link LocalyticsProvider}. Cannot be null.
         * @effects Mutates the database by creating a new upload blob for all events that are unassociated at the time this
         *          method is called.
         */
        /* package */static void preUploadBuildBlobs(final LocalyticsProvider provider)
        {
            /*
             * Group all events that aren't part of an upload blob into a new blob. While this process is a linear algorithm that
             * requires scanning two database tables, the performance won't be a problem for two reasons: 1. This process happens
             * frequently so the number of events to group will always be low. 2. There is a maximum number of events, keeping the
             * overall size low. Note that close events that are younger than SESSION_EXPIRATION will be skipped to allow session
             * reconnects.
             */

            // temporary set of event ids that aren't in a blob
            final Set<Long> eventIds = new HashSet<Long>();

            Cursor eventsCursor = null;
            Cursor blob_eventsCursor = null;
            try
            {
                eventsCursor = provider.query(EventsDbColumns.TABLE_NAME, PROJECTION_UPLOAD_EVENTS, null, null, EVENTS_SORT_ORDER);

                blob_eventsCursor = provider.query(UploadBlobEventsDbColumns.TABLE_NAME, PROJECTION_UPLOAD_BLOBS, null, null, UPLOAD_BLOBS_EVENTS_SORT_ORDER);

                final int idColumn = eventsCursor.getColumnIndexOrThrow(EventsDbColumns._ID);
                final CursorJoiner joiner = new CursorJoiner(eventsCursor, JOINER_ARG_UPLOAD_EVENTS_COLUMNS, blob_eventsCursor, PROJECTION_UPLOAD_BLOBS);
                for (final CursorJoiner.Result joinerResult : joiner)
                {
                    switch (joinerResult)
                    {
                        case LEFT:
                        {
                            if (CLOSE_EVENT.equals(eventsCursor.getString(eventsCursor.getColumnIndexOrThrow(EventsDbColumns.EVENT_NAME))))
                            {
                                if (System.currentTimeMillis() - eventsCursor.getLong(eventsCursor.getColumnIndexOrThrow(EventsDbColumns.WALL_TIME)) < Constants.SESSION_EXPIRATION)
                                {
                                    break;
                                }
                            }
                            eventIds.add(Long.valueOf(eventsCursor.getLong(idColumn)));
                            break;
                        }
                        case BOTH:
                            break;
                        case RIGHT:
                            break;
                    }
                }
            }
            finally
            {
                if (null != eventsCursor)
                {
                    eventsCursor.close();
                    eventsCursor = null;
                }

                if (null != blob_eventsCursor)
                {
                    blob_eventsCursor.close();
                    blob_eventsCursor = null;
                }
            }

            if (eventIds.size() > 0)
            {
                // reusable object
                final ContentValues values = new ContentValues();

                final Long blobId;
                {
                    values.put(UploadBlobsDbColumns.UUID, UUID.randomUUID().toString());
                    blobId = Long.valueOf(provider.insert(UploadBlobsDbColumns.TABLE_NAME, values));
                    values.clear();
                }

                for (final Long x : eventIds)
                {
                    values.put(UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF, blobId);
                    values.put(UploadBlobEventsDbColumns.EVENTS_KEY_REF, x);

                    provider.insert(UploadBlobEventsDbColumns.TABLE_NAME, values);

                    values.clear();
                }

                values.put(EventHistoryDbColumns.PROCESSED_IN_BLOB, blobId);
                provider.update(EventHistoryDbColumns.TABLE_NAME, values, SELECTION_UPLOAD_NULL_BLOBS, null);
                values.clear();
            }
        }

        /**
         * Initiate upload of all session data currently stored on disk.
         * <p>
         * This method must only be called after {@link #init()} is called. The session does not need to be open for an upload to
         * occur.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_UPLOAD} to the Handler.
         *
         * @param callback An optional callback to perform once the upload completes. May be null for no callback.
         * @see #MESSAGE_UPLOAD
         */
        /* package */void upload(final Runnable callback)
        {
            if (sIsUploadingMap.get(mApiKey).booleanValue())
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.d(Constants.LOG_TAG, "Already uploading"); //$NON-NLS-1$
                }

                mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.MESSAGE_RETRY_UPLOAD_REQUEST, callback));
                return;
            }

            try
            {
                preUploadBuildBlobs(mProvider);

                sIsUploadingMap.put(mApiKey, Boolean.TRUE);
                mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.MESSAGE_UPLOAD, callback));
            }
            catch (final Exception e)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Error occurred during upload", e); //$NON-NLS-1$
                }

                sIsUploadingMap.put(mApiKey, Boolean.FALSE);

                // Notify the caller the upload is "complete"
                if (null != callback)
                {
                    /*
                     * Note that a new thread is created for the callback. This ensures that client code can't affect the
                     * performance of the SessionHandler's thread.
                     */
                    new Thread(callback, UploadHandler.UPLOAD_CALLBACK_THREAD_NAME).start();
                }
            }
        }

        /**
         * Projection for {@link #isOptedOut(LocalyticsProvider, String)}.
         */
        private static final String[] PROJECTION_IS_OPTED_OUT = new String[]
            { ApiKeysDbColumns.OPT_OUT };

        /**
         * Selection for {@link #isOptedOut(LocalyticsProvider, String)}.
         * <p>
         * The selection argument is the {@link ApiKeysDbColumns#API_KEY}.
         */
        private static final String SELECTION_IS_OPTED_OUT = String.format("%s = ?", ApiKeysDbColumns.API_KEY); //$NON-NLS-1$

        /**
         * @param provider Instance of {@link LocalyticsProvider}. Cannot be null.
         * @param apiKey Api key to test whether it is opted out. Cannot be null.
         * @return true if data collection has been opted out. Returns false if data collection is opted-in or if {@code apiKey}
         *         doesn't exist in the database.
         * @throws IllegalArgumentException if {@code provider} is null.
         * @throws IllegalArgumentException if {@code apiKey} is null.
         */
        /* package */static boolean isOptedOut(final LocalyticsProvider provider, final String apiKey)
        {
            if (Constants.IS_PARAMETER_CHECKING_ENABLED)
            {
                if (null == provider)
                {
                    throw new IllegalArgumentException("provider cannot be null"); //$NON-NLS-1$
                }

                if (null == apiKey)
                {
                    throw new IllegalArgumentException("apiKey cannot be null"); //$NON-NLS-1$
                }
            }

            Cursor cursor = null;
            try
            {
                cursor = provider.query(ApiKeysDbColumns.TABLE_NAME, PROJECTION_IS_OPTED_OUT, SELECTION_IS_OPTED_OUT, new String[]
                    { apiKey }, null);

                if (cursor.moveToFirst())
                {
                    return cursor.getInt(cursor.getColumnIndexOrThrow(ApiKeysDbColumns.OPT_OUT)) != 0;
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            return false;
        }
    }

    /**
     * Helper object to the {@link SessionHandler} which helps process upload requests.
     */
    /* package */static final class UploadHandler extends Handler
    {

        /**
         * Thread name that the upload callback runnable is executed on.
         */
        private static final String UPLOAD_CALLBACK_THREAD_NAME = "upload_callback"; //$NON-NLS-1$

        /**
         * Localytics upload URL for HTTP, as a format string that contains a format for the API key.
         */
        private final static String ANALYTICS_URL_HTTP = "http://analytics.localytics.com/api/v2/applications/%s/uploads"; //$NON-NLS-1$

        /**
         * Localytics upload URL for HTTPS
         */
        private final static String ANALYTICS_URL_HTTPS = "https://analytics.localytics.com/api/v2/uploads"; //$NON-NLS-1$        
        
        /**
         * Handler message to upload all data collected so far
         * <p>
         * {@link Message#obj} is a {@code Runnable} to execute when upload is complete. The thread that this runnable will
         * executed on is undefined.
         */
        public static final int MESSAGE_UPLOAD = 1;

        /**
         * Handler message indicating that there is a queued upload request. When this message is processed, this handler simply
         * forwards the request back to {@link LocalyticsSession#mSessionHandler} with {@link SessionHandler#MESSAGE_UPLOAD}.
         * <p>
         * {@link Message#obj} is a {@code Runnable} to execute when upload is complete. The thread that this runnable will
         * executed on is undefined.
         */
        public static final int MESSAGE_RETRY_UPLOAD_REQUEST = 2;

        /**
         * Reference to the Localytics database
         */
        protected final LocalyticsProvider mProvider;

        /**
         * Application context
         */
        private final Context mContext;

        /**
         * The Localytics API key
         */
        private final String mApiKey;
        
        /**
         * The Localytics Install ID
         */
        private final String mInstallId;
        
        /**
         * Parent session handler to notify when an upload completes.
         */
        private final Handler mSessionHandler;

        /**
         * Constructs a new Handler that runs on {@code looper}.
         * <p>
         * Note: This constructor may perform disk access.
         *
         * @param context Application context. Cannot be null.
         * @param sessionHandler Parent {@link SessionHandler} object to notify when uploads are completed. Cannot be null.
         * @param apiKey Localytics API key. Cannot be null.
         * @param installId Localytics install ID.
         * @param looper to run the Handler on. Cannot be null.
         */
        public UploadHandler(final Context context, final Handler sessionHandler, final String apiKey, final String installId, final Looper looper)
        {
            super(looper);

            mContext = context;
            mProvider = LocalyticsProvider.getInstance(context, apiKey);
            mSessionHandler = sessionHandler;
            mApiKey = apiKey;
            mInstallId = installId;
        }

        @Override
        public void handleMessage(final Message msg)
        {
            try
            {
                super.handleMessage(msg);

                switch (msg.what)
                {
                    case MESSAGE_UPLOAD:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "UploadHandler received MESSAGE_UPLOAD"); //$NON-NLS-1$
                        }

                        /*
                         * Note that callback may be null
                         */
                        final Runnable callback = (Runnable) msg.obj;

                        try
                        {                            
                            final List<JSONObject> toUpload = convertDatabaseToJson(mContext, mProvider, mApiKey);

                            if (!toUpload.isEmpty())
                            {
                                final StringBuilder builder = new StringBuilder();
                                for (final JSONObject json : toUpload)
                                {
                                    builder.append(json.toString());
                                    builder.append('\n');
                                }
                                
                                String apiKey = mApiKey;
                                String rollupKey = DatapointHelper.getLocalyticsRollupKeyOrNull(mContext);          
                                if (rollupKey != null && !TextUtils.isEmpty(rollupKey))
                                {
                                	apiKey = rollupKey;
                                }
                                                                                                                     
                                if (uploadSessions(Constants.USE_HTTPS ? ANALYTICS_URL_HTTPS : String.format(ANALYTICS_URL_HTTP, apiKey), builder.toString(), mInstallId, apiKey))
                                {
                                    mProvider.runBatchTransaction(new Runnable()
                                    {
                                        public void run()
                                        {
                                            deleteBlobsAndSessions(mProvider);
                                        }
                                    });
                                }
                            }
                        }
                        finally
                        {
                            if (null != callback)
                            {
                                /*
                                 * Execute the callback on a separate thread, to avoid exposing this thread to the client of the
                                 * library
                                 */
                                new Thread(callback, UPLOAD_CALLBACK_THREAD_NAME).start();
                            }

                            mSessionHandler.sendEmptyMessage(SessionHandler.MESSAGE_UPLOAD_CALLBACK);
                        }
                        break;
                    }
                    case MESSAGE_RETRY_UPLOAD_REQUEST:
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.d(Constants.LOG_TAG, "Received MESSAGE_RETRY_UPLOAD_REQUEST"); //$NON-NLS-1$
                        }

                        mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, msg.obj));
                        break;
                    }
                    default:
                    {
                        /*
                         * This should never happen
                         */
                        throw new RuntimeException("Fell through switch statement"); //$NON-NLS-1$
                    }
                }
            }
            catch (final Exception e)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.e(Constants.LOG_TAG, "Localytics library threw an uncaught exception", e); //$NON-NLS-1$
                }

                if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * Uploads the post Body to the webservice
         *
         * @param url where {@code body} will be posted to. Cannot be null.
         * @param body upload body as a string. This should be a plain old string. Cannot be null.
         * @return True on success, false on failure.
         */
		/* package */static boolean uploadSessions(final String url, final String body, final String installId, final String apiKey)
        {
            if (Constants.IS_PARAMETER_CHECKING_ENABLED)
            {
                if (null == url)
                {
                    throw new IllegalArgumentException("url cannot be null"); //$NON-NLS-1$
                }

                if (null == body)
                {
                    throw new IllegalArgumentException("body cannot be null"); //$NON-NLS-1$
                }
            }

            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, String.format("Upload body before compression is: %s", body)); //$NON-NLS-1$
            }

            /*
             * As per Google's documentation, use HttpURLConnection for API 9 and greater and DefaultHttpClient for API 8 and
             * lower. <http://android-developers.blogspot.com/2011/09/androids-http-clients.html>. HTTP library.
             *
             * Note: HTTP GZIP compression is explicitly disabled. Instead, the uploaded data is already GZIPPED before it is put
             * into the HTTP post.
             */
            if (DatapointHelper.getApiLevel() >= 9)
            {
                /*
                 * GZIP the data to upload
                 */
                byte[] data;
                {
                    GZIPOutputStream gos = null;
                    try
                    {
                        final byte[] originalBytes = body.getBytes("UTF-8"); //$NON-NLS-1$
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream(originalBytes.length);
                        gos = new GZIPOutputStream(baos);
                        gos.write(originalBytes);
                        gos.finish();
                        
                        /*
                         * KitKat throws an exception when you call flush
                         * https://code.google.com/p/android/issues/detail?id=62589
                         */
                        if (DatapointHelper.getApiLevel() < 19)
                        {
                        	gos.flush();
                        }

                        data = baos.toByteArray();
                    }
                    catch (final UnsupportedEncodingException e)
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.w(Constants.LOG_TAG, "UnsupportedEncodingException", e); //$NON-NLS-1$
                        }
                        return false;
                    }
                    catch (final IOException e)
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.w(Constants.LOG_TAG, "IOException", e); //$NON-NLS-1$
                        }
                        return false;
                    }
                    finally
                    {
                        if (null != gos)
                        {
                            try
                            {
                                gos.close();
                                gos = null;
                            }
                            catch (final IOException e)
                            {
                                if (Constants.IS_LOGGABLE)
                                {
                                    Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                                }

                                return false;
                            }
                        }
                    }
                }

                HttpURLConnection connection = null;
                try
                {
                    connection = (HttpURLConnection) new URL(url).openConnection();

                    connection.setDoOutput(true); // sets POST method implicitly
                    connection.setRequestProperty("Content-Type", "application/x-gzip"); //$NON-NLS-1$//$NON-NLS-2$
                    connection.setRequestProperty("Content-Encoding", "gzip"); //$NON-NLS-1$//$NON-NLS-2$
                    connection.setRequestProperty("x-upload-time",
                                                  Long.toString(Math.round((double) System.currentTimeMillis()
                                                                           / DateUtils.SECOND_IN_MILLIS))); //$NON-NLS-1$//$NON-NLS-2$
                    connection.setRequestProperty("x-install-id", installId); //$NON-NLS-1$
                    connection.setRequestProperty("x-app-id", apiKey); //$NON-NLS-1$
                    connection.setRequestProperty("x-client-version", Constants.LOCALYTICS_CLIENT_LIBRARY_VERSION); //$NON-NLS-1$
                    connection.setFixedLengthStreamingMode(data.length);

                    OutputStream stream = null;
                    try
                    {
                        stream = connection.getOutputStream();

                        stream.write(data);
                    }
                    finally
                    {
                        if (null != stream)
                        {
                            stream.flush();
                            stream.close();
                            stream = null;
                        }
                    }

                    final int responseCode = connection.getResponseCode();
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, String.format("Upload complete with status %d", Integer.valueOf(responseCode))); //$NON-NLS-1$
                    }

                    /*
                     * 5xx status codes indicate a server error, so upload should be reattempted
                     */
                    if (responseCode >= 500 && responseCode <= 599)
                    {
                        return false;
                    }
                }
                catch (final MalformedURLException e)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "ClientProtocolException", e); //$NON-NLS-1$
                    }

                    return false;
                }
                catch (final IOException e)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "ClientProtocolException", e); //$NON-NLS-1$
                    }

                    return false;
                }

                finally
                {
                    if (null != connection)
                    {
                        connection.disconnect();
                        connection = null;
                    }
                }
            }
            else
            {
                /*
                 * Note: DefaultHttpClient appears to sometimes cause an OutOfMemory error. Although we've seen exceptions from
                 * the wild, it isn't clear whether this is due to a bug in DefaultHttpClient or just a random error that has
                 * occurred once or twice due to buggy devices.
                 */
                final DefaultHttpClient client = new DefaultHttpClient();
                final HttpPost method = new HttpPost(url);
                method.addHeader("Content-Type", "application/x-gzip"); //$NON-NLS-1$ //$NON-NLS-2$
                method.addHeader("Content-Encoding", "gzip"); //$NON-NLS-1$//$NON-NLS-2$
                method.addHeader("x-upload-time",
                                 Long.toString(Math.round((double) System.currentTimeMillis()
                                                          / DateUtils.SECOND_IN_MILLIS))); //$NON-NLS-1$//$NON-NLS-2$
                method.addHeader("x-install-id", installId); //$NON-NLS-1$
                method.addHeader("x-app-id", apiKey); //$NON-NLS-1$
                method.addHeader("x-client-version", Constants.LOCALYTICS_CLIENT_LIBRARY_VERSION); //$NON-NLS-1$
                
                GZIPOutputStream gos = null;
                try
                {
                    final byte[] originalBytes = body.getBytes("UTF-8"); //$NON-NLS-1$
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream(originalBytes.length);
                    gos = new GZIPOutputStream(baos);
                    gos.write(originalBytes);
                    gos.finish();
                    gos.flush();

                    final ByteArrayEntity postBody = new ByteArrayEntity(baos.toByteArray());
                    method.setEntity(postBody);

                    final HttpResponse response = client.execute(method);

                    final StatusLine status = response.getStatusLine();
                    final int statusCode = status.getStatusCode();
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, String.format("Upload complete with status %d", Integer.valueOf(statusCode))); //$NON-NLS-1$
                    }

                    /*
                     * 5xx status codes indicate a server error, so upload should be reattempted
                     */
                    if (statusCode >= 500 && statusCode <= 599)
                    {
                        return false;
                    }
                }
                catch (final UnsupportedEncodingException e)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "UnsupportedEncodingException", e); //$NON-NLS-1$
                    }
                    return false;
                }
                catch (final ClientProtocolException e)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "ClientProtocolException", e); //$NON-NLS-1$
                    }
                    return false;
                }
                catch (final IOException e)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "IOException", e); //$NON-NLS-1$
                    }
                    return false;
                }
                finally
                {
                    if (null != gos)
                    {
                        try
                        {
                            gos.close();
                            gos = null;
                        }
                        catch (final IOException e)
                        {
                            if (Constants.IS_LOGGABLE)
                            {
                                Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                            }
                        }
                    }
                }
            }

            return true;
        }
        
        /**
         * Helper that converts blobs in the database into a JSON representation for upload.
         *
         * @return A list of JSON objects to upload to the server
         */
        /* package */static List<JSONObject> convertDatabaseToJson(final Context context, final LocalyticsProvider provider, final String apiKey)
        {
            final List<JSONObject> result = new LinkedList<JSONObject>();
            Cursor cursor = null;
            try
            {
                cursor = provider.query(UploadBlobsDbColumns.TABLE_NAME, null, null, null, null);

                final long creationTime = getApiKeyCreationTime(provider, apiKey);

                final int idColumn = cursor.getColumnIndexOrThrow(UploadBlobsDbColumns._ID);
                final int uuidColumn = cursor.getColumnIndexOrThrow(UploadBlobsDbColumns.UUID);
                while (cursor.moveToNext())
                {
                    try
                    {
                        final JSONObject blobHeader = new JSONObject();

                        blobHeader.put(JsonObjects.BlobHeader.KEY_DATA_TYPE, BlobHeader.VALUE_DATA_TYPE);
                        blobHeader.put(JsonObjects.BlobHeader.KEY_PERSISTENT_STORAGE_CREATION_TIME_SECONDS, creationTime);
                        blobHeader.put(JsonObjects.BlobHeader.KEY_SEQUENCE_NUMBER, cursor.getLong(idColumn));
                        blobHeader.put(JsonObjects.BlobHeader.KEY_UNIQUE_ID, cursor.getString(uuidColumn));
                        blobHeader.put(JsonObjects.BlobHeader.KEY_ATTRIBUTES, getAttributesFromSession(provider, apiKey, getSessionIdForBlobId(provider, cursor.getLong(idColumn))));
                        
                        final JSONObject identifiers = getIdentifiers(provider);
                        if (null != identifiers)
                        {
                        	blobHeader.put(JsonObjects.BlobHeader.KEY_IDENTIFIERS, identifiers);
                        }
                        
                        result.add(blobHeader);
                        
                        if (Constants.IS_LOGGABLE)
                        {
                        	Log.w(Constants.LOG_TAG, result.toString());
                        }

                        Cursor blobEvents = null;
                        try
                        {
                            blobEvents = provider.query(UploadBlobEventsDbColumns.TABLE_NAME, new String[]
                                { UploadBlobEventsDbColumns.EVENTS_KEY_REF }, String.format("%s = ?", UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF), new String[] //$NON-NLS-1$
                                { Long.toString(cursor.getLong(idColumn)) }, UploadBlobEventsDbColumns.EVENTS_KEY_REF);

                            final int eventIdColumn = blobEvents.getColumnIndexOrThrow(UploadBlobEventsDbColumns.EVENTS_KEY_REF);
                            while (blobEvents.moveToNext())
                            {
                                result.add(convertEventToJson(provider, context, blobEvents.getLong(eventIdColumn), cursor.getLong(idColumn), apiKey));
                            }
                        }
                        finally
                        {
                            if (null != blobEvents)
                            {
                                blobEvents.close();
                            }
                        }
                    }
                    catch (final JSONException e)
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                        }
                    }
                }
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, String.format("JSON result is %s", result.toString())); //$NON-NLS-1$
            }

            return result;
        }

        /**
         * Deletes all blobs and sessions/events/attributes associated with those blobs.
         * <p>
         * This should be called after a successful upload completes.
         *
         * @param provider Localytics database provider. Cannot be null.
         */
        /* package */static void deleteBlobsAndSessions(final LocalyticsProvider provider)
        {
            /*
             * Deletion needs to occur in a specific order due to database constraints. Specifically, blobevents need to be
             * deleted first. Then blobs themselves can be deleted. Then attributes need to be deleted first. Then events. Then
             * sessions.
             */

            final LinkedList<Long> sessionsToDelete = new LinkedList<Long>();
            final HashSet<Long> blobsToDelete = new HashSet<Long>();

            Cursor blobEvents = null;
            try
            {
                blobEvents = provider.query(UploadBlobEventsDbColumns.TABLE_NAME, new String[]
                    {
                        UploadBlobEventsDbColumns._ID,
                        UploadBlobEventsDbColumns.EVENTS_KEY_REF,
                        UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF }, null, null, null);

                final int uploadBlobIdColumn = blobEvents.getColumnIndexOrThrow(UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF);
                final int blobEventIdColumn = blobEvents.getColumnIndexOrThrow(UploadBlobEventsDbColumns._ID);
                final int eventIdColumn = blobEvents.getColumnIndexOrThrow(UploadBlobEventsDbColumns.EVENTS_KEY_REF);
                while (blobEvents.moveToNext())
                {
                    final long blobId = blobEvents.getLong(uploadBlobIdColumn);
                    final long blobEventId = blobEvents.getLong(blobEventIdColumn);
                    final long eventId = blobEvents.getLong(eventIdColumn);

                    // delete the blobevent
                    provider.remove(UploadBlobEventsDbColumns.TABLE_NAME, String.format("%s = ?", UploadBlobEventsDbColumns._ID), new String[] { Long.toString(blobEventId) }); //$NON-NLS-1$

                    /*
                     * Add the blob to the list of blobs to be deleted
                     */
                    blobsToDelete.add(Long.valueOf(blobId));

                    // delete all attributes for the event
                    provider.remove(AttributesDbColumns.TABLE_NAME, String.format("%s = ?", AttributesDbColumns.EVENTS_KEY_REF), new String[] { Long.toString(eventId) }); //$NON-NLS-1$

                    /*
                     * Check to see if the event is a close event, indicating that the session is complete and can also be deleted
                     */
                    Cursor eventCursor = null;
                    try
                    {
                        eventCursor = provider.query(EventsDbColumns.TABLE_NAME, new String[]
                            { EventsDbColumns.SESSION_KEY_REF }, String.format("%s = ? AND %s = ?", EventsDbColumns._ID, EventsDbColumns.EVENT_NAME), new String[] //$NON-NLS-1$
                            {
                                Long.toString(eventId),
                                CLOSE_EVENT }, null);

                        if (eventCursor.moveToFirst())
                        {
                            final long sessionId = eventCursor.getLong(eventCursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF));

                            provider.remove(EventHistoryDbColumns.TABLE_NAME, String.format("%s = ?", EventHistoryDbColumns.SESSION_KEY_REF), new String[] //$NON-NLS-1$
                                { Long.toString(sessionId) });

                            sessionsToDelete.add(Long.valueOf(eventCursor.getLong(eventCursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF))));
                        }
                    }
                    finally
                    {
                        if (null != eventCursor)
                        {
                            eventCursor.close();
                            eventCursor = null;
                        }
                    }

                    // delete the event
                    provider.remove(EventsDbColumns.TABLE_NAME, String.format("%s = ?", EventsDbColumns._ID), new String[] { Long.toString(eventId) }); //$NON-NLS-1$
                }
            }
            finally
            {
                if (null != blobEvents)
                {
                    blobEvents.close();
                    blobEvents = null;
                }
            }

            // delete blobs
            for (final long x : blobsToDelete)
            {
                provider.remove(UploadBlobsDbColumns.TABLE_NAME, String.format("%s = ?", UploadBlobsDbColumns._ID), new String[] { Long.toString(x) }); //$NON-NLS-1$
            }

            // delete sessions
            for (final long x : sessionsToDelete)
            {
                provider.remove(SessionsDbColumns.TABLE_NAME, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(x) }); //$NON-NLS-1$
            }
        }
        
        /**
         * Gets the creation time for an API key.
         *
         * @param provider Localytics database provider. Cannot be null.
         * @param key Localytics API key. Cannot be null.
         * @return The time in seconds since the Unix Epoch when the API key entry was created in the database.
         * @throws RuntimeException if the API key entry doesn't exist in the database.
         */
        /* package */static long getApiKeyCreationTime(final LocalyticsProvider provider, final String key)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(ApiKeysDbColumns.TABLE_NAME, null, String.format("%s = ?", ApiKeysDbColumns.API_KEY), new String[] { key }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    return Math.round((float) cursor.getLong(cursor.getColumnIndexOrThrow(ApiKeysDbColumns.CREATED_TIME)) / DateUtils.SECOND_IN_MILLIS);
                }

                /*
                 * This should never happen
                 */
                throw new RuntimeException("API key entry couldn't be found"); //$NON-NLS-1$
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }
                

        /**
         * Helper method to generate the attributes object for a session
         *
         * @param provider Instance of the Localytics database provider. Cannot be null.
         * @param apiKey Localytics API key. Cannot be null.
         * @param sessionId The {@link SessionsDbColumns#_ID} of the session.
         * @return a JSONObject representation of the session attributes
         * @throws JSONException if a problem occurred converting the element to JSON.
         */
        /* package */static JSONObject getAttributesFromSession(final LocalyticsProvider provider, final String apiKey, final long sessionId) throws JSONException
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(SessionsDbColumns.TABLE_NAME, null, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(sessionId) }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    final JSONObject result = new JSONObject();
                    
                    // Sessions table
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_CLIENT_APP_VERSION, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.APP_VERSION)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DATA_CONNECTION, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.NETWORK_TYPE)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_ANDROID_ID_HASH, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_ANDROID_ID_HASH)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_COUNTRY, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_COUNTRY)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_MANUFACTURER, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_MANUFACTURER)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_MODEL, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_MODEL)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_OS_VERSION, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.ANDROID_VERSION)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_PLATFORM, JsonObjects.BlobHeader.Attributes.VALUE_PLATFORM);
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_SERIAL_HASH, cursor.isNull(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_SERIAL_NUMBER_HASH)) ? JSONObject.NULL
                            : cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_SERIAL_NUMBER_HASH)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_SDK_LEVEL, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.ANDROID_SDK)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALYTICS_API_KEY, apiKey);
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALYTICS_CLIENT_LIBRARY_VERSION, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.LOCALYTICS_LIBRARY_VERSION)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALYTICS_DATA_TYPE, JsonObjects.BlobHeader.Attributes.VALUE_DATA_TYPE);
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_CURRENT_TELEPHONY_ID, cursor.isNull(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_TELEPHONY_ID)) ? JSONObject.NULL
                            : cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_TELEPHONY_ID)));                    
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_CURRENT_ANDROID_ID, cursor.isNull(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_ANDROID_ID)) ? JSONObject.NULL
                            : cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_ANDROID_ID)));

                    // This would only be null after an upgrade from an earlier version of the Localytics library
                    final String installationID = cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.LOCALYTICS_INSTALLATION_ID));
                    if (null != installationID)
                    {
                        result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALYTICS_INSTALLATION_ID, installationID);
                    }
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALE_COUNTRY, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.LOCALE_COUNTRY)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALE_LANGUAGE, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.LOCALE_LANGUAGE)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_NETWORK_CARRIER, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.NETWORK_CARRIER)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_NETWORK_COUNTRY, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.NETWORK_COUNTRY)));
                                                    
                    // Info table
                    String fbAttribution = getStringFromAppInfo(provider, InfoDbColumns.FB_ATTRIBUTION);
                    if (null != fbAttribution)
                    {
                    	result.put(JsonObjects.BlobHeader.Attributes.KEY_FB_COOKIE, fbAttribution);
                    }
                    
                    String playAttribution = getStringFromAppInfo(provider, InfoDbColumns.PLAY_ATTRIBUTION);
                    if (null != playAttribution)
                    {
                    	result.put(JsonObjects.BlobHeader.Attributes.KEY_GOOGLE_PLAY_ATTRIBUTION, playAttribution);
                    }
                    
                    String registrationId = getStringFromAppInfo(provider, InfoDbColumns.REGISTRATION_ID);
                    if (null != registrationId)
                    {
                    	result.put(JsonObjects.BlobHeader.Attributes.KEY_PUSH_ID, registrationId);
                    }

                    String firstAndroidId = getStringFromAppInfo(provider, InfoDbColumns.FIRST_ANDROID_ID);
                    if (null != firstAndroidId)
                    {
                    	result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_ANDROID_ID, firstAndroidId);
                    }

                    String firstTelephonyId = getStringFromAppInfo(provider, InfoDbColumns.FIRST_TELEPHONY_ID);
                    if (null != firstTelephonyId)
                    {
                    	result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_TELEPHONY_ID, firstTelephonyId);
                    }                    
                    
                    String packageName = getStringFromAppInfo(provider, InfoDbColumns.PACKAGE_NAME);
                    if (null != packageName)
                    {
                    	result.put(JsonObjects.BlobHeader.Attributes.KEY_PACKAGE_NAME, packageName);
                    }
                    
                    return result;
                }

                throw new RuntimeException("No session exists"); //$NON-NLS-1$
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }
        
        /**
         * Helper method to generate the attributes object for a session
         *
         * @param provider Instance of the Localytics database provider. Cannot be null.
         * @param apiKey Localytics API key. Cannot be null.
         * @param sessionId The {@link SessionsDbColumns#_ID} of the session.
         * @return a JSONObject representation of the session attributes
         * @throws JSONException if a problem occurred converting the element to JSON.
         */
        /* package */static JSONObject getIdentifiers(final LocalyticsProvider provider) throws JSONException
        {
            Cursor cursor = null;
            try
            {            	
                cursor = provider.query(IdentifiersDbColumns.TABLE_NAME, null, null, null, null); //$NON-NLS-1$

                JSONObject result = null;
                
                while (cursor.moveToNext())
                {
                	if (null == result)
                	{
                		result = new JSONObject();
                	}
                	
                	result.put(cursor.getString(cursor.getColumnIndexOrThrow(IdentifiersDbColumns.KEY)), cursor.getString(cursor.getColumnIndexOrThrow(IdentifiersDbColumns.VALUE)));
                }
                
                return result;
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        /**
         * Converts an event into a JSON object.
         * <p>
         * There are three types of events: open, close, and application. Open and close events are Localytics events, while
         * application events are generated by the app. The return value of this method will vary based on the type of event that
         * is being converted.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param context Application context. Cannot be null.
         * @param eventId {@link EventsDbColumns#_ID} of the event to convert.
         * @param blobId {@link UploadBlobEventsDbColumns#_ID} of the upload blob that contains this event.
         * @param apiKey the Localytics API key. Cannot be null.
         * @return JSON representation of the event.
         * @throws JSONException if a problem occurred converting the element to JSON.
         */
        /* package */static JSONObject convertEventToJson(final LocalyticsProvider provider, final Context context, final long eventId, final long blobId, final String apiKey)
                                                                                                                                                                               throws JSONException
        {
            final JSONObject result = new JSONObject();

            Cursor cursor = null;

            try
            {
                cursor = provider.query(EventsDbColumns.TABLE_NAME, null, String.format("%s = ?", EventsDbColumns._ID), new String[] //$NON-NLS-1$
                    { Long.toString(eventId) }, EventsDbColumns._ID);

                if (cursor.moveToFirst())
                {
                    final String eventName = cursor.getString(cursor.getColumnIndexOrThrow(EventsDbColumns.EVENT_NAME));
                    final long sessionId = getSessionIdForEventId(provider, eventId);
                    final String sessionUuid = getSessionUuid(provider, sessionId);
                    final long sessionStartTime = getSessionStartTime(provider, sessionId);

                    if (OPEN_EVENT.equals(eventName))
                    {
                        result.put(JsonObjects.SessionOpen.KEY_DATA_TYPE, JsonObjects.SessionOpen.VALUE_DATA_TYPE);
                        result.put(JsonObjects.SessionOpen.KEY_WALL_TIME_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                / DateUtils.SECOND_IN_MILLIS));
                        result.put(JsonObjects.SessionOpen.KEY_EVENT_UUID, sessionUuid);

                        /*
                         * Both the database and the web service use 1-based indexing.
                         */
                        result.put(JsonObjects.SessionOpen.KEY_COUNT, sessionId);

                        /*
                         * Append lat/lng if it is available
                         */
                        if (false == cursor.isNull(cursor.getColumnIndexOrThrow(EventsDbColumns.LAT_NAME)) &&
                            false == cursor.isNull(cursor.getColumnIndexOrThrow(EventsDbColumns.LNG_NAME)))
                        {
                        	double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(EventsDbColumns.LAT_NAME));
                        	double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(EventsDbColumns.LNG_NAME));
                        	
                        	if (lat != 0 && lng != 0)
                        	{
                        		result.put(JsonObjects.SessionEvent.KEY_LATITUDE, lat);
                        		result.put(JsonObjects.SessionEvent.KEY_LONGITUDE, lng);
                        	}
                        }
                        
                        /*
                         * Get the custom dimensions from the attributes table
                         */
                        Cursor attributesCursor = null;
                        try
                        {
                            attributesCursor = provider.query(AttributesDbColumns.TABLE_NAME, null, String.format("%s = ?", AttributesDbColumns.EVENTS_KEY_REF), new String[] { Long.toString(eventId) }, null); //$NON-NLS-1$

                            final int keyColumn = attributesCursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_KEY);
                            final int valueColumn = attributesCursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_VALUE);
                            while (attributesCursor.moveToNext())
                            {
                                final String key = attributesCursor.getString(keyColumn);
                                final String value = attributesCursor.getString(valueColumn);

                                if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_1, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_2, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_3, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_4, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_5, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_6, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_7, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_8, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_9, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_10, value);
                                }
                            }
                        }
                        finally
                        {
                            if (null != attributesCursor)
                            {
                                attributesCursor.close();
                                attributesCursor = null;
                            }
                        }
                    }
                    else if (CLOSE_EVENT.equals(eventName))
                    {
                        result.put(JsonObjects.SessionClose.KEY_DATA_TYPE, JsonObjects.SessionClose.VALUE_DATA_TYPE);
                        result.put(JsonObjects.SessionClose.KEY_EVENT_UUID, cursor.getString(cursor.getColumnIndexOrThrow(EventsDbColumns.UUID)));
                        result.put(JsonObjects.SessionClose.KEY_SESSION_UUID, sessionUuid);
                        result.put(JsonObjects.SessionClose.KEY_SESSION_START_TIME, Math.round((double) sessionStartTime / DateUtils.SECOND_IN_MILLIS));
                        result.put(JsonObjects.SessionClose.KEY_WALL_TIME_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                / DateUtils.SECOND_IN_MILLIS));

                        /*
                         * length is a special case, as it depends on the start time embedded in the session table
                         */
                        Cursor sessionCursor = null;
                        try
                        {
                            sessionCursor = provider.query(SessionsDbColumns.TABLE_NAME, new String[]
                                { SessionsDbColumns.SESSION_START_WALL_TIME }, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(cursor.getLong(cursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF))) }, null); //$NON-NLS-1$

                            if (sessionCursor.moveToFirst())
                            {
                                result.put(JsonObjects.SessionClose.KEY_SESSION_LENGTH_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                        / DateUtils.SECOND_IN_MILLIS)
                                        - Math.round((double) sessionCursor.getLong(sessionCursor.getColumnIndexOrThrow(SessionsDbColumns.SESSION_START_WALL_TIME))
                                                / DateUtils.SECOND_IN_MILLIS));
                            }
                            else
                            {
                                // this should never happen
                                throw new RuntimeException("Session didn't exist"); //$NON-NLS-1$
                            }
                        }
                        finally
                        {
                            if (null != sessionCursor)
                            {
                                sessionCursor.close();
                                sessionCursor = null;
                            }
                        }

                        /*
                         * The close also contains a special case element for the screens history
                         */
                        Cursor eventHistoryCursor = null;
                        try
                        {
                            eventHistoryCursor = provider.query(EventHistoryDbColumns.TABLE_NAME, new String[]
                                { EventHistoryDbColumns.NAME }, String.format("%s = ? AND %s = ?", EventHistoryDbColumns.SESSION_KEY_REF, EventHistoryDbColumns.TYPE), new String[] { Long.toString(sessionId), Integer.toString(EventHistoryDbColumns.TYPE_SCREEN) }, EventHistoryDbColumns._ID); //$NON-NLS-1$

                            final JSONArray screens = new JSONArray();
                            while (eventHistoryCursor.moveToNext())
                            {
                                screens.put(eventHistoryCursor.getString(eventHistoryCursor.getColumnIndexOrThrow(EventHistoryDbColumns.NAME)));
                            }

                            if (screens.length() > 0)
                            {
                                result.put(JsonObjects.SessionClose.KEY_FLOW_ARRAY, screens);
                            }
                        }
                        finally
                        {
                            if (null != eventHistoryCursor)
                            {
                                eventHistoryCursor.close();
                                eventHistoryCursor = null;
                            }
                        }

                        /*
                         * Append lat/lng if it is available
                         */
                        if (false == cursor.isNull(cursor.getColumnIndexOrThrow(EventsDbColumns.LAT_NAME)) &&
                            false == cursor.isNull(cursor.getColumnIndexOrThrow(EventsDbColumns.LNG_NAME)))
                        {
                        	double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(EventsDbColumns.LAT_NAME));
                        	double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(EventsDbColumns.LNG_NAME));
                        	
                        	if (lat != 0 && lng != 0)
                        	{
                        		result.put(JsonObjects.SessionEvent.KEY_LATITUDE, lat);
                        		result.put(JsonObjects.SessionEvent.KEY_LONGITUDE, lng);
                        	}
                        }
                        
                        /*
                         * Get the custom dimensions from the attributes table
                         */
                        Cursor attributesCursor = null;
                        try
                        {
                            attributesCursor = provider.query(AttributesDbColumns.TABLE_NAME, null, String.format("%s = ?", AttributesDbColumns.EVENTS_KEY_REF), new String[] { Long.toString(eventId) }, null); //$NON-NLS-1$

                            final int keyColumn = attributesCursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_KEY);
                            final int valueColumn = attributesCursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_VALUE);
                            while (attributesCursor.moveToNext())
                            {
                                final String key = attributesCursor.getString(keyColumn);
                                final String value = attributesCursor.getString(valueColumn);

                                if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_1, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_2, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_3, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_4, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_5, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_6, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_7, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_8, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_9, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_10, value);
                                }                                
                            }
                        }
                        finally
                        {
                            if (null != attributesCursor)
                            {
                                attributesCursor.close();
                                attributesCursor = null;
                            }
                        }
                    }
                    else if (OPT_IN_EVENT.equals(eventName) || OPT_OUT_EVENT.equals(eventName))
                    {
                        result.put(JsonObjects.OptEvent.KEY_DATA_TYPE, JsonObjects.OptEvent.VALUE_DATA_TYPE);
                        result.put(JsonObjects.OptEvent.KEY_API_KEY, apiKey);
                        result.put(JsonObjects.OptEvent.KEY_OPT, OPT_OUT_EVENT.equals(eventName) ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
                        result.put(JsonObjects.OptEvent.KEY_WALL_TIME_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                / DateUtils.SECOND_IN_MILLIS));
                    }
                    else if (FLOW_EVENT.equals(eventName))
                    {
                        result.put(JsonObjects.EventFlow.KEY_DATA_TYPE, JsonObjects.EventFlow.VALUE_DATA_TYPE);
                        result.put(JsonObjects.EventFlow.KEY_EVENT_UUID, cursor.getString(cursor.getColumnIndexOrThrow(EventsDbColumns.UUID)));
                        result.put(JsonObjects.EventFlow.KEY_SESSION_START_TIME, Math.round((double) sessionStartTime / DateUtils.SECOND_IN_MILLIS));

                        /*
                         * Need to generate two objects: the old flow events and the new flow events
                         */

                        /*
                         * Default sort order is ascending by _ID, so these will be sorted chronologically.
                         */
                        Cursor eventHistoryCursor = null;
                        try
                        {
                            eventHistoryCursor = provider.query(EventHistoryDbColumns.TABLE_NAME, new String[]
                                {
                                    EventHistoryDbColumns.TYPE,
                                    EventHistoryDbColumns.PROCESSED_IN_BLOB,
                                    EventHistoryDbColumns.NAME }, String.format("%s = ? AND %s <= ?", EventHistoryDbColumns.SESSION_KEY_REF, EventHistoryDbColumns.PROCESSED_IN_BLOB), new String[] { Long.toString(sessionId), Long.toString(blobId) }, EventHistoryDbColumns._ID); //$NON-NLS-1$

                            final JSONArray newScreens = new JSONArray();
                            final JSONArray oldScreens = new JSONArray();
                            while (eventHistoryCursor.moveToNext())
                            {
                                final String name = eventHistoryCursor.getString(eventHistoryCursor.getColumnIndexOrThrow(EventHistoryDbColumns.NAME));
                                final String type;
                                if (EventHistoryDbColumns.TYPE_EVENT == eventHistoryCursor.getInt(eventHistoryCursor.getColumnIndexOrThrow(EventHistoryDbColumns.TYPE)))
                                {
                                    type = JsonObjects.EventFlow.Element.TYPE_EVENT;
                                }
                                else
                                {
                                    type = JsonObjects.EventFlow.Element.TYPE_SCREEN;
                                }

                                if (blobId == eventHistoryCursor.getLong(eventHistoryCursor.getColumnIndexOrThrow(EventHistoryDbColumns.PROCESSED_IN_BLOB)))
                                {
                                    newScreens.put(new JSONObject().put(type, name));
                                }
                                else
                                {
                                    oldScreens.put(new JSONObject().put(type, name));
                                }
                            }

                            result.put(JsonObjects.EventFlow.KEY_FLOW_NEW, newScreens);
                            result.put(JsonObjects.EventFlow.KEY_FLOW_OLD, oldScreens);
                        }
                        finally
                        {
                            if (null != eventHistoryCursor)
                            {
                                eventHistoryCursor.close();
                                eventHistoryCursor = null;
                            }
                        }
                    }
                    else
                    {
                        /*
                         * This is a normal application event
                         */

                        result.put(JsonObjects.SessionEvent.KEY_DATA_TYPE, JsonObjects.SessionEvent.VALUE_DATA_TYPE);
                        result.put(JsonObjects.SessionEvent.KEY_WALL_TIME_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                / DateUtils.SECOND_IN_MILLIS));
                        result.put(JsonObjects.SessionEvent.KEY_EVENT_UUID, cursor.getString(cursor.getColumnIndexOrThrow(EventsDbColumns.UUID)));
                        result.put(JsonObjects.SessionEvent.KEY_SESSION_UUID, sessionUuid);
                        result.put(JsonObjects.SessionEvent.KEY_NAME, eventName.substring(context.getPackageName().length() + 1, eventName.length()));

                        /*
                         * Add customer value increase if non-zero 
                         */                        
                        long clv = cursor.getLong(cursor.getColumnIndex(EventsDbColumns.CLV_INCREASE));
                        if (clv != 0)
                        {
                        	result.put(JsonObjects.SessionEvent.KEY_CUSTOMER_VALUE_INCREASE, clv);
                        }
                        
                        /*
                         * Append lat/lng if it is available
                         */
                        if (false == cursor.isNull(cursor.getColumnIndexOrThrow(EventsDbColumns.LAT_NAME)) &&
                            false == cursor.isNull(cursor.getColumnIndexOrThrow(EventsDbColumns.LNG_NAME)))
                        {
                        	double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(EventsDbColumns.LAT_NAME));
                        	double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(EventsDbColumns.LNG_NAME));
                        	
                        	if (lat != 0 && lng != 0)
                        	{
                        		result.put(JsonObjects.SessionEvent.KEY_LATITUDE, lat);
                        		result.put(JsonObjects.SessionEvent.KEY_LONGITUDE, lng);
                        	}
                        }
                        
                        /*
                         * Get the custom dimensions from the attributes table
                         */
                        Cursor attributesCursor = null;
                        try
                        {
                            attributesCursor = provider.query(AttributesDbColumns.TABLE_NAME, null, String.format("%s = ?", AttributesDbColumns.EVENTS_KEY_REF), new String[] { Long.toString(eventId) }, null); //$NON-NLS-1$

                            final int keyColumn = attributesCursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_KEY);
                            final int valueColumn = attributesCursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_VALUE);
                            while (attributesCursor.moveToNext())
                            {
                                final String key = attributesCursor.getString(keyColumn);
                                final String value = attributesCursor.getString(valueColumn);

                                if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_1, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_2, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_3, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_4, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_5, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_6, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_7, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_8, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_9, value);
                                }
                                else if (AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10.equals(key))
                                {
                                    result.put(JsonObjects.SessionOpen.KEY_CUSTOM_DIMENSION_10, value);
                                }
                            }
                        }
                        finally
                        {
                            if (null != attributesCursor)
                            {
                                attributesCursor.close();
                                attributesCursor = null;
                            }
                        }

                        final JSONObject attributes = convertAttributesToJson(provider, context, eventId);

                        if (null != attributes)
                        {
                            result.put(JsonObjects.SessionEvent.KEY_ATTRIBUTES, attributes);
                        }
                    }
                }
                else
                {
                    /*
                     * This should never happen
                     */
                    throw new RuntimeException();
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            return result;
        }

        /**
         * Private helper to get the {@link SessionsDbColumns#_ID} for a given {@link EventsDbColumns#_ID}.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param eventId {@link EventsDbColumns#_ID} of the event to look up
         * @return The {@link SessionsDbColumns#_ID} of the session that owns the event.
         */
        /* package */static long getSessionIdForEventId(final LocalyticsProvider provider, final long eventId)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(EventsDbColumns.TABLE_NAME, new String[]
                    { EventsDbColumns.SESSION_KEY_REF }, String.format("%s = ?", EventsDbColumns._ID), new String[] { Long.toString(eventId) }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF));
                }

                /*
                 * This should never happen
                 */
                throw new RuntimeException();
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        /**
         * Private helper to get the {@link SessionsDbColumns#UUID} for a given {@link SessionsDbColumns#_ID}.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param sessionId {@link SessionsDbColumns#_ID} of the event to look up
         * @return The {@link SessionsDbColumns#UUID} of the session.
         */
        /* package */static String getSessionUuid(final LocalyticsProvider provider, final long sessionId)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(SessionsDbColumns.TABLE_NAME, new String[]
                    { SessionsDbColumns.UUID }, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(sessionId) }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    return cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.UUID));
                }

                /*
                 * This should never happen
                 */
                throw new RuntimeException();
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }
        
        /**
         * Private helper to get a column value from the InfoDb table
         *
         * @param provider Localytics database provider. Cannot be null.
         * @param Database key. Cannot be null.
         * @return The requested string
         */
        /* package */static String getStringFromAppInfo(final LocalyticsProvider provider, final String key)
        {
            Cursor cursor = null;
            
            try
            {
                cursor = provider.query(InfoDbColumns.TABLE_NAME, null, null, null, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                	return cursor.getString(cursor.getColumnIndexOrThrow(key));
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
            
            return null;
        }
        

        /**
         * Private helper to get the {@link SessionsDbColumns#SESSION_START_WALL_TIME} for a given {@link SessionsDbColumns#_ID}.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param sessionId {@link SessionsDbColumns#_ID} of the event to look up
         * @return The {@link SessionsDbColumns#SESSION_START_WALL_TIME} of the session.
         */
        /* package */static long getSessionStartTime(final LocalyticsProvider provider, final long sessionId)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(SessionsDbColumns.TABLE_NAME, new String[]
                    { SessionsDbColumns.SESSION_START_WALL_TIME }, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(sessionId) }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(SessionsDbColumns.SESSION_START_WALL_TIME));
                }

                /*
                 * This should never happen
                 */
                throw new RuntimeException();
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        /**
         * Private helper to convert an event's attributes into a {@link JSONObject} representation.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param context Application context. Cannot be null.
         * @param eventId {@link EventsDbColumns#_ID} of the event whose attributes are to be loaded.
         * @return {@link JSONObject} representing the attributes of the event. The order of attributes is undefined and may
         *         change from call to call of this method. If the event has no attributes, returns null.
         * @throws JSONException if an error occurs converting the attributes to JSON
         */
        /* package */static JSONObject convertAttributesToJson(final LocalyticsProvider provider, final Context context, final long eventId) throws JSONException
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(AttributesDbColumns.TABLE_NAME, null, String.format("%s = ? AND %s != ? AND %s != ? AND %s != ? AND %s != ? AND %s != ? AND %s != ? AND %s != ? AND %s != ? AND %s != ? AND %s != ?", AttributesDbColumns.EVENTS_KEY_REF, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_KEY), new String[] { Long.toString(eventId), AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_1, AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_2, AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_3,  AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_4, AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_5, AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_6, AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_7, AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_8, AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_9, AttributesDbColumns.ATTRIBUTE_CUSTOM_DIMENSION_10 }, null); //$NON-NLS-1$

                if (cursor.getCount() == 0)
                {
                    return null;
                }

                final JSONObject attributes = new JSONObject();

                final int keyColumn = cursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_KEY);
                final int valueColumn = cursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_VALUE);
                while (cursor.moveToNext())
                {
                    final String key = cursor.getString(keyColumn);
                    final String value = cursor.getString(valueColumn);

                    attributes.put(key.substring(context.getPackageName().length() + 1, key.length()), value);
                }

                return attributes;
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        /**
         * Given an id of an upload blob, get the session id associated with that blob.
         *
         * @param blobId {@link UploadBlobsDbColumns#_ID} of the upload blob.
         * @return id of the parent session.
         */
        /* package */static long getSessionIdForBlobId(final LocalyticsProvider provider, final long blobId)
        {
            /*
             * This implementation needs to walk up the tree of database elements.
             */

            long eventId;
            {
                Cursor cursor = null;
                try
                {
                    cursor = provider.query(UploadBlobEventsDbColumns.TABLE_NAME, new String[]
                        { UploadBlobEventsDbColumns.EVENTS_KEY_REF }, String.format("%s = ?", UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF), new String[] //$NON-NLS-1$
                        { Long.toString(blobId) }, null);

                    if (cursor.moveToFirst())
                    {
                        eventId = cursor.getLong(cursor.getColumnIndexOrThrow(UploadBlobEventsDbColumns.EVENTS_KEY_REF));
                    }
                    else
                    {
                        /*
                         * This should never happen
                         */
                        throw new RuntimeException("No events associated with blob"); //$NON-NLS-1$
                    }
                }
                finally
                {
                    if (null != cursor)
                    {
                        cursor.close();
                        cursor = null;
                    }
                }
            }

            long sessionId;
            {
                Cursor cursor = null;
                try
                {
                    cursor = provider.query(EventsDbColumns.TABLE_NAME, new String[]
                        { EventsDbColumns.SESSION_KEY_REF }, String.format("%s = ?", EventsDbColumns._ID), new String[] //$NON-NLS-1$
                        { Long.toString(eventId) }, null);

                    if (cursor.moveToFirst())
                    {
                        sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF));
                    }
                    else
                    {
                        /*
                         * This should never happen
                         */
                        throw new RuntimeException("No session associated with event"); //$NON-NLS-1$
                    }
                }
                finally
                {
                    if (null != cursor)
                    {
                        cursor.close();
                        cursor = null;
                    }
                }
            }

            return sessionId;
        }
    }

    /**
     * Internal helper class to pass two objects to the Handler via the {@link Message#obj}.
     */
    /*
     * Once support for Android 1.6 is dropped, using Android's built-in Pair class would be preferable
     */
    private static final class Pair<F, S>
    {
        public final F first;

        public final S second;

        public Pair(final F first, final S second)
        {
            this.first = first;
            this.second = second;
        }
    }
    
    /**
     * Internal helper class to pass three objects to the Handler via the {@link Message#obj}.
     */
    private static final class Triple<F, S, T>
    {
        public final F first;

        public final S second;
        
        public final T third;

        public Triple(final F first, final S second, final T third)
        {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }
}