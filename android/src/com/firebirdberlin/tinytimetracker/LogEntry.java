package com.firebirdberlin.tinytimetracker;

public class LogEntry {
    private long id;
    private long tracker_id;
    private long timestamp;

    public long getID() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
