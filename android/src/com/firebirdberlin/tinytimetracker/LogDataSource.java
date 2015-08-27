package com.firebirdberlin.tinytimetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class LogDataSource {
    private static String TAG = TinyTimeTracker.TAG + ".LogDataSource";
    public static final int AGGRETATION_DAY = 0;
    public static final int AGGRETATION_WEEK = 1;
    public static final int AGGRETATION_YEAR = 2;
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

    public Set<String> getTrackedSSIDs(String method) {
        if (database == null) {
            open();
        }
        Cursor cursor = null;
        Set<String> names = new HashSet<String>();
        try{
            cursor = database.rawQuery("SELECT name FROM trackers WHERE method=?",
                                       new String[] {method});

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(0);
                names.add(name);
                cursor.moveToNext();
            }
        }finally {
            cursor.close();
        }
        return names;
    }

    public List<TrackerEntry> getTrackers() {
        if (database == null) {
            open();
        }
        Cursor cursor = null;
        List<TrackerEntry> entries = new ArrayList<TrackerEntry>();
        try{
            cursor = database.rawQuery("SELECT _id, name FROM trackers",
                                       new String[] {});

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                TrackerEntry entry = new TrackerEntry();
                entry.setID(cursor.getLong(0));
                entry.setSSID(cursor.getString(1));

                entries.add(entry);

                cursor.moveToNext();
            }
        }finally {
            cursor.close();
        }
        return entries;

    }

    public TrackerEntry getTracker(String name) {
        if (database == null) {
            open();
        }
        Cursor cursor = null;
        TrackerEntry entry = new TrackerEntry();
        try{
            cursor = database.rawQuery("SELECT _id, name FROM trackers WHERE name=?",
                                       new String[] {name});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                entry.setID(cursor.getLong(0));
                entry.setSSID(cursor.getString(1));
            }
        }finally {
            cursor.close();
        }
        return entry;

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
                                   + "WHERE tracker_id=? ORDER BY timestamp_start DESC LIMIT 500",
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

    public List< Pair<Long,Long> > getTotalDurationAggregated(long tracker_id, int aggregation_type) {
        String group_by = "";
        switch (aggregation_type) {
            case AGGRETATION_DAY:
            default:
                group_by = "strftime('%Y-%m-%d', timestamp_start/1000, 'unixepoch', 'localtime')";
                break;
            case AGGRETATION_WEEK:
                group_by = "strftime('%Y-%W', timestamp_start/1000, 'unixepoch', 'localtime')";
                break;
            case AGGRETATION_YEAR:
                group_by = "strftime('%Y', timestamp_start/1000, 'unixepoch', 'localtime')";
                break;
        }

        Cursor cursor = null;
        List< Pair<Long,Long> > results = new ArrayList< Pair<Long, Long> >();
        try{
            cursor = database.rawQuery("SELECT timestamp_start, "
                                       + "SUM(timestamp_end - timestamp_start) FROM logs "
                                       + "WHERE tracker_id=? GROUP BY " + group_by
                                       + " ORDER BY timestamp_start DESC",
                                       new String[] {String.valueOf(tracker_id)});

            Log.d(TAG, String.valueOf(cursor.getCount()) + " results");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                long timestamp = cursor.getLong(0);
                long duration_millis = cursor.getLong(1);
                Log.d(TAG, String.valueOf(timestamp) + " : " + String.valueOf(duration_millis) + " s");
                results.add(new Pair(timestamp, duration_millis));

                cursor.moveToNext();
            }
        }finally {
            cursor.close();
        }
        return results;
    }

    public UnixTimestamp getTotalDurationSince(long timestamp, long tracker_id) {
        Cursor cursor = null;
        long duration_millis = 0;
        try{
            cursor = database.rawQuery("SELECT SUM(timestamp_end - timestamp_start) FROM logs "
                                       + "WHERE tracker_id=? and timestamp_start>=?",
                                       new String[] {String.valueOf(tracker_id),
                                                     String.valueOf(timestamp)});

            Log.d(TAG, String.valueOf(cursor.getCount()) + " results");
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                Log.d(TAG, String.valueOf(cursor.getLong(0)) + " ms");
                duration_millis = cursor.getLong(0);
            }
        }finally {
            cursor.close();
        }
        return new UnixTimestamp(duration_millis);
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
