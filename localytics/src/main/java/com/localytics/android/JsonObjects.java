// @formatter:off
/*
 * JsonObjects.java Copyright (C) 2013 Char Software Inc., DBA Localytics. This code is provided under the Localytics
 * Modified BSD License. A copy of this license has been distributed in a file called LICENSE with this source code. Please visit
 * www.localytics.com for more information.
 */
// @formatter:on

package com.localytics.android;

import android.Manifest.permission;

import org.json.JSONArray;

import com.localytics.android.LocalyticsProvider.ApiKeysDbColumns;

/**
 * Set of constants for building JSON objects that get sent to the Localytics web service.
 */
/* package */final class JsonObjects
{
    /**
     * Private constructor prevents instantiation
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private JsonObjects()
    {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }

    /**
     * Set of constants for the blob header JSON object.
     */
    public static final class BlobHeader
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private BlobHeader()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * Data type for the JSON object.
         *
         * @see #VALUE_DATA_TYPE
         */
        public static final String KEY_DATA_TYPE = "dt"; //$NON-NLS-1$

        /**
         * @see #KEY_DATA_TYPE
         */
        public static final String VALUE_DATA_TYPE = "h"; //$NON-NLS-1$

        /**
         * Timestamp when the app was first launched and the persistent storage was created. Represented as seconds since the Unix
         * Epoch. (Note: This is SECONDS and not milliseconds. This requires care, because Android represents time as
         * milliseconds).
         */
        public static final String KEY_PERSISTENT_STORAGE_CREATION_TIME_SECONDS = "pa"; //$NON-NLS-1$

        /**
         * Sequence number. A monotonically increasing count for each new blob.
         */
        public static final String KEY_SEQUENCE_NUMBER = "seq"; //$NON-NLS-1$

        /**
         * A UUID for the blob.
         */
        public static final String KEY_UNIQUE_ID = "u"; //$NON-NLS-1$

        /**
         * A JSON Object for attributes for the session.
         */
        public static final String KEY_ATTRIBUTES = "attrs"; //$NON-NLS-1$
        
        /**
         * A JSON Object for identifiers
         */
        public static final String KEY_IDENTIFIERS = "ids"; //$NON-NLS-1$

        /**
         * Attributes under {@link BlobHeader#KEY_ATTRIBUTES}
         */
        public static final class Attributes
        {
            /**
             * Private constructor prevents instantiation
             *
             * @throws UnsupportedOperationException because this class cannot be instantiated.
             */
            private Attributes()
            {
                throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
            }

            /**
             * Type: {@code String}
             * <p>
             * Data connection type.
             */
            public static final String KEY_DATA_CONNECTION = "dac"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Version name of the application, taken from the Android Manifest.
             */
            public static final String KEY_CLIENT_APP_VERSION = "av"; //$NON-NLS-1$

            /**
             * Key which maps to the SHA-256 of the device's {@link android.provider.Settings.Secure#ANDROID_ID}.
             */
            public static final String KEY_DEVICE_ANDROID_ID_HASH = "du"; //$NON-NLS-1$

            /**
             * Key which maps to the device's initial {@link android.provider.Settings.Secure#ANDROID_ID}.
             */
            public static final String KEY_DEVICE_ANDROID_ID = "aid"; //$NON-NLS-1$

            /**
             * Key which maps to the device's current {@link android.provider.Settings.Secure#ANDROID_ID}.
             */
            public static final String KEY_CURRENT_ANDROID_ID = "caid"; //$NON-NLS-1$     
            
            /**
             * Type: {@code String}
             * <p>
             */
            public static final String KEY_DEVICE_COUNTRY = "dc"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Manufacturer of the device (e.g. HTC, Samsung, Motorola, Kyocera, etc.)
             */
            public static final String KEY_DEVICE_MANUFACTURER = "dma"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Model of the device (e.g. dream,
             */
            public static final String KEY_DEVICE_MODEL = "dmo"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Android version (e.g. 1.6 or 2.3.4).
             */
            public static final String KEY_DEVICE_OS_VERSION = "dov"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Initial telephony ID of the device, if the device has telephony and the app has {@link permission#READ_PHONE_STATE}.
             * Otherwise null.
             */
            public static final String KEY_DEVICE_TELEPHONY_ID = "tdid"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Current telephony ID of the device, if the device has telephony and the app has {@link permission#READ_PHONE_STATE}.
             * Otherwise null.
             */
            public static final String KEY_CURRENT_TELEPHONY_ID = "ctdid"; //$NON-NLS-1$            
            
            /**
             * Type: {@code String}
             * <p>
             * Platform of the device. For Android devices, this is always "android"
             */
            public static final String KEY_DEVICE_PLATFORM = "dp"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * SHA-256 hash of the device's serial number. Only reported for Android 2.3 or later. Otherwise null.
             */
            public static final String KEY_DEVICE_SERIAL_HASH = "dms"; //$NON-NLS-1$

            /**
             * Type: {@code int}
             * <p>
             * SDK compatibility level of the device.
             *
             * @see android.os.Build.VERSION#SDK
             */
            public static final String KEY_DEVICE_SDK_LEVEL = "dsdk"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * SHA-256 hash of the device's Telephony ID, if the device has telephony and the app has
             * {@link permission#READ_PHONE_STATE}. Otherwise null.
             */
            public static final String KEY_DEVICE_TELEPHONY_ID_HASH = "dtidh"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Country for the device's current locale settings
             */
            public static final String KEY_LOCALE_COUNTRY = "dlc"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Language for the device's current locale settings
             */
            public static final String KEY_LOCALE_LANGUAGE = "dll"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Api key
             */
            public static final String KEY_LOCALYTICS_API_KEY = "au"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Localytics library version
             *
             * @see Constants#LOCALYTICS_CLIENT_LIBRARY_VERSION
             */
            public static final String KEY_LOCALYTICS_CLIENT_LIBRARY_VERSION = "lv"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Data type for the JSON object.
             *
             * @see #VALUE_DATA_TYPE
             */
            public static final String KEY_LOCALYTICS_DATA_TYPE = "dt"; //$NON-NLS-1$

            /**
             * Type {@code String}
             * <p>
             * Installation UUID
             *
             * @see ApiKeysDbColumns#UUID
             */
            public static final String KEY_LOCALYTICS_INSTALLATION_ID = "iu"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Network carrier of the device
             */
            public static final String KEY_NETWORK_CARRIER = "nca"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             */
            public static final String KEY_NETWORK_COUNTRY = "nc"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Google Play attribution referrer string
             */
            public static final String KEY_GOOGLE_PLAY_ATTRIBUTION = "aurl"; //$NON-NLS-1$
            
            /**
             * Type: {@code String}
             * <p>
             * Facebook attribution referrer cookie
             */
            public static final String KEY_FB_COOKIE = "fbat"; //$NON-NLS-1$
            
            /**
             * Type: {@code String}
             * <p>
             * GCM registration id
             */
            public static final String KEY_PUSH_ID = "push"; //$NON-NLS-1$

            /**
             * Type: {@code String}
             * <p>
             * Package name
             */
            public static final String KEY_PACKAGE_NAME = "pkg"; //$NON-NLS-1$            
            
            /**
             * @see #KEY_LOCALYTICS_DATA_TYPE
             */
            @SuppressWarnings("hiding")
            public static final String VALUE_DATA_TYPE = "a"; //$NON-NLS-1$

            /**
             * Value for the platform.
             *
             * @see #KEY_DEVICE_PLATFORM
             */
            public static final String VALUE_PLATFORM = "Android"; //$NON-NLS-1$
        }
        
        public static final class Identifiers
        {
            /**
             * Private constructor prevents instantiation
             *
             * @throws UnsupportedOperationException because this class cannot be instantiated.
             */
            private Identifiers()
            {
                throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
            }

            /**
             * Type: {@code String}
             * <p>
             * Key for identifiers information
             */
            public static final String KEY = "key"; //$NON-NLS-1$
        }
    }

    /**
     * Set of constants for the session open event.
     */
    /* package */static final class SessionOpen
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private SessionOpen()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * Type: {@code String}
         * <p>
         * Data type for the JSON object.
         *
         * @see #VALUE_DATA_TYPE
         */
        public static final String KEY_DATA_TYPE = "dt"; //$NON-NLS-1$

        /**
         * @see #KEY_DATA_TYPE
         */
        public static final String VALUE_DATA_TYPE = "s"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * Epoch timestamp when the session was started in seconds.
         */
        public static final String KEY_WALL_TIME_SECONDS = "ct"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * UUID of the event, which is the same thing as the session UUID
         */
        public static final String KEY_EVENT_UUID = "u"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * Count for the number of sessions
         */
        public static final String KEY_COUNT = "nth"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional first custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_1 = "c0"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional second custom dimension. Can only exist if there is a first custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_2 = "c1"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional third custom dimension. Can only exist if there is a second custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_3 = "c2"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional fourth custom dimension. Can only exist if there is a third custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_4 = "c3"; //$NON-NLS-1$
        
        /**
         * Type: {@code String}
         * <p>
         * Optional fifth custom dimension. Can only exist if there is a fourth custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_5 = "c4"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional sixth custom dimension. Can only exist if there is a fifth custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_6 = "c5"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional seventh custom dimension. Can only exist if there is a sixth custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_7 = "c6"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional eighth custom dimension. Can only exist if there is a seventh custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_8 = "c7"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional ninth custom dimension. Can only exist if there is a eighth custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_9 = "c8"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional tenth custom dimension. Can only exist if there is a ninth custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_10 = "c9"; //$NON-NLS-1$
    }

    /**
     * Set of constants for the session close event.
     */
    /* package */static final class SessionClose
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private SessionClose()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * Type: {@code String}
         * <p>
         * Data type for the JSON object.
         *
         * @see #VALUE_DATA_TYPE
         */
        public static final String KEY_DATA_TYPE = "dt"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * UUID of the event.
         */
        public static final String KEY_EVENT_UUID = "u"; //$NON-NLS-1$

        /**
         * Type: {@code String[]} (technically, a JSON array of strings)
         * <p>
         * Ordered list of flow events that occurred
         */
        public static final String KEY_FLOW_ARRAY = "fl"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * Epoch timestamp when the session was started
         */
        public static final String KEY_SESSION_LENGTH_SECONDS = "ctl"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * Start time of the parent session
         */
        public static final String KEY_SESSION_START_TIME = "ss"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * UUID of the session.
         */
        public static final String KEY_SESSION_UUID = "su"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * Epoch timestamp when the session was started in seconds.
         */
        public static final String KEY_WALL_TIME_SECONDS = "ct"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional first custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_1 = "c0"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional second custom dimension. Can only exist if there is a first custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_2 = "c1"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional third custom dimension. Can only exist if there is a second custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_3 = "c2"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional fourth custom dimension. Can only exist if there is a third custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_4 = "c3"; //$NON-NLS-1$

        /**
         * Data type for close events.
         *
         * @see #KEY_DATA_TYPE
         */
        public static final String VALUE_DATA_TYPE = "c"; //$NON-NLS-1$
    }

    /**
     * Set of constants for the session event event.
     */
    /* package */static final class SessionEvent
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private SessionEvent()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * Type: {@code String}
         * <p>
         * Data type for the JSON object.
         *
         * @see #VALUE_DATA_TYPE
         */
        public static final String KEY_DATA_TYPE = "dt"; //$NON-NLS-1$

        /**
         * Data type for application events.
         *
         * @see #KEY_DATA_TYPE
         */
        public static final String VALUE_DATA_TYPE = "e"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * Epoch timestamp when the session was started in seconds.
         */
        public static final String KEY_WALL_TIME_SECONDS = "ct"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * UUID of the session.
         */
        public static final String KEY_SESSION_UUID = "su"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * UUID of the event.
         */
        public static final String KEY_EVENT_UUID = "u"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Name of the event.
         */
        public static final String KEY_NAME = "n"; //$NON-NLS-1$

        /**
         * Type: {@code JSONObject}.
         * <p>
         * Maps to the attributes of the event.
         * <p>
         * Note that this key is optional. If it is present, it will point to a non-null value representing the attributes of the
         * event. Otherwise the key will not exist, indicating the event had no attributes.
         */
        public static final String KEY_ATTRIBUTES = "attrs"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional first custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_1 = "c0"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional second custom dimension. Can only exist if there is a first custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_2 = "c1"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional third custom dimension. Can only exist if there is a second custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_3 = "c2"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * Optional fourth custom dimension. Can only exist if there is a third custom dimension.
         */
        public static final String KEY_CUSTOM_DIMENSION_4 = "c3"; //$NON-NLS-1$
        
        /**
         * Type: {@code long}
         * <p>
         * Optional customer value increase
         */
        public static final String KEY_CUSTOMER_VALUE_INCREASE = "v"; //$NON-NLS-1$
        
        /**
         * Type: {@code Double}
         * <p>
         * Optional latitude value
         */
        public static final String KEY_LATITUDE = "lat";
        
        /**
         * Type: {@code Double}
         * <p>
         * Optional longitude value
         */
        public static final String KEY_LONGITUDE = "lng";
    }

    /**
     * Set of constants for the session opt in/out event
     */
    /* package */static final class OptEvent
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private OptEvent()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * Type: {@code String}
         * <p>
         * Data type for the JSON object.
         *
         * @see #VALUE_DATA_TYPE
         */
        public static final String KEY_DATA_TYPE = "dt"; //$NON-NLS-1$

        /**
         * Data type for opt in/out events.
         *
         * @see #KEY_DATA_TYPE
         */
        public static final String VALUE_DATA_TYPE = "o"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * Epoch timestamp when the session was started in seconds.
         */
        public static final String KEY_WALL_TIME_SECONDS = "ct"; //$NON-NLS-1$

        /**
         * Type: {@code String}
         * <p>
         * API key
         */
        public static final String KEY_API_KEY = "u"; //$NON-NLS-1$

        /**
         * Type: {@code boolean}
         * <p>
         * True to opt-out. False to opt-in
         */
        public static final String KEY_OPT = "out"; //$NON-NLS-1$
    }

    /**
     * Set of constants for the session flow event.
     */
    /* package */static final class EventFlow
    {
        /**
         * Private constructor prevents instantiation
         *
         * @throws UnsupportedOperationException because this class cannot be instantiated.
         */
        private EventFlow()
        {
            throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
        }

        /**
         * Type: {@code String}
         * <p>
         * Data type for the JSON object.
         *
         * @see #VALUE_DATA_TYPE
         */
        public static final String KEY_DATA_TYPE = "dt"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * UUID of the event, which is the same thing as the session UUID
         */
        public static final String KEY_EVENT_UUID = "u"; //$NON-NLS-1$

        /**
         * Type: {@code long}
         * <p>
         * Start time of the parents session.
         */
        public static final String KEY_SESSION_START_TIME = "ss"; //$NON-NLS-1$

        /**
         * Type: {@code Element[]} (technically a {@link JSONArray} of {@link Element} objects)
         * <p>
         * Ordered set of new flow elements that occurred since the last upload for this session.
         */
        public static final String KEY_FLOW_NEW = "nw"; //$NON-NLS-1$

        /**
         * Type: {@code Element[]} (technically a {@link JSONArray} of {@link Element} objects)
         * <p>
         * Ordered set of old flow elements that occurred during all previous uploads for this session.
         */
        public static final String KEY_FLOW_OLD = "od"; //$NON-NLS-1$

        /**
         * @see #KEY_DATA_TYPE
         */
        public static final String VALUE_DATA_TYPE = "f"; //$NON-NLS-1$

        /**
         * Flow event element that indicates the type and name of the flow event.
         */
        /* package */static final class Element
        {
            /**
             * Private constructor prevents instantiation
             *
             * @throws UnsupportedOperationException because this class cannot be instantiated.
             */
            private Element()
            {
                throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
            }

            /**
             * A flow event that was due to an {@link SessionEvent}.
             */
            public static final String TYPE_EVENT = "e"; //$NON-NLS-1$

            /**
             * A flow event that was due to a screen event.
             */
            public static final String TYPE_SCREEN = "s"; //$NON-NLS-1$
        }
    }
}
