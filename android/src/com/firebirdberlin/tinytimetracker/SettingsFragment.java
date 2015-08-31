package com.firebirdberlin.tinytimetracker;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
    public static final String TAG = TinyTimeTracker.TAG + ".SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);
    }
}
