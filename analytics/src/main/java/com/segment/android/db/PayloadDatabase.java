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
import android.util.Log;
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
      Logger.e("Failed to create Segment SQL lite database: " + Log.getStackTraceString(e));
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
    if (rowCount >= Analytics.getOptions().getMaxQueueSize()) {
      Logger.w("Cant add action, the database is larger than max queue size.");
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
          Logger.w("Database insert failed. Result: " + result);
        } else {
          success = true;
          // increase the row count
          count.addAndGet(1);
        }
      } catch (SQLiteException e) {

        Logger.e("Failed to open or write to Segment payload db: " + Log.getStackTraceString(e));
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
        Logger.e(
            "Failed to ensure row count in the Segment payload db: " + Log.getStackTraceString(e));
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

          BasePayload payload = serializer.deseralize(json);

          if (payload != null) result.add(new Pair<Long, BasePayload>(id, payload));
        }
      } catch (SQLiteException e) {

        Logger.e(
            "Failed to open or read from the Segment payload db: " + Log.getStackTraceString(e));
      } finally {
        try {
          if (cursor != null) cursor.close();
          if (db != null) db.close();
        } catch (SQLiteException e) {
          Logger.e("Failed to close db cursor: " + Log.getStackTraceString(e));
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

    String ID_FIELD_NAME = Constants.Database.PayloadTable.Fields.Id.NAME;

    String filter =
        String.format("%s >= %d AND %s <= %d", ID_FIELD_NAME, minId, ID_FIELD_NAME, maxId);

    int deleted = -1;

    synchronized (this) {
      try {
        db = getWritableDatabase();
        deleted = db.delete(Constants.Database.PayloadTable.NAME, filter, null);
        // decrement the row counter
        count.addAndGet(-deleted);
      } catch (SQLiteException e) {
        Logger.e(
            "Failed to remove items from the Segment payload db: " + Log.getStackTraceString(e));
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
        Logger.e(
            "Failed to remove all items from the Segment payload db: " + Log.getStackTraceString(
                e));
      } finally {
        if (db != null) db.close();
      }
    }

    Logger.i("Removed all " + deleted + " event items from the Segment payload db.");
    return deleted;
  }
}