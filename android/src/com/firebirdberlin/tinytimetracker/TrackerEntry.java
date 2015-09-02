package com.firebirdberlin.tinytimetracker;

public class TrackerEntry {
    private long id;
    private String ssid;
    private String verbose_name;

    public TrackerEntry(long id, String name, String verbose_name, String method){
        this.id = id;
        this.ssid = name;
        this.verbose_name = verbose_name;
    }

    public long getID() {
        return id;
    }

    public void setID(long id) {
        this.id = id;
    }

    public String getSSID() {
        return ssid;
    }

    public void setVerboseName(String verbose_name) {
        this.verbose_name = verbose_name;
    }

    public String getVerboseName() {
        return verbose_name;
    }

    public void setSSID(String ssid) {
        this.ssid = ssid;
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
        if (!ssid.equals(other.ssid)) return false;
        return true;
    }
}
