package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class Settings extends AppCompatActivity {
    public static final String PREF_KEY_AUTO_DISABLE_WIFI = "pref_key_auto_disable_wifi";
    public static final String PREF_KEY_SHOW_NOTIFICATIONS = "pref_key_show_notifications";
    public static final String PREF_KEY_LAST_TRACKER_ID = "last_tracker_id";
    public static final String PREF_KEY_AUTO_DETECTION = "pref_key_notification_new_access_points";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, new SettingsFragment())
        .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.action_settings);
    }

    public static void openSettings(Context context) {
        Intent myIntent = new Intent(context, Settings.class);
        context.startActivity(myIntent);
    }

    public static boolean showNotifications(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(Settings.PREF_KEY_SHOW_NOTIFICATIONS, false);
    }

    public static boolean useAutoDetection(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(Settings.PREF_KEY_AUTO_DETECTION, true);
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
