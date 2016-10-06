package com.firebirdberlin.tinytimetracker.models;

import java.text.SimpleDateFormat;

public class LogEntry {
    public final static long NOT_SAVED = -1L;
    public long id;
    public long tracker_id;
    public UnixTimestamp timestamp_start;
    public UnixTimestamp timestamp_end;


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

    public void setTimestampStart(long timestamp) {
        timestamp_start = new UnixTimestamp(timestamp);
        return;
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

    public String toCSVString(SimpleDateFormat timeFormat) {
        return timestamp_start.toDateString() + ", "
            + timestamp_start.toTimeString(timeFormat) + ", "
            + timestamp_end.toTimeString(timeFormat) + "\n";
    }

    public String startAsDateString() {
        return timestamp_start.toDateString();
    }
}
