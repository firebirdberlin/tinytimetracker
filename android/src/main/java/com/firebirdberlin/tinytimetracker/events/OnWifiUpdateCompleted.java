package com.firebirdberlin.tinytimetracker.events;

import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;

public class OnWifiUpdateCompleted {

    public boolean success = false;
    public TrackerEntry tracker = null;
    public LogEntry logentry = null;

    public OnWifiUpdateCompleted(TrackerEntry tracker, LogEntry logentry) {
        this.logentry = logentry;
        this.success = true;
        this.tracker = tracker;
    }

    public OnWifiUpdateCompleted() {
        this.logentry = null;
        this.success = false;
        this.tracker = null;
    }
}
