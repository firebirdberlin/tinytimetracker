package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Calendar;

public class Settings extends BillingHelperActivity {
    public static final String PREF_KEY_SHOW_NOTIFICATIONS = "pref_key_show_notifications";
    public static final String PREF_KEY_LAST_TRACKER_ID = "last_tracker_id";
    public static final String PREF_KEY_AUTO_DETECTION = "pref_key_notification_new_access_points";
    public static final String PREF_KEY_THEME = "pref_key_theme";

    SettingsFragment fragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        fragment = new SettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment)
                .commit();
    }


    @Override
    protected void onPurchasesInitialized() {
        super.onPurchasesInitialized();
        if (fragment != null) {
            fragment.toggleEnabledState();
        }
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
        if (fragment != null) {
            fragment.toggleEnabledState();
        }
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

    public static long getLastTrackerID(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getLong(Settings.PREF_KEY_LAST_TRACKER_ID, -1);
    }

    public static long getSecondsUntilConnectionLost(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return 60L * settings.getInt("pref_key_absence_time", 20);
    }

    public static int getDayNightTheme(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = settings.getString(Settings.PREF_KEY_THEME, "2");
        return getDayNightTheme(theme);
    }

    public static int getDayNightTheme(String theme) {
        switch (theme) {
            case "1":
                return AppCompatDelegate.MODE_NIGHT_NO;
            case "2":
                return AppCompatDelegate.MODE_NIGHT_YES;
            case "3":
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    return AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
                } else {
                    return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                }
        }
        return AppCompatDelegate.MODE_NIGHT_YES;
    }

    public static int getReferenceMonths(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String value = settings.getString("pref_key_reference_time_months", "");
        if (value.isEmpty()) {
            return 3;
        }
        return Integer.valueOf(value);
    }

    public static void setNextUpgradeRequestTime(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = settings.edit();
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, 15);
        edit.putLong("nextUpgradeRequestTime", now.getTimeInMillis());
        edit.apply();
    }

    public static boolean shallAskForUpgrade(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.contains("nextUpgradeRequestTime")) {
            setNextUpgradeRequestTime(context);
        }

        Calendar now = Calendar.getInstance();
        long nowMillis = now.getTimeInMillis();

        now.add(Calendar.DAY_OF_MONTH, 15);
        long thenMillis = settings.getLong("nextUpgradeRequestTime", now.getTimeInMillis());

        Log.i(TAG, nowMillis + " > " + thenMillis);
        return nowMillis > thenMillis;
    }
}
