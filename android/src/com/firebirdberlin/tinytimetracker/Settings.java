package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class Settings extends AppCompatActivity {
    public static final String PREF_KEY_AUTO_DISABLE_WIFI = "pref_key_auto_disable_wifi";
    public static final String PREF_KEY_SHOW_NOTIFICATIONS = "pref_key_show_notifications";
    public static final String PREF_KEY_LAST_TRACKER_ID = "last_tracker_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new SettingsFragment())
        .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);
        root.addView(toolbar, 0); // insert at top

        toolbar.setTitle(getResources().getString(R.string.action_settings));
        setSupportActionBar(toolbar);

        // Enable the Up button
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
    }

    public static void openSettings(Context context) {
        Intent myIntent = new Intent(context, Settings.class);
        context.startActivity(myIntent);
    }

    public static boolean showNotifications(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(Settings.PREF_KEY_SHOW_NOTIFICATIONS, false);
    }

    public static boolean autoDisableWifi(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(Settings.PREF_KEY_AUTO_DISABLE_WIFI, false);
    }

    public static long getLastTrackerID(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getLong(Settings.PREF_KEY_LAST_TRACKER_ID, -1);
    }

    public static long getSecondsUntilConnectionLost(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return 60L * settings.getInt("pref_key_absence_time", 20);
    }
}
