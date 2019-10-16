package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.firebirdberlin.tinytimetracker.models.AccessPoint;

import java.util.HashSet;
import java.util.List;

public class AccessPointAdapter extends ArrayAdapter<AccessPoint> {
    private Context mContext;
    private int viewid;
    private HashSet<String> activeBSSIDs = new HashSet<>();
    private HashSet<String> activeSSIDs = new HashSet<>();

    AccessPointAdapter(Context context, int viewid, List<AccessPoint> values) {
        super(context, viewid, R.id.text1, values);
        mContext = context;
        this.viewid = viewid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent ) {
        super.getView(position, convertView, parent);
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View v = inflater.inflate(viewid, parent, false);
        TextView text1 = v.findViewById ( R.id.text1 );
        TextView text2 = v.findViewById ( R.id.text2 );

        int colorActivated = Utility.getColor(mContext, R.color.listItemActivated);
        if (position < getCount()) {
            AccessPoint accessPoint = getItem(position);
            text1.setText(accessPoint.ssid);
            text2.setText(accessPoint.bssid);
            if ( isActive(accessPoint.ssid, accessPoint.bssid) ) {
                if ( accessPoint.bssid.isEmpty() ) {
                    text1.setTextColor(colorActivated);
                } else {
                    text2.setTextColor(colorActivated);
                }
            }
            if (accessPoint.bssid.isEmpty()) {
                text2.setVisibility(View.GONE);
            } else {
                text1.setVisibility(View.GONE);
            }
        }

        return v;
    }

    boolean addUnique(AccessPoint accessPoint) {
        int index = indexOfBSSID(accessPoint.ssid, accessPoint.bssid);

        if (index == -1) {
            add(accessPoint);
        }

        return (index == -1);
    }

    int indexOfBSSID(String ssid, String bssid) {
        for (int i = 0; i < getCount() ; i++ ) {
            AccessPoint ap = getItem(i);

            if (ap.bssid.equals(bssid) && ap.ssid.equals(ssid)) {
                return i;
            }
        }

        return -1;
    }

    void clearActiveNetworks() {
        activeBSSIDs.clear();
        activeSSIDs.clear();
    }

    void setActive(String ssid, String bssid) {
        activeBSSIDs.add(bssid + "|" + ssid);
    }

    void setInactive(String ssid, String bssid) {
        activeBSSIDs.remove(bssid + "|" + ssid);
    }

    boolean isActive(String ssid, String bssid) {
        return activeBSSIDs.contains(bssid + "|" + ssid);
    }

    boolean toggleActive(String ssid, String bssid) {
        if (isActive(ssid, bssid)) {
            activeBSSIDs.remove(bssid + "|" + ssid);
            return false;
        } else {
            setActive(ssid, bssid);
            return true;
        }
    }

    void setActive(String ssid) {
        activeSSIDs.add(ssid);
        HashSet<String> bssids = new HashSet<>();
        for (int i = 0; i < getCount(); i++) {
            AccessPoint ap = getItem(i);
            if (ap.ssid.equals(ssid) && !ap.bssid.isEmpty()) {
                bssids.add(ap.bssid);
            }
        }
        for (String bssid : bssids) {
            setActive(ssid, bssid);
        }
    }

    void setInactive(String ssid) {
        activeSSIDs.remove(ssid);
        HashSet<String> bssids = new HashSet<>();
        for (int i = 0; i < getCount(); i++) {
            AccessPoint ap = getItem(i);
            if (ap.ssid.equals(ssid) && !ap.bssid.isEmpty()) {
                bssids.add(ap.bssid);
            }
        }
        for (String bssid : bssids) {
            setInactive(ssid, bssid);
        }
    }

    boolean toggleActive(String ssid) {
        if (activeSSIDs.contains(ssid)) {
            setInactive(ssid);
            return false;
        } else {
            setActive(ssid);
            return true;
        }
    }
}
