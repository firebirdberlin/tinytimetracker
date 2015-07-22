package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import java.util.List;
import java.util.ArrayList;

public class SettingsFragment extends PreferenceFragment {

    private WifiManager wifiManager = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        ListPreference ssid_select = (ListPreference) findPreference("pref_key_wifi_ssid_select");
        wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> networkList = wifiManager.getScanResults();
        if (networkList != null) {
            ArrayList<String> ssids = new ArrayList<String>();
            for (ScanResult network : networkList) {
                ssids.add(network.SSID);
            }
            CharSequence[] ssid_seq = ssids.toArray(new CharSequence[ssids.size()]);

            ssid_select.setEntries(ssid_seq);
            ssid_select.setEntryValues(ssid_seq);

        }
    }
}
