package io.segment.android.db;

import io.segment.android.Constants;
import io.segment.android.models.BasePayload;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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

public class PayloadDatabase extends SQLiteOpenHelper {

	private static final String TAG = SQLiteOpenHelper.class.getName();
	
	/**
	 * Singleton
	 */
	private static PayloadDatabase instance;
	
	/**
	 * Singleton getter
	 * @param context
	 * @return
	 */
	static PayloadDatabase getInstance(Context context) {
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
	private IPayloadSerializer serializer = new JsonPayloadSerializer();

	private PayloadDatabase(Context context) {
		super(context, Constants.Database.NAME, null,
				Constants.Database.VERSION);
		
		count = new AtomicLong();
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
			Log.e(TAG, "Failed to create Segment.io SQL lite database: " + 
						Log.getStackTraceString(e));
		}
		
		countSize();
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		countSize();
	}

	/**
	 * Counts the size of the current database and sets the cached counter
	 */
	private void countSize() {
		count.set(getNumberRows());
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// do nothing here
	}

	/**
	 * Adds a payload to the database
	 * @param payload 
	 */
	void addPayload(BasePayload payload) {
		
		String json = serializer.serialize(payload);
		
		synchronized (this) {
			
			SQLiteDatabase db = null;
			
			try {
				
				db = getWritableDatabase();
				ContentValues contentValues = new ContentValues();
				
				contentValues.put(
						Constants.Database.PayloadTable.Fields.Payload.NAME, 
						json);
				
				long result = db.insert(Constants.Database.PayloadTable.NAME, null,
						contentValues);
				
				if (result == -1) {
					Log.w(TAG, "Database insert failed. Result: " + result);
				}
				
			} catch (SQLiteException e) {
				
				Log.e(TAG, "Failed to open or write to Segment.io payload db: " + 
						Log.getStackTraceString(e));
				
			} finally {
				if (db != null)
					db.close();
			}
			
		}
	}

	/**
	 * Fetchs the total amount of rows in the database
	 * @return
	 */
	long getNumberRows() {
		
		String sql = String.format("SELECT COUNT(*) FROM %s", 
				Constants.Database.PayloadTable.NAME);
		
		SQLiteDatabase db = getWritableDatabase();
		SQLiteStatement statement = db.compileStatement(sql);
		long numberRows = statement.simpleQueryForLong();
		db.close();
		
		return numberRows;
	}

	/**
	 * Get the next (limit) events from the database
	 * @param limit
	 * @return
	 */
	List<Pair<Long, BasePayload>> getEvents(int limit) {

		List<Pair<Long, BasePayload>> result = 
				new LinkedList<Pair<Long, BasePayload>>();
		
		SQLiteDatabase db = null;
		Cursor cursor = null;
		
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
			
			cursor = db.query(table, columns, selection, selectionArgs, 
					groupBy, having, orderBy, limitBy);
		
			while (cursor.moveToNext()) {
				long id = cursor.getLong(0);
				String json = cursor.getString(1);
	
				BasePayload payload = serializer.deseralize(json);
				
				if (payload != null) 
					result.add(new Pair<Long, BasePayload>(id, payload));
			}
		} catch (SQLiteException e) {

			Log.e(TAG, "Failed to open or read from the Segment.io payload db: " + 
					Log.getStackTraceString(e));
			
		} finally {
			if (db != null) db.close();
			if (cursor != null) cursor.close();
		}
	
		return result;
	}

	/**
	 * Remove these events from the database
	 * @param maxId
	 */
	void removeEvents(long maxId) {
		
		SQLiteDatabase db = null;

		String filter = Constants.Database.PayloadTable.Fields.Id.NAME + " <= " + maxId;
				
		try {
			db = getWritableDatabase();
			db.delete(Constants.Database.PayloadTable.NAME, filter, null);
			
		} catch (SQLiteException e) {

			Log.e(TAG, "Failed to remove items from the Segment.io payload db: " + 
					Log.getStackTraceString(e));
			
		} finally {
			if (db != null) db.close();
		}
	}
	
}