package com.firebirdberlin.tinytimetracker;

import android.util.Log;


public class LogEntry {
    private static String TAG = TinyTimeTracker.TAG + ".LogEntry";
    private long id;
    private long tracker_id;
    private long timestamp_start;
    private long timestamp_end;


    public LogEntry(long id, long tracker_id, long timestamp_start, long timestamp_end){
        Log.i(TAG, "LOG ENTRY : " + String.valueOf(id) + ", "
                + String.valueOf(tracker_id) + ", "
                + String.valueOf(timestamp_start) + ", "
                + String.valueOf(timestamp_end));
        this.id = id;
        this.tracker_id = tracker_id;
        this.timestamp_start = timestamp_start;
        this.timestamp_end = timestamp_end;
    }

    public long getID() {
        return id;
    }

    public long getTrackerID() {
        return tracker_id;
    }

    public long getTimestampStart() {
        return timestamp_start;
    }

    public long getTimestampEnd() {
        return timestamp_end;
    }

    public void setTimestampEnd(long timestamp) {
        timestamp_end = timestamp;
        return;
    }

    public long getTimeDiffSeconds() {
        return (timestamp_end - timestamp_start)/1000;
    }
}
