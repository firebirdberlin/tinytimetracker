package com.firebirdberlin.tinytimetracker;

import java.text.SimpleDateFormat;


public class LogEntry {
    private static String TAG = TinyTimeTracker.TAG + ".LogEntry";
    public long id;
    private long tracker_id;
    private UnixTimestamp timestamp_start;
    private UnixTimestamp timestamp_end;


    public LogEntry(long id, long tracker_id, long timestamp_start, long timestamp_end){
        this.id = id;
        this.tracker_id = tracker_id;
        this.timestamp_start = new UnixTimestamp(timestamp_start);
        this.timestamp_end = new UnixTimestamp(timestamp_end);
    }

    public long getID() {
        return id;
    }

    public long getTrackerID() {
        return tracker_id;
    }

    public long getTimestampStart() {
        return timestamp_start.getTimestamp();
    }

    public long getTimestampEnd() {
        return timestamp_end.getTimestamp();
    }

    public void setTimestampEnd(long timestamp) {
        timestamp_end = new UnixTimestamp(timestamp);
        return;
    }

    public long getTimeDiffSeconds() {
        return (timestamp_end.getTimestamp() - timestamp_start.getTimestamp())/1000;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return timestamp_start.toTimeString() + " - " + timestamp_end.toTimeString();
    }

    public String toString(SimpleDateFormat dateFormat) {
        return timestamp_start.toTimeString(dateFormat) + " - " +
               timestamp_end.toTimeString(dateFormat);
    }

    public String startAsDateString() {
        return timestamp_start.toDateString();
    }
}
