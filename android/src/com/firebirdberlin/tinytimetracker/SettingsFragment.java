package com.firebirdberlin.tinytimetracker;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;

public class SettingsFragment extends PreferenceFragment {
    public static final String TAG = TinyTimeTracker.TAG + ".SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);
    }

    private void sendMessageToActivity(String msg) {
        Intent intent = new Intent("WiFiServiceUpdates");
        intent.putExtra("Status", msg);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }
}
