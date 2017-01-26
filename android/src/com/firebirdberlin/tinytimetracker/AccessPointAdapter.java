package com.firebirdberlin.tinytimetracker;

import java.util.List;
import java.util.HashSet;

import com.firebirdberlin.tinytimetracker.models.AccessPoint;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AccessPointAdapter extends ArrayAdapter<AccessPoint> {
    private Context mContext = null;
    private int viewid = -1;
    private HashSet<String> activeBSSIDs = new HashSet<String>();

    public AccessPointAdapter(Context context, int viewid, List<AccessPoint> values) {
        super(context, viewid, R.id.text1, values);
        mContext = context;
        this.viewid = viewid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent ) {
        super.getView(position, convertView, parent);
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View v = inflater.inflate(viewid, parent, false);
        TextView text1 = ( TextView ) v.findViewById ( R.id.text1 );
        TextView text2 = ( TextView ) v.findViewById ( R.id.text2 );

        if (position < getCount()) {
            AccessPoint accessPoint = (AccessPoint) getItem(position);
            text1.setText(accessPoint.ssid);
            text2.setText(accessPoint.bssid);
            if ( isActive(accessPoint.ssid, accessPoint.bssid) ) {
                text2.setTextColor(Color.parseColor("#8bc34a"));
            }
        }

        return v;
    }

    public boolean addUnique(AccessPoint accessPoint) {
        int index = indexOfBSSID(accessPoint.ssid, accessPoint.bssid);

        if (index == -1) {
            add(accessPoint);
        }

        return (index == -1);
    }

    public int indexOfBSSID(String bssid) {
        for (int i = 0; i < getCount() ; i++ ) {
            AccessPoint ap = getItem(i);

            if (ap.bssid.equals(bssid) ) {
                return i;
            }
        }

        return -1;
    }

    public int indexOfBSSID(String ssid, String bssid) {
        for (int i = 0; i < getCount() ; i++ ) {
            AccessPoint ap = getItem(i);

            if (ap.bssid.equals(bssid) && ap.ssid.equals(ssid)) {
                return i;
            }
        }

        return -1;
    }

    public void clearActiveNetworks() {
        activeBSSIDs.clear();
    }

    public void setActive(String ssid, String bssid) {
        activeBSSIDs.add(bssid + "|" + ssid);
    }

    public boolean isActive(String ssid, String bssid) {
        return activeBSSIDs.contains(bssid + "|" + ssid);
    }

    public boolean toggleActive(String ssid, String bssid) {
        if (isActive(ssid, bssid)) {
            activeBSSIDs.remove(bssid + "|" + ssid);
            return false;
        } else {
            setActive(ssid, bssid);
            return true;
        }
    }
}
