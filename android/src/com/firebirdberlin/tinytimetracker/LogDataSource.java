package com.firebirdberlin.tinytimetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;
import de.greenrobot.event.EventBus;
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

    private void init() {
        if (database == null) {
            open();
        }
    }

    public void close() {
        dbHelper.close();
    }

    public TrackerEntry createTracker(String name, String method) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_NAME, name);
        values.put(SQLiteHandler.COLUMN_METHOD, method);
        long insertId = database.insert(SQLiteHandler.TABLE_TRACKERS, null, values);
        TrackerEntry tracker = new TrackerEntry(insertId, name, method);
        EventBus bus = EventBus.getDefault();
        bus.post(new OnTrackerAdded(tracker));
        return tracker;
    }

    public Set<String> getTrackedSSIDs(String method) {
        init();
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
        init();
        Cursor cursor = null;
        List<TrackerEntry> entries = new ArrayList<TrackerEntry>();
        try{
            cursor = database.rawQuery("SELECT _id, name FROM trackers",
                                       new String[] {});

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                TrackerEntry entry = new TrackerEntry(cursor.getLong(0),
                                                      cursor.getString(1),
                                                      "WLAN");
                entries.add(entry);

                cursor.moveToNext();
            }
        }finally {
            cursor.close();
        }
        return entries;

    }

    public TrackerEntry getTracker(long id) {
        init();
        Cursor cursor = null;
        TrackerEntry entry = null;
        try{
            cursor = database.rawQuery("SELECT _id, name FROM trackers WHERE _id=?",
                                       new String[] {String.valueOf(id)});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                entry = new TrackerEntry(cursor.getLong(0),
                                         cursor.getString(1),
                                         "WLAN");
            }
        }finally {
            cursor.close();
        }
        return entry;

    }


    public TrackerEntry getTracker(String name) {
        init();
        Cursor cursor = null;
        TrackerEntry entry = null;
        try{
            cursor = database.rawQuery("SELECT _id, name FROM trackers WHERE name=?",
                                       new String[] {name});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                entry = new TrackerEntry(cursor.getLong(0),
                                         cursor.getString(1),
                                         "WLAN");
            }
        }finally {
            cursor.close();
        }
        return entry;

    }

    public long getTrackerID(String name, String method) {
        init();
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

    public TrackerEntry getOrCreateTracker(String name, String method) {
        TrackerEntry tracker = getTracker(name);
        if (tracker == null) {
            tracker = createTracker(name, method);
        }
        return tracker;
    }

    public boolean deleteTracker(long id) {
        TrackerEntry tracker = getTracker(id);
        init();
        database.delete(SQLiteHandler.TABLE_LOGS, "tracker_id=?", new String[] {String.valueOf(id)});
        int rows_affected = database.delete(SQLiteHandler.TABLE_TRACKERS, "_id=?", new String[] {String.valueOf(id)});
        EventBus bus = EventBus.getDefault();
        bus.post(new OnTrackerDeleted(tracker));
        return rows_affected > 0;
    }


    public LogEntry createLogEntry(long tracker_id, long timestamp_start, long timestamp_end) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_TRACKER_ID, tracker_id);
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_START, timestamp_start);
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_END, timestamp_end);
        long insertId = database.insert(SQLiteHandler.TABLE_LOGS, null, values);
        return new LogEntry(insertId, tracker_id, timestamp_start, timestamp_end);
    }

    public LogEntry replaceLogEntry(LogEntry log_entry) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_ID, log_entry.getID());
        values.put(SQLiteHandler.COLUMN_TRACKER_ID, log_entry.getTrackerID());
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_START, log_entry.getTimestampStart());
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_END, log_entry.getTimestampEnd());
        long log_id = database.replace(SQLiteHandler.TABLE_LOGS, null, values);
        return log_entry;
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
            return getTotalDurationAggregated(tracker_id, aggregation_type, -1L);
    }

    public List< Pair<Long,Long> > getTotalDurationAggregated(long tracker_id, int aggregation_type, long limit) {
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

        String limit_str = "";
        if ( limit > 0 ) {
            limit_str = " LIMIT " + String.valueOf(limit);
        }

        Cursor cursor = null;
        List< Pair<Long,Long> > results = new ArrayList< Pair<Long, Long> >();
        try{
            cursor = database.rawQuery("SELECT timestamp_start, "
                                       + "SUM(timestamp_end - timestamp_start) FROM logs "
                                       + "WHERE tracker_id=? GROUP BY " + group_by
                                       + " ORDER BY timestamp_start DESC"
                                       + limit_str,
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

    public LogEntry addTimeStamp(TrackerEntry tracker, long timestamp, long seconds_connection_lost){
        if (tracker == null) return null;

        long tracker_id = tracker.getID();
        Log.i(TAG, "addTimestamp(" + String.valueOf(tracker_id) + ", " + String.valueOf(timestamp) + ", " + String.valueOf(seconds_connection_lost) + ")");
        LogEntry log = getLatestLogEntry(tracker_id);
        if (log != null) {
            long cmp_time = timestamp - 1000 * seconds_connection_lost;
            if (log.getTimestampEnd() >= cmp_time) {
                log.setTimestampEnd(timestamp);
                replaceLogEntry(log);
                return log;
            }
        }

        // make a new entry
        return createLogEntry(tracker_id, timestamp, timestamp);
    }
}
