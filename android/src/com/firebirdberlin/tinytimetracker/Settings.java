package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class Settings extends Activity {
    public static final String PREF_KEY_WORKING_HOURS = "pref_key_working_hours";
    public static final String PREF_KEY_SHOW_NOTIFICATIONS = "pref_key_show_notifications";
    public static final String PREF_KEY_NOTIFICATION_INTERVAL_MINUTES = "pref_key_notification_interval_minutes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }


    public static void openSettings(Context context) {
        Intent myIntent = new Intent(context, Settings.class);
        context.startActivity(myIntent);
    }


    public static boolean showNotifications(Context context){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(Settings.PREF_KEY_SHOW_NOTIFICATIONS, false);
    }


    public static int getNotificationInterval(Context context){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getInt(Settings.PREF_KEY_NOTIFICATION_INTERVAL_MINUTES, 60);
    }


    public static float getWorkingHours(Context context){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String workingHours = settings.getString(Settings.PREF_KEY_WORKING_HOURS, "8");
        return Float.parseFloat(workingHours);
    }
}
