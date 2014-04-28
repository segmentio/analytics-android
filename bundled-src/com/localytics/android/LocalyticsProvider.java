// @formatter:off
/*
 * LocalyticsProvider.java Copyright (C) 2013 Char Software Inc., DBA Localytics. This code is provided under the Localytics
 * Modified BSD License. A copy of this license has been distributed in a file called LICENSE with this source code. Please visit
 * www.localytics.com for more information.
 */
// @formatter:on

package com.localytics.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Implements the storage mechanism for the Localytics library. The interface and implementation are similar to a ContentProvider
 * but modified to be better suited to a library. The interface is table-oriented, rather than Uri-oriented.
 * <p>
 * This is not a public API.
 */
/* package */final class LocalyticsProvider
{
    /**
     * Name of the Localytics database, stored in the host application's {@link Context#getDatabasePath(String)}.
     * <p>
     * This is not a public API.
     */
    /*
     * This field is made package-accessible for unit testing. While the exact file name is arbitrary, this name was chosen to
     * avoid collisions with app developers because it is sufficiently long and uses the Localytics package namespace.
     */
    /* package */static final String DATABASE_FILE = "com.localytics.android.%s.sqlite"; //$NON-NLS-1$

    /**
     * Version of the database.
     * <p>
     * Version history:
     * <ol>
     * <li>1: Initial version</li>
     * <li>2: No format changes--just deleting bad data stranded in the database</li>
     * <li>3: No format changes--just deleting bad data stranded in the database</li>
     * <li>4: Add {@link SessionsDbColumns#LOCALYTICS_INSTALLATION_ID}</li>
     * <li>5: Add {@link SessionsDbColumns#DEVICE_WIFI_MAC_HASH}</li>
     * <li>6: Change attributes to have a package name prefix to allow for Localytics internal attributes</li>
     * <li>7: Add info table</li>
     * <li>8: Create identifiers table and add primary key to info table</li>
     * <li>9: Add {@link EventsDbColumns#CLV_INCREASE}</li>
     * <li>10: Add {@link InfoDbColumns#PLAY_ATTRIBUTION}</li>
     * <li>11: Add {@link InfoDbColumns#REGISTRATION_ID}</li>
     * <li>12: Add {@link InfoDbColumns#FIRST_ANDROID_ID} and {@link SessionsDbColumns#DEVICE_ANDROID_ID}</li>
     * <li>13: Add {@link EventsDbColumns#LAT_NAME} and {@link EventsDbColumns#LNG_NAME</li>
     * </ol>
     */
    private static final int DATABASE_VERSION = 13;

    /**
     * Singleton instance of the {@link LocalyticsProvider}. Lazily initialized via {@link #getInstance(Context, String)}.
     */
    private static final Map<String, LocalyticsProvider> sLocalyticsProviderMap = new HashMap<String, LocalyticsProvider>();

    /**
     * Intrinsic lock for synchronizing the initialization of {@link #sLocalyticsProviderMap}.
     */
    /*
     * Fun fact: Object[0] is more efficient that Object for an intrinsic lock
     */
    private static final Object[] sLocalyticsProviderIntrinsicLock = new Object[0];

    /**
     * Projection map for {@link BaseColumns#_COUNT}.
     */
    private static final Map<String, String> sCountProjectionMap = Collections.unmodifiableMap(getCountProjectionMap());

    /**
     * Unmodifiable set of valid table names.
     */
    private static final Set<String> sValidTables = Collections.unmodifiableSet(getValidTables());

    /**
     * SQLite database owned by the provider.
     */
    private final SQLiteDatabase mDb;

    /**
     * Obtains an instance of the Localytics Provider. Since the provider is a singleton object, only a single instance will be
     * returned.
     * <p>
     * Note: if {@code context} is an instance of {@link android.test.RenamingDelegatingContext}, then a new object will be
     * returned every time. This is not a "public" API, but is documented here as it aids unit testing.
     *
     * @param context Application context. Cannot be null.
     * @param apiKey TODO
     * @return An instance of {@link LocalyticsProvider}.
     * @throws IllegalArgumentException if {@code context} is null
     */
    public static LocalyticsProvider getInstance(final Context context, final String apiKey)
    {
        /*
         * Note: Don't call getApplicationContext() on the context, as that would return a different context and defeat useful
         * contexts such as RenamingDelegatingContext.
         */

        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (null == context)
            {
                throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
            }
        }

        /*
         * Although RenamingDelegatingContext is part of the Android SDK, the class isn't present in the ClassLoader unless the
         * process is being run as a unit test. For that reason, comparing class names is necessary instead of doing instanceof.
         */
        if (context.getClass().getName().equals("android.test.RenamingDelegatingContext")) //$NON-NLS-1$
        {
            return new LocalyticsProvider(context, apiKey);
        }

        synchronized (sLocalyticsProviderIntrinsicLock)
        {
            LocalyticsProvider provider = sLocalyticsProviderMap.get(apiKey);

            if (null == provider)
            {
                provider = new LocalyticsProvider(context, apiKey);
                sLocalyticsProviderMap.put(apiKey, provider);
            }

            return provider;
        }
    }

    /**
     * Constructs a new Localytics Provider.
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param context application context. Cannot be null.
     */
    private LocalyticsProvider(final Context context, final String apiKey)
    {
        /*
         * Rather than use the API key directly in the file name, it is put through SHA-256. The main reason for doing that is to
         * decouple the requirements of the Android file system from the possible values of the API key string. There is a very,
         * very small risk of a collision with the SHA-256 algorithm, but most clients will only have a single API key. Those with
         * multiple keys may have 2 or 3, so the risk of a collision there is also very low.
         */
        mDb = new DatabaseHelper(context, String.format(DATABASE_FILE, DatapointHelper.getSha256_buggy(apiKey)), DATABASE_VERSION).getWritableDatabase();
    }

    /**
     * Inserts a new record.
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param tableName name of the table operate on. Must be one of the recognized tables. Cannot be null.
     * @param values ContentValues to insert. Cannot be null.
     * @return the {@link BaseColumns#_ID} of the inserted row or -1 if an error occurred.
     * @throws IllegalArgumentException if tableName is null or not a valid table name.
     * @throws IllegalArgumentException if values are null.
     */
    public long insert(final String tableName, final ContentValues values)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (!isValidTable(tableName))
            {
                throw new IllegalArgumentException(String.format("tableName %s is invalid", tableName)); //$NON-NLS-1$
            }

            if (null == values)
            {
                throw new IllegalArgumentException("values cannot be null"); //$NON-NLS-1$
            }
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Insert table: %s, values: %s", tableName, values.toString())); //$NON-NLS-1$
        }

        final long result = mDb.insertOrThrow(tableName, null, values);

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Inserted row with new id %d", Long.valueOf(result))); //$NON-NLS-1$
        }

        return result;
    }

    /**
     * Performs a query.
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param tableName name of the table operate on. Must be one of the recognized tables. Cannot be null.
     * @param projection The list of columns to include. If null, then all columns are included by default.
     * @param selection A filter to apply to all rows, like the SQLite WHERE clause. Passing null will query all rows. This param
     *            may contain ? symbols, which will be replaced by values from the {@code selectionArgs} param.
     * @param selectionArgs An optional string array of replacements for ? symbols in {@code selection}. May be null.
     * @param sortOrder How the rows in the cursor should be sorted. If null, then the sort order is undefined.
     * @return Cursor for the query. To the receiver: Don't forget to call .close() on the cursor when finished with it.
     * @throws IllegalArgumentException if tableName is null or not a valid table name.
     */
    public Cursor query(final String tableName, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (!isValidTable(tableName))
            {
                throw new IllegalArgumentException(String.format("tableName %s is invalid", tableName)); //$NON-NLS-1$
            }
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Query table: %s, projection: %s, selection: %s, selectionArgs: %s", tableName, Arrays.toString(projection), selection, Arrays.toString(selectionArgs))); //$NON-NLS-1$
        }

        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(tableName);

        if (projection != null && 1 == projection.length && BaseColumns._COUNT.equals(projection[0]))
        {
            qb.setProjectionMap(sCountProjectionMap);
        }

        final Cursor result = qb.query(mDb, projection, selection, selectionArgs, null, null, sortOrder);

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, "Query result is: " + DatabaseUtils.dumpCursorToString(result)); //$NON-NLS-1$
        }

        return result;
    }

    /**
     * Updates row(s).
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param tableName name of the table operate on. Must be one of the recognized tables. Cannot be null.
     * @param values A ContentValues mapping from column names (see the associated BaseColumns class for the table) to new column
     *            values.
     * @param selection A filter to limit which rows are updated, like the SQLite WHERE clause. Passing null implies all rows.
     *            This param may contain ? symbols, which will be replaced by values from the {@code selectionArgs} param.
     * @param selectionArgs An optional string array of replacements for ? symbols in {@code selection}. May be null.
     * @return int representing the number of rows modified, which is in the range from 0 to the number of items in the table.
     * @throws IllegalArgumentException if tableName is null or not a valid table name.
     */
    public int update(final String tableName, final ContentValues values, final String selection, final String[] selectionArgs)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (!isValidTable(tableName))
            {
                throw new IllegalArgumentException(String.format("tableName %s is invalid", tableName)); //$NON-NLS-1$
            }
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Update table: %s, values: %s, selection: %s, selectionArgs: %s", tableName, values.toString(), selection, Arrays.toString(selectionArgs))); //$NON-NLS-1$
        }

        return mDb.update(tableName, values, selection, selectionArgs);
    }		

    /**
     * Deletes row(s).
     * 
     * WORKAROUND for ART verifier bug in KitKat: Changed method name from delete to remove
     * 
     * <p>
     * Note: this method may perform disk operations.
     *
     * @param tableName name of the table operate on. Must be one of the recognized tables. Cannot be null.
     * @param selection A filter to limit which rows are deleted, like the SQLite WHERE clause. Passing null implies all rows.
     *            This param may contain ? symbols, which will be replaced by values from the {@code selectionArgs} param.
     * @param selectionArgs An optional string array of replacements for ? symbols in {@code selection}. May be null.
     * @return The number of rows affected, which is in the range from 0 to the number of items in the table.
     * @throws IllegalArgumentException if tableName is null or not a valid table name.
     */
    public int remove(final String tableName, final String selection, final String[] selectionArgs)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (!isValidTable(tableName))
            {
                throw new IllegalArgumentException(String.format("tableName %s is invalid", tableName)); //$NON-NLS-1$
            }
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Delete table: %s, selection: %s, selectionArgs: %s", tableName, selection, Arrays.toString(selectionArgs))); //$NON-NLS-1$
        }

        final int count;
        if (null == selection)
        {
            count = mDb.delete(tableName, "1", null); //$NON-NLS-1$
        }
        else
        {
            count = mDb.delete(tableName, selection, selectionArgs);
        }

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Deleted %d rows", Integer.valueOf(count))); //$NON-NLS-1$
        }

        return count;
    }

    /**
     * Executes an arbitrary runnable with exclusive access to the database, essentially allowing an atomic transaction.
     *
     * @param runnable Runnable to execute. Cannot be null.
     * @throws IllegalArgumentException if {@code runnable} is null
     */
    /*
     * This implementation is sort of a hack. In the future, it would be better model this after applyBatch() with a list of
     * ContentProviderOperation objects. But that API isn't available until Android 2.0.
     *
     * An alternative implementation would have been to expose the begin/end transaction methods on the Provider object. While
     * that would work, it makes it harder to transition to a ContentProviderOperation model in the future.
     */
    public void runBatchTransaction(final Runnable runnable)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (null == runnable)
            {
                throw new IllegalArgumentException("runnable cannot be null"); //$NON-NLS-1$
            }
        }

        mDb.beginTransaction();
        try
        {
            runnable.run();
            mDb.setTransactionSuccessful();
        }
        finally
        {
            mDb.endTransaction();
        }
    }

    /**
     * Closes the LocalyticsProvider object. Normally the provider is a long-lived object and should not be closed during normal
     * application use. This method is intended for unit testing purposes only, where a lot of temporary provider objects are
     * created and should be closed.
     */
    /* package */void close()
    {
        synchronized (sLocalyticsProviderIntrinsicLock)
        {
            String key = null;
            for (Entry<String, LocalyticsProvider> entry : sLocalyticsProviderMap.entrySet())
            {
                if (this == entry.getValue())
                {
                    key = entry.getKey();
                    break;
                }
            }

            if (null != key)
            {
                sLocalyticsProviderMap.remove(key);
            }
        }

        mDb.close();
    }

    /**
     * Private helper to test whether a given table name is valid
     *
     * @param table name of a table to check. This param may be null.
     * @return true if the table is valid, false if the table is invalid. If {@code table} is null, returns false.
     */
    private static boolean isValidTable(final String table)
    {
        if (null == table)
        {
            return false;
        }

        return sValidTables.contains(table);
    }

    /**
     * Private helper that knows all the tables that {@link LocalyticsProvider} can operate on.
     *
     * @return returns a set of the valid tables.
     */
    private static Set<String> getValidTables()
    {
        final HashSet<String> tables = new HashSet<String>();

        tables.add(ApiKeysDbColumns.TABLE_NAME);
        tables.add(AttributesDbColumns.TABLE_NAME);
        tables.add(EventsDbColumns.TABLE_NAME);
        tables.add(EventHistoryDbColumns.TABLE_NAME);
        tables.add(SessionsDbColumns.TABLE_NAME);
        tables.add(UploadBlobsDbColumns.TABLE_NAME);
        tables.add(UploadBlobEventsDbColumns.TABLE_NAME);
        tables.add(InfoDbColumns.TABLE_NAME);
        tables.add(IdentifiersDbColumns.TABLE_NAME);

        return tables;
    }

    /**
     * @return Projection map for {@link BaseColumns#_COUNT}.
     */
    private static HashMap<String, String> getCountProjectionMap()
    {
        final HashMap<String, String> temp = new HashMap<String, String>();
        temp.put(BaseColumns._COUNT, "COUNT(*)"); //$NON-NLS-1$

        return temp;
    }

    /**
     * Private helper that deletes files from older versions of the Localytics library.
     * <p>
     * Note: This is a private method that is only made package-accessible for unit testing.
     *
     * @param context application context
     * @throws IllegalArgumentException if {@code context} is null
     */
    /* package */static void deleteOldFiles(final Context context)
    {
        if (Constants.IS_PARAMETER_CHECKING_ENABLED)
        {
            if (null == context)
            {
                throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
            }
        }

        deleteDirectory(new File(context.getFilesDir(), "localytics")); //$NON-NLS-1$
    }

    /**
     * Private helper to delete a directory, regardless of whether the directory is empty.
     *
     * @param directory Directory or file to delete. Cannot be null.
     * @return true if deletion was successful. False if deletion failed.
     */
    private static boolean deleteDirectory(final File directory)
    {
        if (directory.exists() && directory.isDirectory())
        {
            for (final String child : directory.list())
            {
                final boolean success = deleteDirectory(new File(directory, child));
                if (!success)
                {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return directory.delete();
    }

    /**
     * A private helper class to open and create the Localytics SQLite database.
     */
    private static final class DatabaseHelper extends SQLiteOpenHelper
    {
        /**
         * Constant representing the SQLite value for true
         */
        private static final String SQLITE_BOOLEAN_TRUE = "1"; //$NON-NLS-1$

        /**
         * Constant representing the SQLite value for false
         */
        private static final String SQLITE_BOOLEAN_FALSE = "0"; //$NON-NLS-1$

        /**
         * Application context
         */
        private final Context mContext;

        /**
         * @param context Application context. Cannot be null.
         * @param name File name of the database. Cannot be null or empty. A database with this name will be opened in
         *            {@link Context#getDatabasePath(String)}.
         * @param version version of the database.
         */
        public DatabaseHelper(final Context context, final String name, final int version)
        {
            super(context, name, null, version);

            mContext = context;
        }

        /**
         * Initializes the tables of the database.
         * <p>
         * If an error occurs during initialization and an exception is thrown, {@link SQLiteDatabase#close()} will not be called
         * by this method. That responsibility is left to the caller.
         *
         * @param db The database to perform post-creation processing on. db cannot not be null
         * @throws IllegalArgumentException if db is null
         */
        @Override
        public void onCreate(final SQLiteDatabase db)
        {
            if (null == db)
            {
                throw new IllegalArgumentException("db cannot be null"); //$NON-NLS-1$
            }

            // api_keys table
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT UNIQUE NOT NULL, %s TEXT UNIQUE NOT NULL, %s INTEGER NOT NULL CHECK (%s >= 0), %s INTEGER NOT NULL CHECK(%s IN (%s, %s)));", ApiKeysDbColumns.TABLE_NAME, ApiKeysDbColumns._ID, ApiKeysDbColumns.API_KEY, ApiKeysDbColumns.UUID, ApiKeysDbColumns.CREATED_TIME, ApiKeysDbColumns.CREATED_TIME, ApiKeysDbColumns.OPT_OUT, ApiKeysDbColumns.OPT_OUT, SQLITE_BOOLEAN_FALSE, SQLITE_BOOLEAN_TRUE)); //$NON-NLS-1$

            // sessions table
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER REFERENCES %s(%s) NOT NULL, %s TEXT UNIQUE NOT NULL, %s INTEGER NOT NULL CHECK (%s >= 0), %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT);", SessionsDbColumns.TABLE_NAME, SessionsDbColumns._ID, SessionsDbColumns.API_KEY_REF, ApiKeysDbColumns.TABLE_NAME, ApiKeysDbColumns._ID, SessionsDbColumns.UUID, SessionsDbColumns.SESSION_START_WALL_TIME, SessionsDbColumns.SESSION_START_WALL_TIME, SessionsDbColumns.LOCALYTICS_LIBRARY_VERSION, SessionsDbColumns.LOCALYTICS_INSTALLATION_ID, SessionsDbColumns.APP_VERSION, SessionsDbColumns.ANDROID_VERSION, SessionsDbColumns.ANDROID_SDK, SessionsDbColumns.DEVICE_MODEL, SessionsDbColumns.DEVICE_MANUFACTURER, SessionsDbColumns.DEVICE_ANDROID_ID_HASH, SessionsDbColumns.DEVICE_TELEPHONY_ID, SessionsDbColumns.DEVICE_TELEPHONY_ID_HASH, SessionsDbColumns.DEVICE_SERIAL_NUMBER_HASH, SessionsDbColumns.DEVICE_WIFI_MAC_HASH, SessionsDbColumns.LOCALE_LANGUAGE, SessionsDbColumns.LOCALE_COUNTRY, SessionsDbColumns.NETWORK_CARRIER, SessionsDbColumns.NETWORK_COUNTRY, SessionsDbColumns.NETWORK_TYPE, SessionsDbColumns.DEVICE_COUNTRY, SessionsDbColumns.LATITUDE, SessionsDbColumns.LONGITUDE, SessionsDbColumns.DEVICE_ANDROID_ID)); //$NON-NLS-1$

            // events table
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER REFERENCES %s(%s) NOT NULL, %s TEXT UNIQUE NOT NULL, %s TEXT NOT NULL, %s INTEGER NOT NULL CHECK (%s >= 0), %s INTEGER NOT NULL CHECK (%s >= 0), %s INTEGER NOT NULL DEFAULT 0, %s REAL, %s REAL);", EventsDbColumns.TABLE_NAME, EventsDbColumns._ID, EventsDbColumns.SESSION_KEY_REF, SessionsDbColumns.TABLE_NAME, SessionsDbColumns._ID, EventsDbColumns.UUID, EventsDbColumns.EVENT_NAME, EventsDbColumns.REAL_TIME, EventsDbColumns.REAL_TIME, EventsDbColumns.WALL_TIME, EventsDbColumns.WALL_TIME, EventsDbColumns.CLV_INCREASE, EventsDbColumns.LAT_NAME, EventsDbColumns.LNG_NAME)); //$NON-NLS-1$
            
            // event_history table
            /*
             * Note: the events history should be using foreign key constrains on the upload blobs table, but that is currently
             * disabled to simplify the implementation of the upload processing.
             */
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER REFERENCES %s(%s) NOT NULL, %s TEXT NOT NULL CHECK(%s IN (%s, %s)), %s TEXT NOT NULL, %s INTEGER);", EventHistoryDbColumns.TABLE_NAME, EventHistoryDbColumns._ID, EventHistoryDbColumns.SESSION_KEY_REF, SessionsDbColumns.TABLE_NAME, SessionsDbColumns._ID, EventHistoryDbColumns.TYPE, EventHistoryDbColumns.TYPE, Integer.valueOf(EventHistoryDbColumns.TYPE_EVENT), Integer.valueOf(EventHistoryDbColumns.TYPE_SCREEN), EventHistoryDbColumns.NAME, EventHistoryDbColumns.PROCESSED_IN_BLOB)); //$NON-NLS-1$
            //db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER REFERENCES %s(%s) NOT NULL, %s TEXT NOT NULL CHECK(%s IN (%s, %s)), %s TEXT NOT NULL, %s INTEGER REFERENCES %s(%s));", EventHistoryDbColumns.TABLE_NAME, EventHistoryDbColumns._ID, EventHistoryDbColumns.SESSION_KEY_REF, SessionsDbColumns.TABLE_NAME, SessionsDbColumns._ID, EventHistoryDbColumns.TYPE, EventHistoryDbColumns.TYPE, Integer.valueOf(EventHistoryDbColumns.TYPE_EVENT), Integer.valueOf(EventHistoryDbColumns.TYPE_SCREEN), EventHistoryDbColumns.NAME, EventHistoryDbColumns.PROCESSED_IN_BLOB, UploadBlobsDbColumns.TABLE_NAME, UploadBlobsDbColumns._ID)); //$NON-NLS-1$

            // attributes table
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER REFERENCES %s(%s) NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL);", AttributesDbColumns.TABLE_NAME, AttributesDbColumns._ID, AttributesDbColumns.EVENTS_KEY_REF, EventsDbColumns.TABLE_NAME, EventsDbColumns._ID, AttributesDbColumns.ATTRIBUTE_KEY, AttributesDbColumns.ATTRIBUTE_VALUE)); //$NON-NLS-1$

            // upload blobs
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT UNIQUE NOT NULL);", UploadBlobsDbColumns.TABLE_NAME, UploadBlobsDbColumns._ID, UploadBlobsDbColumns.UUID)); //$NON-NLS-1$

            // upload events
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER REFERENCES %s(%s) NOT NULL, %s INTEGER REFERENCES %s(%s) NOT NULL);", UploadBlobEventsDbColumns.TABLE_NAME, UploadBlobEventsDbColumns._ID, UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF, UploadBlobsDbColumns.TABLE_NAME, UploadBlobsDbColumns._ID, UploadBlobEventsDbColumns.EVENTS_KEY_REF, EventsDbColumns.TABLE_NAME, EventsDbColumns._ID)); //$NON-NLS-1$
        
            // info table
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s INTEGER);", InfoDbColumns.TABLE_NAME, InfoDbColumns._ID, InfoDbColumns.FB_ATTRIBUTION, InfoDbColumns.PLAY_ATTRIBUTION, InfoDbColumns.REGISTRATION_ID, InfoDbColumns.REGISTRATION_VERSION, InfoDbColumns.FIRST_ANDROID_ID, InfoDbColumns.FIRST_TELEPHONY_ID, InfoDbColumns.PACKAGE_NAME, InfoDbColumns.FIRST_RUN));
            final ContentValues values = new ContentValues();
            values.put(InfoDbColumns.FB_ATTRIBUTION, DatapointHelper.getFBAttribution(mContext));
            values.put(InfoDbColumns.FIRST_RUN, Boolean.TRUE);
            values.put(InfoDbColumns.FIRST_ANDROID_ID, DatapointHelper.getAndroidIdOrNull(mContext));
            values.put(InfoDbColumns.FIRST_TELEPHONY_ID, DatapointHelper.getTelephonyDeviceIdOrNull(mContext));
            values.put(InfoDbColumns.PACKAGE_NAME, mContext.getPackageName());
            db.insertOrThrow(InfoDbColumns.TABLE_NAME, null, values);
            
            // identifiers table
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT UNIQUE NOT NULL, %s TEXT NOT NULL);", IdentifiersDbColumns.TABLE_NAME, IdentifiersDbColumns._ID, IdentifiersDbColumns.KEY, IdentifiersDbColumns.VALUE));	
        }

        @Override
        public void onOpen(final SQLiteDatabase db)
        {
            super.onOpen(db);

            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, String.format("SQLite library version is: %s", DatabaseUtils.stringForQuery(db, "select sqlite_version()", null))); //$NON-NLS-1$//$NON-NLS-2$
            }

            if (!db.isReadOnly())
            {
                /*
                 * Enable foreign key support
                 */
                db.execSQL("PRAGMA foreign_keys = ON;"); //$NON-NLS-1$

                // if (Constants.IS_LOGGABLE)
                // {
                // try
                // {
                //                        final String result1 = DatabaseUtils.stringForQuery(db, "PRAGMA foreign_keys;", null); //$NON-NLS-1$
                //                        Log.v(Constants.LOG_TAG, String.format("Foreign keys support result was: %s", result1)); //$NON-NLS-1$
                // }
                // catch (final SQLiteDoneException e)
                // {
                // Log.w(Constants.LOG_TAG, e);
                // }
                // }
            }
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion)
        {
            /*
             * Delete all sessions in the database, in order to get the data back into a consistent state. This is necessary
             * because an Android bug that caused the database in older versions of the Localytics library to become corrupted.
             */
            if (oldVersion < 3)
            {
                db.delete(UploadBlobEventsDbColumns.TABLE_NAME, null, null);
                db.delete(EventHistoryDbColumns.TABLE_NAME, null, null);
                db.delete(UploadBlobsDbColumns.TABLE_NAME, null, null);
                db.delete(AttributesDbColumns.TABLE_NAME, null, null);
                db.delete(EventsDbColumns.TABLE_NAME, null, null);
                db.delete(SessionsDbColumns.TABLE_NAME, null, null);
            }

            if (oldVersion < 4)
            {
                // if the table is upgraded, it won't have the NOT NULL constraint that is normally present when the table is
                // freshly created
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;", SessionsDbColumns.TABLE_NAME, SessionsDbColumns.LOCALYTICS_INSTALLATION_ID)); //$NON-NLS-1$
            }

            if (oldVersion < 5)
            {
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;", SessionsDbColumns.TABLE_NAME, SessionsDbColumns.DEVICE_WIFI_MAC_HASH)); //$NON-NLS-1$
            }

            if (oldVersion < 6)
            {
                Cursor attributesCursor = null;
                try
                {
                    attributesCursor = db.query(AttributesDbColumns.TABLE_NAME, new String[]
                        {
                            AttributesDbColumns._ID,
                            AttributesDbColumns.ATTRIBUTE_KEY }, null, null, null, null, null);

                    final int idColumnIndex = attributesCursor.getColumnIndexOrThrow(AttributesDbColumns._ID);
                    final int keyColumnIndex = attributesCursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_KEY);

                    final ContentValues tempValues = new ContentValues();
                    final String whereClause = String.format("%s = ?", AttributesDbColumns._ID); //$NON-NLS-1$
                    final String[] whereArgs = new String[1];

                    attributesCursor.moveToPosition(-1);
                    while (attributesCursor.moveToNext())
                    {
                        tempValues.put(AttributesDbColumns.ATTRIBUTE_KEY, String.format(AttributesDbColumns.ATTRIBUTE_FORMAT, mContext.getPackageName(), attributesCursor.getString(keyColumnIndex)));

                        whereArgs[0] = Long.toString(attributesCursor.getLong(idColumnIndex));
                        db.update(AttributesDbColumns.TABLE_NAME, tempValues, whereClause, whereArgs);

                        tempValues.clear();
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
            
            if (oldVersion < 7)
            {
                // info table
            	db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s (%s TEXT, %s INTEGER);", InfoDbColumns.TABLE_NAME, InfoDbColumns.FB_ATTRIBUTION, InfoDbColumns.FIRST_RUN));
            	final ContentValues values = new ContentValues();
            	values.putNull(InfoDbColumns.FB_ATTRIBUTION);
            	values.put(InfoDbColumns.FIRST_RUN, Boolean.FALSE);
            	db.insertOrThrow(InfoDbColumns.TABLE_NAME, null, values);
            }
            
            if (oldVersion < 8)
            {
                // identifiers table
            	db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT UNIQUE NOT NULL, %s TEXT NOT NULL);", IdentifiersDbColumns.TABLE_NAME, IdentifiersDbColumns._ID, IdentifiersDbColumns.KEY, IdentifiersDbColumns.VALUE));	         
            }
            
            if (oldVersion < 9)
            {            	
            	// add clv_increase column to events table
                db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT 0;", EventsDbColumns.TABLE_NAME, EventsDbColumns.CLV_INCREASE)); //$NON-NLS-1$
            }
            
            if (oldVersion < 10)
            {
            	// add play_attribution to info table
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;", InfoDbColumns.TABLE_NAME, InfoDbColumns.PLAY_ATTRIBUTION)); //$NON-NLS-1$
            }
            
            if (oldVersion < 11)
            {
            	// add registration_id and registration_version to info table
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;", InfoDbColumns.TABLE_NAME, InfoDbColumns.REGISTRATION_ID)); //$NON-NLS-1$
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;", InfoDbColumns.TABLE_NAME, InfoDbColumns.REGISTRATION_VERSION)); //$NON-NLS-1$
            }
            
            if (oldVersion < 12)
            {
            	// add first_android_id to info table
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;", InfoDbColumns.TABLE_NAME, InfoDbColumns.FIRST_ANDROID_ID)); //$NON-NLS-1$
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;", InfoDbColumns.TABLE_NAME, InfoDbColumns.FIRST_TELEPHONY_ID)); //$NON-NLS-1$
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;", InfoDbColumns.TABLE_NAME, InfoDbColumns.PACKAGE_NAME)); //$NON-NLS-1$
            	final ContentValues values = new ContentValues();
                values.put(InfoDbColumns.FIRST_ANDROID_ID, DatapointHelper.getAndroidIdOrNull(mContext));
                values.put(InfoDbColumns.FIRST_TELEPHONY_ID, DatapointHelper.getTelephonyDeviceIdOrNull(mContext));
                values.put(InfoDbColumns.PACKAGE_NAME, mContext.getPackageName());
            	db.update(InfoDbColumns.TABLE_NAME, values, null, null);
            	
            	// add device_android_id to sessions table
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT;", SessionsDbColumns.TABLE_NAME, SessionsDbColumns.DEVICE_ANDROID_ID)); //$NON-NLS-1$
            }
            
            if (oldVersion < 13)
            {
            	// add lat and lng to events table
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s REAL;", EventsDbColumns.TABLE_NAME, EventsDbColumns.LAT_NAME)); //$NON-NLS-1$
            	db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s REAL;", EventsDbColumns.TABLE_NAME, EventsDbColumns.LNG_NAME)); //$NON-NLS-1$
            }
        }
        // @Override
        // public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
        // {
        // }
    }

    /**
     * Table for the API keys used and the opt-out preferences for each API key.
     * <p>
     * This is not a public API.
     */
    public static final class ApiKeysDbColumns implements BaseColumns
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private ApiKeysDbColumns()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "api_keys"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * The Localytics API key.
         * <p>
         * Constraints: This column is unique and cannot be null.
         */
        public static final String API_KEY = "api_key"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * A UUID for the installation.
         * <p>
         * Constraints: This column is unique and cannot be null.
         */
        public static final String UUID = "uuid"; //$NON-NLS-1$

        /**
         * TYPE: {@code boolean}
         * <p>
         * A flag indicating whether the user has opted out of data collection.
         * <p>
         * Constraints: This column must be in the set {0, 1} and cannot be null.
         */
        public static final String OPT_OUT = "opt_out"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A long representing the {@link System#currentTimeMillis()} when the row was created. Once created, this row will not be
         * modified.
         * <p>
         * Constraints: This column must be >=0. This column cannot be null.
         */
        public static final String CREATED_TIME = "created_time"; //$NON-NLS-1$
 
    }
    
    /**
     * Table for storing global Localytics info
     * <p>
     * This is not a public API.
     */
    public static final class InfoDbColumns implements BaseColumns
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private InfoDbColumns()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "info"; //$NON-NLS-1$

        /**
         * TYPE: {@code boolean}
         * <p>
         * A flag indicating whether this is the first run.
         * <p>
         * Constraints: This column must be in the set {0, 1} and cannot be null.
         */
        public static final String FIRST_RUN = "first_run"; //$NON-NLS-1$
        
        /**
         * TYPE: {@code String}
         * <p>
         * The FB attribution cookie at install-time. May be null if unavailable or already uploaded.
         * <p>
         */
        public static final String FB_ATTRIBUTION = "fb_attribution"; //$NON-NLS-1$
        
        /**
         * TYPE: {@code String}
         * <p>
         * The Google Play referrer string at install-time. May be null if unavailable or already uploaded.
         * <p>
         */
        public static final String PLAY_ATTRIBUTION = "play_attribution"; //$NON-NLS-1$
        
        /**
         * TYPE: {@code String}
         * <p>
         * The GCM registration ID. May be null if notifications are disabled.
         * <p>
         */
        public static final String REGISTRATION_ID = "registration_id"; //$NON-NLS-1$
        
        /**
         * TYPE: {@code String}
         * <p>
         * The app version used to determine if GCM needs to be re-registered. May be null if notifications are disabled. 
         * <p>
         */
        public static final String REGISTRATION_VERSION = "registration_version"; //$NON-NLS-1$
        
        /**
         * TYPE: {@code String}
         * <p>
         * String representing the device Android ID at install time
         * <p>
         * Constraints: None
         *
         * @see android.provider.Settings.Secure#ANDROID_ID
         */
        public static final String FIRST_ANDROID_ID = "first_android_id"; //$NON-NLS-1$
        
        /**
         * TYPE: {@code String}
         * <p>
         * String representing the telephony ID at install time. May be null for non-telephony devices. May also be null if the
         * parent application doesn't have {@link android.Manifest.permission#READ_PHONE_STATE}.
         * <p>
         * Constraints: None
         *
         * @see android.telephony.TelephonyManager#getDeviceId()
         */
        public static final String FIRST_TELEPHONY_ID = "first_telephony_id"; //$NON-NLS-1$
        
        /**
         * TYPE: {@code String}
         * <p>
         * String representing the package name. 
         * <p>
         */
        public static final String PACKAGE_NAME = "package_name"; //$NON-NLS-1$        
    }
    
    /**
     * Table for storing identifying information
     * <p>
     * This is not a public API.
     */
    public static final class IdentifiersDbColumns implements BaseColumns
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private IdentifiersDbColumns()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "identifiers"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * The key value for identifying information about the user
         * <p>
         */
        public static final String KEY = "key"; //$NON-NLS-1$
        
        /**
         * TYPE: {@code String}
         * <p>
         * Identifying information about the user
         * <p>
         */
        public static final String VALUE = "value"; //$NON-NLS-1$
    }

    /**
     * Database table for the session attributes. There is a one-to-many relationship between one event in the
     * {@link EventsDbColumns} table and the many attributes associated with that event.
     * <p>
     * This is not a public API.
     */
    public static final class AttributesDbColumns implements BaseColumns
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private AttributesDbColumns()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "attributes"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A one-to-many relationship with {@link EventsDbColumns#_ID}.
         * <p>
         * Constraints: This is a foreign key with the {@link EventsDbColumns#_ID} column. This cannot be null.
         */
        public static final String EVENTS_KEY_REF = "events_key_ref"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the key name of the attribute.
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String ATTRIBUTE_KEY = "attribute_key"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the value of the attribute.
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String ATTRIBUTE_VALUE = "attribute_value"; //$NON-NLS-1$

        /**
         * Format string for attributes. The string is packagename:attribute name
         */
        /* package */static final String ATTRIBUTE_FORMAT = "%s:%s"; //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_1 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_0"); //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_2 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_1"); //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_3 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_2"); //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_4 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_3"); //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_5 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_4"); //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_6 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_5"); //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_7 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_6"); //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_8 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_7"); //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_9 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_8"); //$NON-NLS-1$

        /**
         * Format string for the custom dimension attribute
         */
        /* package */static final String ATTRIBUTE_CUSTOM_DIMENSION_10 = String.format(ATTRIBUTE_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "custom_dimension_9"); //$NON-NLS-1$    
    }

    /**
     * Database table for the session events. There is a one-to-many relationship between one session data entry in the
     * {@link SessionsDbColumns} table and the many events associated with that session.
     * <p>
     * This is not a public API.
     */
    public static final class EventsDbColumns implements BaseColumns
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private EventsDbColumns()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "events"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A one-to-many relationship with {@link SessionsDbColumns#_ID}.
         * <p>
         * Constraints: This is a foreign key with the {@link SessionsDbColumns#_ID} column. This cannot be null.
         */
        public static final String SESSION_KEY_REF = "session_key_ref"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Unique ID of the event, as generated from {@link java.util.UUID}.
         * <p>
         * Constraints: This is unique and cannot be null.
         */
        public static final String UUID = "uuid"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the name of the event.
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String EVENT_NAME = "event_name"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A long representing the {@link android.os.SystemClock#elapsedRealtime()} when the event occurred.
         * <p>
         * Constraints: This column must be >=0. This column cannot be null.
         */
        public static final String REAL_TIME = "real_time"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A long representing the {@link System#currentTimeMillis()} when the event occurred.
         * <p>
         * Constraints: This column must be >=0. This column cannot be null.
         */
        public static final String WALL_TIME = "wall_time"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A long representing the customer value increase
         */
        public static final String CLV_INCREASE = "clv_increase"; //$NON-NLS-1$
        
        /**
         * Latitude of where a given event occurred
         */
        public static final String LAT_NAME = "event_lat";
        
        /**
         * Longitude of where a given event occurred
         */
        public static final String LNG_NAME = "event_lng";
    }

    /**
     * Database table for tracking the history of events and screens. There is a one-to-many relationship between one session data
     * entry in the {@link SessionsDbColumns} table and the many historical events associated with that session.
     * <p>
     * This is not a public API.
     */
    public static final class EventHistoryDbColumns implements BaseColumns
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private EventHistoryDbColumns()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "event_history"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A one-to-many relationship with {@link SessionsDbColumns#_ID}.
         * <p>
         * Constraints: This is a foreign key with the {@link SessionsDbColumns#_ID} column. This cannot be null.
         */
        public static final String SESSION_KEY_REF = "session_key_ref"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Unique ID of the event, as generated from {@link java.util.UUID}.
         * <p>
         * Constraints: This is unique and cannot be null.
         */
        public static final String TYPE = "type"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the name of the screen or event.
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String NAME = "name"; //$NON-NLS-1$

        /**
         * TYPE: {@code boolean}
         * <p>
         * Foreign key to the upload blob that this event was processed in. May be null indicating that this event wasn't
         * processed yet.
         */
        public static final String PROCESSED_IN_BLOB = "processed_in_blob"; //$NON-NLS-1$

        /**
         * Type value for {@link #TYPE} indicates an event event.
         */
        public static final int TYPE_EVENT = 0;

        /**
         * Type value for {@link #TYPE} that indicates a screen event.
         */
        public static final int TYPE_SCREEN = 1;
    }

    /**
     * Database table for the session data. There is a one-to-many relationship between one API key entry in the
     * {@link ApiKeysDbColumns} table and many sessions for that API key.
     * <p>
     * This is not a public API.
     */
    public static final class SessionsDbColumns implements BaseColumns
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private SessionsDbColumns()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "sessions"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A one-to-one relationship with {@link ApiKeysDbColumns#_ID}.
         * <p>
         * Constraints: This is a foreign key with the {@link ApiKeysDbColumns#_ID} column. This cannot be null.
         */
        public static final String API_KEY_REF = "api_key_ref"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Unique ID of the event, as generated from {@link java.util.UUID}.
         * <p>
         * Constraints: This is unique and cannot be null.
         */
        public static final String UUID = "uuid"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * The wall time when the session started.
         * <p>
         * Constraints: This column must be >=0. This column cannot be null.
         */
        /*
         * Note: While this same information is encoded in {@link EventsDbColumns#WALL_TIME} for the session open event, that row
         * may not be available when an upload occurs and the upload needs to compute the duration of the session.
         */
        public static final String SESSION_START_WALL_TIME = "session_start_wall_time"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Version of the Localytics client library.
         *
         * @see Constants#LOCALYTICS_CLIENT_LIBRARY_VERSION
         */
        public static final String LOCALYTICS_LIBRARY_VERSION = "localytics_library_version"; //$NON-NLS-1$

        /**
         * Type {@code String}
         * <p>
         * Installation UUID
         * <p>
         * Constraints: This column cannot be null.
         *
         * @see ApiKeysDbColumns#UUID
         */
        public static final String LOCALYTICS_INSTALLATION_ID = "iu"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the app's versionName
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String APP_VERSION = "app_version"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the version of Android
         * <p>
         * Constraints: This cannot be null.
         */
        public static final String ANDROID_VERSION = "android_version"; //$NON-NLS-1$

        /**
         * TYPE: {@code int}
         * <p>
         * Integer the Android SDK
         * <p>
         * Constraints: Must be an integer and cannot be null.
         *
         * @see android.os.Build.VERSION#SDK
         */
        public static final String ANDROID_SDK = "android_sdk"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the device model
         * <p>
         * Constraints: None
         *
         * @see android.os.Build#MODEL
         */
        public static final String DEVICE_MODEL = "device_model"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the device manufacturer
         * <p>
         * Constraints: None
         *
         * @see android.os.Build#MANUFACTURER
         */
        public static final String DEVICE_MANUFACTURER = "device_manufacturer"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing a hash of the device Android ID
         * <p>
         * Constraints: None
         *
         * @see android.provider.Settings.Secure#ANDROID_ID
         */
        public static final String DEVICE_ANDROID_ID_HASH = "device_android_id_hash"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing the device Android ID
         * <p>
         * Constraints: None
         *
         * @see android.provider.Settings.Secure#ANDROID_ID
         */
        public static final String DEVICE_ANDROID_ID = "device_android_id"; //$NON-NLS-1$
        
        /**
         * TYPE: {@code String}
         * <p>
         * String representing the telephony ID of the device. May be null for non-telephony devices. May also be null if the
         * parent application doesn't have {@link android.Manifest.permission#READ_PHONE_STATE}.
         * <p>
         * Constraints: None
         *
         * @see android.telephony.TelephonyManager#getDeviceId()
         */
        public static final String DEVICE_TELEPHONY_ID = "device_telephony_id"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing a hash of the telephony ID of the device. May be null for non-telephony devices. May also be null
         * if the parent application doesn't have {@link android.Manifest.permission#READ_PHONE_STATE}.
         * <p>
         * Constraints: None
         *
         * @see android.telephony.TelephonyManager#getDeviceId()
         */
        public static final String DEVICE_TELEPHONY_ID_HASH = "device_telephony_id_hash"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing a hash of the serial number of the device. May be null for some telephony devices.
         * <p>
         * Constraints: None
         */
        public static final String DEVICE_SERIAL_NUMBER_HASH = "device_serial_number_hash"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * String representing a hash of the Wi-Fi MAC address of the device. May be null if Wi-Fi isn't available or is disabled.
         * <p>
         * Constraints: None
         */
        public static final String DEVICE_WIFI_MAC_HASH = "device_wifi_mac_hash"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the locale language of the device.
         * <p>
         * Constraints: Cannot be null.
         */
        public static final String LOCALE_LANGUAGE = "locale_language"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the locale country of the device.
         * <p>
         * Constraints: Cannot be null.
         */
        public static final String LOCALE_COUNTRY = "locale_country"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the locale country of the device, according to the SIM card.
         * <p>
         * Constraints: Cannot be null.
         */
        public static final String DEVICE_COUNTRY = "device_country"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the network carrier of the device. May be null for non-telephony devices.
         * <p>
         * Constraints: None
         */
        public static final String NETWORK_CARRIER = "network_carrier"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the network country of the device. May be null for non-telephony devices.
         * <p>
         * Constraints: None
         */
        public static final String NETWORK_COUNTRY = "network_country"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Represents the primary network connection type for the device. This could be any type, including Wi-Fi, various cell
         * networks, Ethernet, etc.
         * <p>
         * Constraints: None
         *
         * @see android.telephony.TelephonyManager
         */
        public static final String NETWORK_TYPE = "network_type"; //$NON-NLS-1$

        /**
         * TYPE: {@code double}
         * <p>
         * Represents the latitude of the device. May be null if no longitude is known.
         * <p>
         * Constraints: None
         */
        public static final String LATITUDE = "latitude"; //$NON-NLS-1$

        /**
         * TYPE: {@code double}
         * <p>
         * Represents the longitude of the device. May be null if no longitude is known.
         * <p>
         * Constraints: None
         */
        public static final String LONGITUDE = "longitude"; //$NON-NLS-1$

    }

    /**
     * Database table for the events associated with a given upload blob. There is a one-to-many relationship between one upload
     * blob in the {@link UploadBlobsDbColumns} table and the blob events. There is a one-to-one relationship between each blob
     * event entry and the actual events in the {@link EventsDbColumns} table. *
     * <p>
     * This is not a public API.
     */
    public static final class UploadBlobEventsDbColumns implements BaseColumns
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private UploadBlobEventsDbColumns()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "upload_blob_events"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * A one-to-many relationship with {@link UploadBlobsDbColumns#_ID}.
         * <p>
         * Constraints: This is a foreign key with the {@link UploadBlobsDbColumns#_ID} column. This cannot be null.
         */
        public static final String UPLOAD_BLOBS_KEY_REF = "upload_blobs_key_ref"; //$NON-NLS-1$

        /**
         * TYPE: {@code long}
         * <p>
         * A one-to-one relationship with {@link EventsDbColumns#_ID}.
         * <p>
         * Constraints: This is a foreign key with the {@link EventsDbColumns#_ID} column. This cannot be null.
         */
        public static final String EVENTS_KEY_REF = "events_key_ref"; //$NON-NLS-1$
    }

    /**
     * Database table for the upload blobs. Logically, a blob owns many events. In terms of the implementation, some indirection
     * is introduced by a blob having a one-to-many relationship with {@link UploadBlobsDbColumns} and
     * {@link UploadBlobsDbColumns} having a one-to-one relationship with {@link EventsDbColumns}
     * <p>
     * This is not a public API.
     */
    public static final class UploadBlobsDbColumns implements BaseColumns
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private UploadBlobsDbColumns()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * SQLite table name
         */
        public static final String TABLE_NAME = "upload_blobs"; //$NON-NLS-1$

        /**
         * TYPE: {@code String}
         * <p>
         * Unique ID of the upload blob, as generated from {@link java.util.UUID}.
         * <p>
         * Constraints: This is unique and cannot be null.
         */
        public static final String UUID = "uuid"; //$NON-NLS-1$

    }
}
