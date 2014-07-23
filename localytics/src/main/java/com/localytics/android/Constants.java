// @formatter:off
/*
 * Constants.java Copyright (C) 2013 Char Software Inc., DBA Localytics. This code is provided under the Localytics
 * Modified BSD License. A copy of this license has been distributed in a file called LICENSE with this source code. Please visit
 * www.localytics.com for more information.
 */
// @formatter:on

package com.localytics.android;

import android.text.format.DateUtils;

/**
 * Build constants for the Localytics library.
 * <p>
 * This is not a public API.
 */
/* package */final class Constants
{

    /**
     * Version number of this library. This number is primarily important in terms of changes to the upload format.
     */
    //@formatter:off
    /*
     * Version history:
     *
     * 1.6: Fixed network type reporting.  Added reporting of app signature, device SDK level, device manufacturer, serial number.
     * 2.0: New upload format.
 	 * 2.5: Improvements in detecting time skew and Facebook attribution ability.
 	 * 2.6: Customer lifetime value support
 	 * 2.7: Google Play attribution and support for app keys in AndroidManifest.xml
 	 * 2.8: Add support for rollup keys in AndroidManifest.xml
 	 * 2.10: Push support
 	 * 2.11: Logging fixes and unhashed Android ID 
 	 * 2.12: Prevent multiple sessions from being created if open is called multiple times
 	 * 2.13: Add support for up to 10 custom dimensions (previously 4) 
 	 * 2.14: Expose appKey in ReferralReceiver so it can be set in code via subclassing
 	 * 2.15: KitKat GZIP bug workaround, optional HTTPS, setLocation API, and default to 50 attributes
 	 * 2.16: Improved handling of missing meta-data in AndroidManifest.xml
 	 * 2.17: Database migration fix
 	 * 2.18: Workaround for ART verifier bug on KitKat
 	 * 2.19: Remove collection of unused Wi-Fi MAC address and ignore GCM messages from other providers
     */
    //@formatter:on
    public static final String LOCALYTICS_CLIENT_LIBRARY_VERSION = "android_2.19"; //$NON-NLS-1$

    /**
     * The package name of the Localytics library.
     */
    /*
     * Note: This value cannot be changed without significant consequences to the data in the database.
     *
     * To prevent problems with some app trying to use the Localytics package name, an unpublished app has been uploaded to the
     * Android Market under the Localytics account in order to reserve the package name "com.localytics.android".
     */
    public static final String LOCALYTICS_PACKAGE_NAME = "com.localytics.android"; //$NON-NLS-1$

    /**
     * Name of the metadata property in the manifest that represents the app key
     */
    public static final String LOCALYTICS_METADATA_APP_KEY = "LOCALYTICS_APP_KEY"; //$NON-NLS-1$

    /**
     * Name of the metadata property in the manifest that represents the rollup key
     */
    public static final String LOCALYTICS_METADATA_ROLLUP_KEY = "LOCALYTICS_ROLLUP_KEY"; //$NON-NLS-1$
    
    /**
     * Maximum number of sessions to store on disk.
     */
    public static final int MAX_NUM_SESSIONS = 10;

    /**
     * Maximum number of attributes per event session.
     */
    public static final int MAX_NUM_ATTRIBUTES = 50;

    /**
     * The maximum number of custom dimensions reported to the server.
     */
    public static final int MAX_CUSTOM_DIMENSIONS = 10;

    /**
     * Maximum characters in an event name or attribute key/value.
     */
    public static final int MAX_NAME_LENGTH = 128;

    /**
     * Milliseconds after which a session is considered closed and cannot be reattached to.
     * <p>
     * For example, if the user opens an app, presses home, and opens the app again in less than this number of milliseconds, that
     * will count as one session rather than two sessions.
     */
    public static final long SESSION_EXPIRATION = 15 * DateUtils.SECOND_IN_MILLIS;

    /**
     * logcat log tag
     */
    public static final String LOG_TAG = "Localytics"; //$NON-NLS-1$

    /**
     * Boolean indicating whether logcat messages are enabled.
     * <p>
     * Before releasing a production version of an app, this should be set to false for privacy and performance reasons. When
     * logging is enabled, sensitive information such as the device ID may be printed to the log.
     */
    public static final boolean IS_LOGGABLE = false;

    /**
     * Flag indicating whether to use HTTPS for uploads
     */
    public static final boolean USE_HTTPS = false;    
    
    /**
     * Flag indicating whether runtime method parameter checking is performed.
     */
    public static final boolean IS_PARAMETER_CHECKING_ENABLED = true;

    /**
     * Flag indicating whether uncaught exceptions should be suppressed. Analytics are secondary to any other functions performed
     * by an app, which means that analytics should never cause an app to crash.
     * <p>
     * Normally this should be set to true. The only time this should be set to false is during development on the Localytics
     * library itself, in order to detect bugs more quickly.
     */
    public static final boolean IS_EXCEPTION_SUPPRESSION_ENABLED = true;

    /**
     * Flag to control whether device identifiers are uploaded.
     */
    public static final boolean IS_DEVICE_IDENTIFIER_UPLOADED = true;

    /**
     * Cached copy of the current Android API level
     *
     * @see DatapointHelper#getApiLevel()
     */
    /* package */static final int CURRENT_API_LEVEL = DatapointHelper.getApiLevel();

    /**
     * Private constructor prevents instantiation
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private Constants()
    {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
