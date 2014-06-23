/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.android;

public class Constants {

  public static final String PACKAGE_NAME = Constants.class.getPackage().getName();

  /**
   * Logging tag
   */
  public static final String TAG = "analytics";

  /**
   * The maximum amount of events to flush at a time
   */
  public static final int MAX_FLUSH = 20;

  public static class Database {

    /**
     * Version 1: uses payload.action
     * Version 2: uses payload.type
     */
    public static final int VERSION = 2;

    public static final String NAME = PACKAGE_NAME;

    public static class PayloadTable {

      public static final String NAME = "payload_table";

      public static final String[] FIELD_NAMES = new String[] {
          Fields.Id.NAME, Fields.Payload.NAME
      };

      public static class Fields {

        public static class Id {

          public static final String NAME = "id";

          /**
           * INTEGER PRIMARY KEY AUTOINCREMENT means index is monotonically
           * increasing, regardless of removals
           */
          public static final String TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT";
        }

        public static class Payload {

          public static final String NAME = "payload";

          public static final String TYPE = " TEXT";
        }
      }
    }
  }

  public class SharedPreferences {
    public static final String ANONYMOUS_ID_KEY = "anonymous.id";
    public static final String USER_ID_KEY = "user.id";
    public static final String GROUP_ID_KEY = "group.id";
  }

  public class Permission {

    public static final String GET_ACCOUNTS = "android.permission.GET_ACCOUNTS";
    public static final String READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
    public static final String FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    public static final String COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    public static final String INTERNET = "android.permission.INTERNET";
    public static final String ACCESS_NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE";
  }
}
