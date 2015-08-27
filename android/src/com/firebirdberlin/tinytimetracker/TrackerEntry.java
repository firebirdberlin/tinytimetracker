package com.firebirdberlin.tinytimetracker;

public class TrackerEntry {
    private long id;
    private String ssid;

    public long getID() {
        return id;
    }

    public void setID(long id) {
        this.id = id;
    }

    public String getSSID() {
        return ssid;
    }

    public void setSSID(String ssid) {
        this.ssid = ssid;
    }

    public String toString() {
        return ssid;
    }
}
