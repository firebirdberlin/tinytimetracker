package com.firebirdberlin.tinytimetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;


public class LogDataSource {
    private SQLiteDatabase database;
    private SQLiteHandler dbHelper;

    public LogDataSource(Context context) {
        dbHelper = new SQLiteHandler(context);
    }


    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long createTrackerEntry(String ssid) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_SSID, ssid);
        long insertId = database.insert(SQLiteHandler.TABLE_TRACKERS, null, values);
        return insertId;
    }

    public long getTrackerID(String ssid) {
        Cursor cursor = null;
        long tracker_id = -1L;
        try{
            cursor = database.rawQuery("SELECT _id FROM trackers WHERE ssid=?", new String[] {ssid + ""});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                tracker_id  = cursor.getLong(cursor.getColumnIndex("_id"));
            }
        }finally {
            cursor.close();
        }
        return tracker_id;
    }

    public long getOrCreateTrackerID(String ssid) {
        long tracker_id = getTrackerID(ssid);
        if (tracker_id == -1L) {
            tracker_id = createTrackerEntry(ssid);
        }
        return tracker_id;
    }

    public long createLogEntry(long tracker_id, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_TRACKER_ID, tracker_id);
        values.put(SQLiteHandler.COLUMN_TIMESTAMP, timestamp);
        long insertId = database.insert(SQLiteHandler.TABLE_LOGS, null, values);
        return insertId;
    }
}
