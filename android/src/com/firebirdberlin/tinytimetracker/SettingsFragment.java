package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends PreferenceFragment {
    public static final String TAG = TinyTimeTracker.TAG + ".SettingsFragment";
    private WifiManager wifiManager = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);
        final ListPreference ssid_select = (ListPreference) findPreference("pref_key_wifi_ssid_select");
        final EditTextPreference ssid_name = (EditTextPreference) findPreference("pref_key_wifi_ssid");

        ssid_name.setSummary(ssid_name.getText());

        ssid_select.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {

                int index = ssid_select.findIndexOfValue(newValue.toString());
                if (index != -1) {
                    String new_ssid = newValue.toString();
                    Log.i(TAG, ssid_select.getEntries()[index].toString());
                    SharedPreferences.Editor editor = ssid_name.getEditor();

                    editor.putString("pref_key_wifi_ssid", new_ssid);
                    editor.commit();

                    ssid_name.setText(new_ssid);
                    ssid_name.setSummary(new_ssid);
                }
                return true;
            }
        });

        ssid_name.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ssid_name.setSummary(newValue.toString());
                return true;
            }
        });

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
