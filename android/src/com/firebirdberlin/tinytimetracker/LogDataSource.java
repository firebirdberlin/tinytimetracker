package com.firebirdberlin.tinytimetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class LogDataSource {
    private static String TAG = TinyTimeTracker.TAG + ".LogDataSource";
    private Context mContext = null;
    private SQLiteDatabase database = null;
    private SQLiteHandler dbHelper;

    public LogDataSource(Context context) {
        mContext = context;
        dbHelper = new SQLiteHandler(context);
    }


    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long createTrackerEntry(String name, String method) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_NAME, name);
        values.put(SQLiteHandler.COLUMN_METHOD, method);
        long insertId = database.insert(SQLiteHandler.TABLE_TRACKERS, null, values);
        return insertId;
    }

    public long getTrackerID(String name, String method) {
        if (database == null) {
            open();
        }
        Cursor cursor = null;
        long tracker_id = -1L;
        try{
            cursor = database.rawQuery("SELECT _id FROM trackers WHERE name=? AND method=?",
                                       new String[] {name, method});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                tracker_id  = cursor.getLong(cursor.getColumnIndex(SQLiteHandler.COLUMN_ID));
            }
        }finally {
            cursor.close();
        }
        return tracker_id;
    }

    public long getOrCreateTrackerID(String name, String method) {
        long tracker_id = getTrackerID(name, method);
        if (tracker_id == -1L) {
            tracker_id = createTrackerEntry(name, method);
        }
        return tracker_id;
    }

    public long createLogEntry(long tracker_id, long timestamp_start, long timestamp_end) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_TRACKER_ID, tracker_id);
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_START, timestamp_start);
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_END, timestamp_end);
        long insertId = database.insert(SQLiteHandler.TABLE_LOGS, null, values);
        return insertId;
    }

    public long replaceLogEntry(LogEntry log_entry) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_ID, log_entry.getID());
        values.put(SQLiteHandler.COLUMN_TRACKER_ID, log_entry.getTrackerID());
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_START, log_entry.getTimestampStart());
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_END, log_entry.getTimestampEnd());
        long log_id = database.replace(SQLiteHandler.TABLE_LOGS, null, values);
        return log_id;
    }

    public List<LogEntry> getAllEntries(String name) {
        long tracker_id = getTrackerID(name, "WLAN");
        return getAllEntries(tracker_id);
    }

    public List<LogEntry> getAllEntries(long tracker_id) {
        List<LogEntry> entries = new ArrayList<LogEntry>();
        Cursor cursor = null;

        cursor = database.rawQuery("SELECT _id, timestamp_start, timestamp_end FROM logs "
                                   + "WHERE tracker_id=? ORDER BY timestamp_start DESC",
                                   new String[] {String.valueOf(tracker_id)});

        Log.d(TAG, String.valueOf(cursor.getCount()) + " results");
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            long log_id = cursor.getLong(0);
            long time_start = cursor.getLong(1);
            long time_end = cursor.getLong(2);
            LogEntry logEntry = new LogEntry(log_id, tracker_id, time_start, time_end);
            entries.add(logEntry);

            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return entries;
    }

    public LogEntry getLatestLogEntry(long tracker_id) {
        Cursor cursor = null;
        LogEntry log = null;
        try{
            cursor = database.rawQuery("SELECT _id, timestamp_start, timestamp_end FROM logs "
                                       + "WHERE tracker_id=? ORDER BY timestamp_start DESC LIMIT 1",
                                       new String[] {String.valueOf(tracker_id)});

            Log.d(TAG, String.valueOf(cursor.getCount()) + " results");
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                long log_id = cursor.getLong(0);
                long time_start = cursor.getLong(1);
                long time_end = cursor.getLong(2);
                log = new LogEntry(log_id, tracker_id, time_start, time_end);
            }
        }finally {
            cursor.close();
        }
        return log;
    }

    public long getTotalDurationSince(long timestamp, long tracker_id) {
        Cursor cursor = null;
        long duration_seconds = 0;
        try{
            cursor = database.rawQuery("SELECT SUM(timestamp_end - timestamp_start) FROM logs "
                                       + "WHERE tracker_id=? and timestamp_start>=?",
                                       new String[] {String.valueOf(tracker_id),
                                                     String.valueOf(timestamp)});

            Log.d(TAG, String.valueOf(cursor.getCount()) + " results");
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                Log.d(TAG, String.valueOf(cursor.getLong(0)) + " ms");
                duration_seconds = cursor.getLong(0) / 1000L;
            }
        }finally {
            cursor.close();
        }
        return duration_seconds;
    }

    public long addTimeStamp(long tracker_id, long timestamp, long seconds_connection_lost){
        Log.i(TAG, "addTimestamp(" + String.valueOf(tracker_id) + ", " + String.valueOf(timestamp) + ", " + String.valueOf(seconds_connection_lost) + ")");
        LogEntry log = getLatestLogEntry(tracker_id);
        if (log != null) {
            long cmp_time = timestamp - 1000 * seconds_connection_lost;
            if (log.getTimestampEnd() >= cmp_time) {
                log.setTimestampEnd(timestamp);
                return replaceLogEntry(log);
            }
        }

        // make a new entry
        return createLogEntry(tracker_id, timestamp, timestamp);
    }
}
