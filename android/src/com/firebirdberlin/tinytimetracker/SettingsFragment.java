package com.firebirdberlin.tinytimetracker;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;        
import android.preference.PreferenceFragment;


public class SettingsFragment extends PreferenceFragment {
    public static final String TAG = TinyTimeTracker.TAG + ".SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);

        Preference pref_data_export = (Preference) findPreference("pref_key_data_export");
        pref_data_export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                DbImportExport.exportDb();
                DbImportExport.shareFile(getActivity());
                return true;
            }
        });

        Preference pref_data_import = (Preference) findPreference("pref_key_data_import");
        pref_data_import.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                DbImportExport.restoreDb();
                return true;
            }
        });
    }
}
