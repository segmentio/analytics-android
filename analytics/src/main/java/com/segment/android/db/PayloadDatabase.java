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

package com.segment.android.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Pair;
import com.segment.android.Analytics;
import com.segment.android.Constants;
import com.segment.android.Logger;
import com.segment.android.models.BasePayload;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class PayloadDatabase extends SQLiteOpenHelper {

  //
  // Singleton
  //

  private static PayloadDatabase instance;

  public static PayloadDatabase getInstance(Context context) {
    if (instance == null) {
      instance = new PayloadDatabase(context);
    }

    return instance;
  }

  //
  // Instance
  //

  /**
   * Caches the count of the database without requiring SQL count to be
   * called every time. This will allow us to quickly determine whether
   * our database is full and we shouldn't add anymore
   */
  private AtomicLong count;
  private boolean initialCount;

  private IPayloadSerializer serializer = new JsonPayloadSerializer();

  private PayloadDatabase(Context context) {
    super(context, Constants.Database.NAME, null, Constants.Database.VERSION);

    this.count = new AtomicLong();
  }

  @Override
  public void onCreate(SQLiteDatabase db) {

    String sql = String.format("CREATE TABLE IF NOT EXISTS %s (%s %s, %s %s);",
        Constants.Database.PayloadTable.NAME,

        Constants.Database.PayloadTable.Fields.Id.NAME,
        Constants.Database.PayloadTable.Fields.Id.TYPE,

        Constants.Database.PayloadTable.Fields.Payload.NAME,
        Constants.Database.PayloadTable.Fields.Payload.TYPE);
    try {
      db.execSQL(sql);
    } catch (SQLException e) {
      Logger.e(e, "Failed to create Segment SQL lite database");
    }
  }

  @Override
  public void onOpen(SQLiteDatabase db) {
    super.onOpen(db);
  }

  /**
   * Counts the size of the current database and sets the cached counter
   *
   * This shouldn't be called onOpen() or onCreate() because it will cause
   * a recursive database get.
   */
  private void ensureCount() {
    if (!initialCount) {
      count.set(countRows());
      initialCount = true;
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // migration from db version, 1->2, we want to drop all the existing items to prevent crashes
    if (oldVersion == 1) removeEvents();
  }

  /**
   * Adds a payload to the database
   */
  public boolean addPayload(BasePayload payload) {

    ensureCount();

    long rowCount = getRowCount();
    final int maxQueueSize = Analytics.getOptions().getMaxQueueSize();
    if (rowCount >= maxQueueSize) {
      Logger.w("Cant add action, the database is larger than max queue size (%d).", maxQueueSize);
      return false;
    }

    boolean success = false;

    String json = serializer.serialize(payload);

    synchronized (this) {

      SQLiteDatabase db = null;

      try {

        db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(Constants.Database.PayloadTable.Fields.Payload.NAME, json);

        long result = db.insert(Constants.Database.PayloadTable.NAME, null, contentValues);

        if (result == -1) {
          Logger.w("Database insert failed. Result: %s", result);
        } else {
          success = true;
          // increase the row count
          count.addAndGet(1);
        }
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to open or write to Segment payload db");
      } finally {
        if (db != null) db.close();
      }

      return success;
    }
  }

  /**
   * Fetches the total amount of rows in the database
   */
  private long countRows() {

    String sql = String.format("SELECT COUNT(*) FROM %s", Constants.Database.PayloadTable.NAME);

    long numberRows = 0;

    SQLiteDatabase db = null;

    synchronized (this) {

      try {
        db = getWritableDatabase();
        SQLiteStatement statement = db.compileStatement(sql);
        numberRows = statement.simpleQueryForLong();
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to ensure row count in the Segment payload db");
      } finally {
        if (db != null) db.close();
      }
    }

    return numberRows;
  }

  /**
   * Fetches the total amount of rows in the database without
   * an actual database query, using a cached counter.
   */
  public long getRowCount() {
    if (!initialCount) ensureCount();
    return count.get();
  }

  /**
   * Get the next (limit) events from the database
   */
  public List<Pair<Long, BasePayload>> getEvents(int limit) {

    List<Pair<Long, BasePayload>> result = new LinkedList<Pair<Long, BasePayload>>();

    SQLiteDatabase db = null;
    Cursor cursor = null;

    synchronized (this) {

      try {

        db = getWritableDatabase();

        String table = Constants.Database.PayloadTable.NAME;
        String[] columns = Constants.Database.PayloadTable.FIELD_NAMES;
        String selection = null;
        String selectionArgs[] = null;
        String groupBy = null;
        String having = null;
        String orderBy = Constants.Database.PayloadTable.Fields.Id.NAME + " ASC";
        String limitBy = "" + limit;

        cursor =
            db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limitBy);

        while (cursor.moveToNext()) {
          long id = cursor.getLong(0);
          String json = cursor.getString(1);

          BasePayload payload = serializer.deserialize(json);

          if (payload != null) result.add(new Pair<Long, BasePayload>(id, payload));
        }
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to open or read from the Segment payload db");
      } finally {
        try {
          if (cursor != null) cursor.close();
          if (db != null) db.close();
        } catch (SQLiteException e) {
          Logger.e(e, "Failed to close db cursor");
        }
      }
    }

    return result;
  }

  /**
   * Remove these events from the database
   */
  @SuppressLint("DefaultLocale")
  public int removeEvents(long minId, long maxId) {
    ensureCount();

    SQLiteDatabase db = null;

    String idFieldName = Constants.Database.PayloadTable.Fields.Id.NAME;

    String filter = String.format("%s >= %d AND %s <= %d", idFieldName, minId, idFieldName, maxId);

    int deleted = -1;

    synchronized (this) {
      try {
        db = getWritableDatabase();
        deleted = db.delete(Constants.Database.PayloadTable.NAME, filter, null);
        // decrement the row counter
        count.addAndGet(-deleted);
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to remove items from the Segment payload db");
      } finally {
        if (db != null) db.close();
      }
    }

    return deleted;
  }

  /**
   * Remove all events from the database
   */
  @SuppressLint("DefaultLocale")
  public int removeEvents() {
    ensureCount();
    SQLiteDatabase db = null;
    int deleted = -1;

    synchronized (this) {
      try {
        db = getWritableDatabase();
        deleted = db.delete(Constants.Database.PayloadTable.NAME, null, null);
        // decrement the row counter
        count.addAndGet(-deleted);
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to remove all items from the Segment payload db");
      } finally {
        if (db != null) db.close();
      }
    }

    Logger.d("Removed all %d event items from the Segment payload db.", deleted);
    return deleted;
  }
}