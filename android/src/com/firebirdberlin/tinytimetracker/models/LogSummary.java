package com.firebirdberlin.tinytimetracker.models;

import java.util.List;
import java.util.ArrayList;

public class LogSummary {
    public List<LogDailySummary> dailySummaries;
    public TrackerEntry tracker;

    public LogSummary(TrackerEntry tracker) {
        this.dailySummaries = new ArrayList<LogDailySummary>();
        this.tracker = tracker;
    }
}
