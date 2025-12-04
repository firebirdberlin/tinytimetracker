package com.firebirdberlin.tinytimetracker.models;

public class WorkTimeStatistics {
    public long reference_time_millis = 0;
    public long total_duration_millis = 0;
    public long distinct_date_count = 0;

    public WorkTimeStatistics(long reference_time_millis, long total_duration_millis, long distinct_date_count) {
        this.reference_time_millis = reference_time_millis;
        this.total_duration_millis = total_duration_millis;
        this.distinct_date_count = distinct_date_count;
    }

    public long getTotalDurationMills() {
        return total_duration_millis;
    }

    public long getOvertimeMillis(float dailyWorkingHours) {
        long target_working_time_in_millis =
                (long) (dailyWorkingHours * 3600L * 1000L * distinct_date_count);
        return total_duration_millis - target_working_time_in_millis;
    }

    public long getMeanDurationMillis() {
        if ( distinct_date_count > 0 ) {
            return total_duration_millis / distinct_date_count;
        }
        return 0L;
    }
}
