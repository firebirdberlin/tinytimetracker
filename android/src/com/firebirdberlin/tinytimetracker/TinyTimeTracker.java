package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;


import java.lang.Runnable;

public class TinyTimeTracker extends Activity {
    public static final String TAG = "TinyTimeTracker";
    private Handler viewHandler = new Handler();
    private MainView timeView = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        timeView = (MainView) findViewById(R.id.main_time_view);

        enableBootReceiver(this);
        scheduleWiFiService(this);
        startService(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewHandler.post(updateView);
    }

    @Override
    public void onPause() {
        super.onPause();
        viewHandler.removeCallbacks(updateView);
    }

    Runnable updateView = new Runnable(){
        @Override
        public void run(){
            timeView.invalidate();
            viewHandler.postDelayed(updateView, 20000);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                Settings.openSettings(this);
                return true;
            case R.id.action_donate:
                openDonationPage();
                return true;
            case R.id.action_pebble_app_store:
                openPebbleAppStore();
                return true;
            case R.id.action_open_github:
                openGitHub();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openDonationPage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=5PX9XVHHE6XP8"));
        startActivity(browserIntent);
    }


    private void openPebbleAppStore() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("pebble://appstore/55a573a4ba679a9523000071"));
        startActivity(intent);
    }


    private void openGitHub() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/firebirdberlin/tinytimetracker"));
        startActivity(intent);
    }


    public static void startService(Context context){
        Intent intent = new Intent(context, WiFiService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intent);
    }


    public static void scheduleWiFiService(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 5000, 120000, sender);
    }


    public void enableBootReceiver(Context context){
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }


    public void disableBootReceiver(Context context){
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }



}
