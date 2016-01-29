package com.firebirdberlin.tinytimetracker;

public class TrackerEntry {
    public final static long NOT_SAVED = -1L;
    public long id;
    public String ssid = "_deprecated_";
    public String verbose_name;
    public String method;
    public float working_hours = 8.f;

    public TrackerEntry() {

    }

    public TrackerEntry(String verbose_name, String method){
        this.id = -1L;
        this.ssid = "_deprecated_";
        this.verbose_name = verbose_name;
        this.method = method;
    }

    public String toString() {
        return verbose_name;
    }

    @Override
    public boolean equals(Object other_object) {
        if (other_object == null) return false;
        if (other_object.getClass() != getClass()) return false;
        TrackerEntry other = (TrackerEntry) other_object;
        if (id != other.id) return false;
        if (!method.equals(other.method)) return false;
        if (!verbose_name.equals(other.verbose_name)) return false;
        return true;
    }

    public long getOvertimeMillis(long duration_millis, long distinct_date_count) {
        long target_working_time_in_millis = (long) (this.working_hours * 3600L * 1000L *
                                                     distinct_date_count);
        return duration_millis - target_working_time_in_millis;
    }

    public long getMeanDurationMillis(long duration_millis, long distinct_date_count) {
        if ( distinct_date_count > 0 ) {
            return duration_millis / distinct_date_count;
        }
        return 0L;
    }
}
