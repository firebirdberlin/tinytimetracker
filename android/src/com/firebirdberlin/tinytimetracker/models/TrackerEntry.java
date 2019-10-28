package com.firebirdberlin.tinytimetracker.models;

public class TrackerEntry {
    public final static long NOT_SAVED = -1L;
    public final static int OPERATION_STATE_AUTOMATIC = 0;
    public final static int OPERATION_STATE_AUTOMATIC_PAUSED = 1;
    public final static int OPERATION_STATE_AUTOMATIC_RESUMED = 2;
    public final static int OPERATION_STATE_MANUAL_ACTIVE = 3;
    public final static int OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI = 4;

    public long id;
    public String ssid = "_deprecated_";
    public String verbose_name;
    public String method;
    public float working_hours = 8.f;
    public int operation_state = OPERATION_STATE_AUTOMATIC;

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
    public int hashCode() {
        return (int) id;
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
}
