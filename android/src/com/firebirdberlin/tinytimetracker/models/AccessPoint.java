package com.firebirdberlin.tinytimetracker;


public class AccessPoint {
    private static String TAG = TinyTimeTracker.TAG + ".AccessPoint";
    public final static long NOT_SAVED = -1L;

    private long id = -1L;
    private long tracker_id = -1L;
    public String ssid = "";
    public String bssid = "";


    public AccessPoint(String ssid, String bssid){
        this.id = -1L;
        this.tracker_id = -1L;
        this.ssid = ssid;
        this.bssid = bssid;
    }

    public AccessPoint(long id, long tracker_id, String ssid, String bssid){
        this.id = id;
        this.tracker_id = tracker_id;
        this.ssid = ssid;
        this.bssid = bssid;
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

    public String getBSSID() {
        return bssid;
    }

    public long getTrackerID() {
        return tracker_id;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return String.valueOf(id) + ": " + ssid + " " + bssid;
    }
}
