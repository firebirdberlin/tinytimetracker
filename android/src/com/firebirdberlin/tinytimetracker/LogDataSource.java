package com.firebirdberlin.tinytimetracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;
import de.greenrobot.event.EventBus;

import com.firebirdberlin.tinytimetracker.events.OnLogEntryChanged;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerAdded;
import com.firebirdberlin.tinytimetracker.events.OnTrackerChanged;
import com.firebirdberlin.tinytimetracker.events.OnTrackerDeleted;
import com.firebirdberlin.tinytimetracker.models.AccessPoint;
import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;


public class LogDataSource {
    private static String TAG = TinyTimeTracker.TAG + ".LogDataSource";
    public static final int AGGRETATION_DAY = 0;
    public static final int AGGRETATION_WEEK = 1;
    public static final int AGGRETATION_YEAR = 2;
    private Context mContext = null;
    private SQLiteDatabase database = null;
    private SQLiteHandler dbHelper;
    private EventBus bus = EventBus.getDefault();

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

    public TrackerEntry save(TrackerEntry tracker) {
        init();
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_NAME, tracker.ssid);
        values.put(SQLiteHandler.COLUMN_METHOD, tracker.method);
        values.put(SQLiteHandler.COLUMN_VERBOSE, tracker.verbose_name);
        values.put(SQLiteHandler.COLUMN_WORKING_HOURS, tracker.working_hours);
        values.put(SQLiteHandler.COLUMN_OPERATION_STATE, tracker.operation_state);

        if (tracker.id == TrackerEntry.NOT_SAVED) {
            long id = database.insert(SQLiteHandler.TABLE_TRACKERS, null, values);
            tracker.id = id;
            bus.postSticky(new OnTrackerAdded(tracker));
        }
        else {
            values.put(SQLiteHandler.COLUMN_ID, tracker.id);
            database.replace(SQLiteHandler.TABLE_TRACKERS, null, values);
            bus.post(new OnTrackerChanged(tracker));
        }

        return tracker;
    }

    public AccessPoint save(AccessPoint accessPoint) {
        init();

        if (accessPoint.getTrackerID() == accessPoint.NOT_SAVED) {
            return accessPoint;
        }

        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_SSID, accessPoint.ssid);
        values.put(SQLiteHandler.COLUMN_BSSID, accessPoint.bssid);
        values.put(SQLiteHandler.COLUMN_TRACKER_ID, accessPoint.getTrackerID());

        if (accessPoint.getID() == AccessPoint.NOT_SAVED) {
            long id = database.insert(SQLiteHandler.TABLE_ACCESS_POINTS, null, values);
            accessPoint.setID(id);
        }
        else {
            values.put(SQLiteHandler.COLUMN_ID, accessPoint.getID());
            database.replace(SQLiteHandler.TABLE_ACCESS_POINTS, null, values);
        }

        return accessPoint;
    }

    public LogEntry save(LogEntry log_entry) {
        init();
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_ID, log_entry.getID());
        values.put(SQLiteHandler.COLUMN_TRACKER_ID, log_entry.getTrackerID());
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_START, log_entry.getTimestampStart());
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_END, log_entry.getTimestampEnd());

        if (log_entry.id == LogEntry.NOT_SAVED) {
            long id = database.insert(SQLiteHandler.TABLE_LOGS, null, values);
            log_entry.id = id;
        }
        else {
            values.put(SQLiteHandler.COLUMN_ID, log_entry.id);
            database.replace(SQLiteHandler.TABLE_LOGS, null, values);
            bus.post(new OnLogEntryChanged(log_entry));
        }

        return log_entry;
    }

    public Set<String> getTrackedSSIDs(String method) {
        init();
        Cursor cursor = null;
        Set<String> names = new HashSet<String>();

        try {
            cursor = database.rawQuery("SELECT name FROM trackers WHERE method=?",
                                       new String[] {method});
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                String name = cursor.getString(0);
                names.add(name);
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return names;
    }

    public Set<String> getTrackedBSSIDs() {
        init();
        Cursor cursor = null;
        Set<String> bssids = new HashSet<String>();

        try {
            cursor = database.query(SQLiteHandler.TABLE_ACCESS_POINTS, new String[] {"bssid"},
                                    null, null, null, null, null, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                bssids.add(cursor.getString(0));
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return bssids;
    }

    public List<TrackerEntry> getTrackers() {
        init();
        Cursor cursor = null;
        List<TrackerEntry> entries = new ArrayList<TrackerEntry>();

        try {
            cursor = database.rawQuery("SELECT _id, name, verbose_name, method, working_hours, operation_state FROM trackers",
                                       new String[] {});
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                TrackerEntry entry = getTrackerEntryFromCursor(cursor);
                entries.add(entry);
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return entries;
    }

    public Set<TrackerEntry> getTrackersInManualMode() {
        Set<TrackerEntry> trackers = new HashSet<TrackerEntry>();
        List<TrackerEntry> allTrackers = getTrackers();
        for (TrackerEntry tracker : allTrackers) {
            if (tracker.operation_state == TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE
                    || tracker.operation_state == TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI ) {
                trackers.add(tracker);
            }
        }
        return trackers;
    }

    public void updateTrackerInManualMode(TrackerEntry tracker) {
        if (tracker.operation_state != TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE
                && tracker.operation_state != TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI ) {
            return;
        }
        LogEntry logEntry = getLatestLogEntry(tracker.id);
        long now = System.currentTimeMillis();
        logEntry.setTimestampEnd(now);
        save(logEntry);
    }

    public TrackerEntry getTracker(long id) {
        init();
        Cursor cursor = null;
        TrackerEntry entry = null;

        try {
            cursor = database.rawQuery("SELECT _id, name, verbose_name, method, working_hours, operation_state FROM trackers WHERE _id=?",
                                       new String[] {String.valueOf(id)});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                entry = getTrackerEntryFromCursor(cursor);
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return entry;
    }

    public TrackerEntry getTracker(String verbose_name) {
        init();
        Cursor cursor = null;
        TrackerEntry entry = null;

        try {
            cursor = database.rawQuery("SELECT _id, name, verbose_name, method, working_hours, operation_state FROM trackers WHERE verbose_name=?",
                                       new String[] {verbose_name});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                entry = getTrackerEntryFromCursor(cursor);
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return entry;
    }

    public TrackerEntry getTrackerBySSID(String name) {
        init();
        Cursor cursor = null;
        TrackerEntry entry = null;

        try {
            cursor = database.rawQuery("SELECT _id, name, verbose_name, method, working_hours, operation_state FROM trackers WHERE name=?",
                                       new String[] {name});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                entry = getTrackerEntryFromCursor(cursor);
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return entry;
    }

    public Set<TrackerEntry> getTrackersByBSSID(String bssid) {
        final String query = "SELECT trackers._id, name, verbose_name, method, working_hours, operation_state FROM trackers " +
                             "INNER JOIN access_points ON trackers._id=access_points.tracker_id " +
                             "WHERE access_points.bssid=?";
        init();
        Set<TrackerEntry> entries = new HashSet<TrackerEntry>();
        Cursor cursor = database.rawQuery(query, new String[] {bssid});

        try {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                TrackerEntry entry = getTrackerEntryFromCursor(cursor);
                entries.add(entry);
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return entries;
    }

    private TrackerEntry getTrackerEntryFromCursor(Cursor cursor) {
        TrackerEntry entry = new TrackerEntry();
        entry.id = cursor.getLong(0);
        entry.ssid = cursor.getString(1);
        entry.verbose_name = cursor.getString(2);
        entry.method = cursor.getString(3);
        entry.working_hours = cursor.getFloat(4);
        entry.operation_state = cursor.getInt(5);
        return entry;
    }

    public long getTrackerID(String name, String method) {
        init();
        Cursor cursor = null;
        long tracker_id = -1L;

        try {
            cursor = database.rawQuery("SELECT _id FROM trackers WHERE name=? AND method=?",
                                       new String[] {name, method});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                tracker_id  = cursor.getLong(cursor.getColumnIndex(SQLiteHandler.COLUMN_ID));
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return tracker_id;
    }

    public boolean delete(TrackerEntry tracker) {
        if (tracker == null) {
            return false;
        }
        long id = tracker.id;
        if (id == TrackerEntry.NOT_SAVED) {
            return false;
        }

        init();
        database.delete(SQLiteHandler.TABLE_LOGS, "tracker_id=?", new String[] {String.valueOf(id)});
        database.delete(SQLiteHandler.TABLE_ACCESS_POINTS, "tracker_id=?", new String[] {String.valueOf(id)});
        int rows_affected = database.delete(SQLiteHandler.TABLE_TRACKERS, "_id=?", new String[] {String.valueOf(id)});
        boolean success = (rows_affected > 0);

        if (success) {
            bus.post(new OnTrackerDeleted(tracker));
        }

        return success;
    }

    public boolean delete(AccessPoint accessPoint) {
        if (accessPoint == null) {
            return false;
        }

        long id = accessPoint.getID();

        if (id == AccessPoint.NOT_SAVED) {
            return false;
        }

        init();
        int rows_affected = database.delete(SQLiteHandler.TABLE_ACCESS_POINTS, "_id=?",
                                            new String[] {String.valueOf(id)});
        return (rows_affected > 0);
    }

    public boolean delete(LogEntry logEntry) {
        if (logEntry == null) {
            return false;
        }

        if (logEntry.id == LogEntry.NOT_SAVED) {
            return false;
        }

        init();
        int rows_affected = database.delete(SQLiteHandler.TABLE_LOGS, "_id=?",
                                            new String[] {String.valueOf(logEntry.id)});
        bus.post(new OnLogEntryDeleted(logEntry.tracker_id, logEntry.id));
        return (rows_affected > 0);
    }

    private LogEntry createLogEntry(long tracker_id, long timestamp_start, long timestamp_end) {
        init();
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_TRACKER_ID, tracker_id);
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_START, timestamp_start);
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_END, timestamp_end);
        long insertId = database.insert(SQLiteHandler.TABLE_LOGS, null, values);
        return new LogEntry(insertId, tracker_id, timestamp_start, timestamp_end);
    }

    private LogEntry replace(LogEntry log_entry) {
        init();
        ContentValues values = new ContentValues();
        values.put(SQLiteHandler.COLUMN_ID, log_entry.getID());
        values.put(SQLiteHandler.COLUMN_TRACKER_ID, log_entry.getTrackerID());
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_START, log_entry.getTimestampStart());
        values.put(SQLiteHandler.COLUMN_TIMESTAMP_END, log_entry.getTimestampEnd());
        long log_id = database.replace(SQLiteHandler.TABLE_LOGS, null, values);
        return log_entry;
    }

    public List<LogEntry> getAllEntries(long tracker_id, long from_time, long to_time) {
        init();
        List<LogEntry> entries = new ArrayList<LogEntry>();
        Cursor cursor = null;
        try {

            if (from_time < 0) {
                cursor = database.rawQuery("SELECT _id, timestamp_start, timestamp_end FROM logs "
                                           + "WHERE tracker_id=? ORDER BY timestamp_start DESC LIMIT 500",
                                           new String[] {String.valueOf(tracker_id)});
            } else {
                cursor = database.rawQuery("SELECT _id, timestamp_start, timestamp_end FROM logs "
                                           + "WHERE tracker_id=? and timestamp_end>=? and "
                                           + "timestamp_end<? "
                                           + "ORDER BY timestamp_start DESC LIMIT 500",
                                           new String[] {String.valueOf(tracker_id),
                                                         String.valueOf(from_time),
                                                         String.valueOf(to_time)});
            }
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                long log_id = cursor.getLong(0);
                long time_start = cursor.getLong(1);
                long time_end = cursor.getLong(2);
                LogEntry logEntry = new LogEntry(log_id, tracker_id, time_start, time_end);
                entries.add(logEntry);
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }
        return entries;
    }

    public List<AccessPoint> getAllAccessPoints(long tracker_id) {
        init();
        List<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT _id, ssid, bssid FROM access_points "
                                       + "WHERE tracker_id=?",
                                       new String[] {String.valueOf(tracker_id)});
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                long log_id = cursor.getLong(0);
                String ssid = cursor.getString(1);
                String bssid = cursor.getString(2);
                AccessPoint ap = new AccessPoint(log_id, tracker_id, ssid, bssid);
                accessPoints.add(ap);
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return accessPoints;
    }

    public LogEntry getLatestLogEntry(long tracker_id) {
        init();
        Cursor cursor = null;
        LogEntry log = null;

        try {
            cursor = database.rawQuery("SELECT _id, timestamp_start, timestamp_end FROM logs "
                                       + "WHERE tracker_id=? ORDER BY timestamp_start DESC LIMIT 1",
                                       new String[] {String.valueOf(tracker_id)});

            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                long log_id = cursor.getLong(0);
                long time_start = cursor.getLong(1);
                long time_end = cursor.getLong(2);
                log = new LogEntry(log_id, tracker_id, time_start, time_end);
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return log;
    }

    public List< Pair<Long, Long> > getTotalDurationAggregated(long tracker_id, int aggregation_type) {
        return getTotalDurationAggregated(tracker_id, aggregation_type, -1L);
    }

    public List< Pair<Long, Long> > getTotalDurationAggregated(long tracker_id, int aggregation_type, long start_timestamp) {
        init();
        String group_by = "";

        switch (aggregation_type) {
        case AGGRETATION_DAY:
        default:
            group_by = "strftime('%Y-%m-%d', timestamp_end/1000, 'unixepoch', 'localtime')";
            break;
        case AGGRETATION_WEEK:
            group_by = "strftime('%Y-%W', timestamp_end/1000, 'unixepoch', 'localtime')";
            break;
        case AGGRETATION_YEAR:
            group_by = "strftime('%Y', timestamp_end/1000, 'unixepoch', 'localtime')";
            break;
        }

        Cursor cursor = null;
        List< Pair<Long, Long> > results = new ArrayList< Pair<Long, Long> >();

        if ( start_timestamp < 0L ) start_timestamp = 0L;

        try {
            cursor = database.rawQuery("SELECT timestamp_end, "
                                       + "SUM(timestamp_end - timestamp_start) FROM logs "
                                       + "WHERE timestamp_end>=? and tracker_id=? GROUP BY "
                                       + group_by
                                       + " ORDER BY timestamp_start DESC",
                                       new String[] {String.valueOf(start_timestamp),
                                                     String.valueOf(tracker_id)});
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                long timestamp = cursor.getLong(0);
                long duration_millis = cursor.getLong(1);
                results.add(new Pair<Long, Long>(timestamp, duration_millis));
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return results;
    }

    public UnixTimestamp getTotalDurationSince(long timestamp, long tracker_id) {
        init();
        Cursor cursor = null;
        long duration_millis = 0;

        cursor = database.rawQuery("SELECT SUM(timestamp_end - timestamp_start) FROM logs " +
                                   "WHERE tracker_id=? and timestamp_end>=?",
                                   new String[] {String.valueOf(tracker_id),
                                                 String.valueOf(timestamp)});

        try {
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                duration_millis = cursor.getLong(0);
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return new UnixTimestamp(duration_millis);
    }

    public Pair<Long, Long> getTotalDurationPairSince(long timestamp, long tracker_id) {
        init();
        long duration_millis = 0;
        long distinct_date_count = 0;

        Cursor cursor = database.rawQuery("SELECT SUM(timestamp_end - timestamp_start), " +
                                          "       COUNT(DISTINCT DATE(timestamp_end/1000, " +
                                          "                           'unixepoch', 'localtime')) " +
                                          "FROM logs " +
                                          "WHERE tracker_id=? and timestamp_end>=?",
                                          new String[] {String.valueOf(tracker_id),
                                              String.valueOf(timestamp)});

        try {
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                duration_millis = cursor.getLong(0);
                distinct_date_count = cursor.getLong(1);
            }
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return new Pair<Long, Long>(duration_millis, distinct_date_count);
    }

    public LogEntry addTimeStamp(TrackerEntry tracker, long timestamp, long graceTime) {
        init();

        if (tracker == null) {
            return null;
        }

        Log.i(TAG, "addTimestamp(" + String.valueOf(tracker.id) +
                   ", " + String.valueOf(timestamp) +
                   ", " + String.valueOf(graceTime) + ")");
        LogEntry log = getLatestLogEntry(tracker.id);

        if (log != null && log.getTimestampEnd() >= graceTime) {
            log.setTimestampEnd(timestamp);
            replace(log);
            return log;
        }

        // make a new entry
        return createLogEntry(tracker.id, timestamp, timestamp);
    }
}
