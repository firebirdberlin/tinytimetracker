package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;


public class TinyTimeTracker extends FragmentActivity {
    public static final String TAG = "TinyTimeTracker";
    private MainFragment mainFragment = null;
    private StatsFragment statsFragment = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mainFragment = new MainFragment();
        statsFragment = new StatsFragment();
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        enableBootReceiver(this);
        scheduleWiFiService(this);
        startService(this);
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int pos) {
            switch(pos) {
                case 0: return mainFragment;
                case 1: return statsFragment;
                default: return mainFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("WiFiServiceUpdates"));
    }

    @Override
    public void onPause() {
        super.onPause();
        //viewHandler.removeCallbacks(updateView);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

        }
    }


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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("Status");
            if (message != null ) {
                Log.d(TAG, "WiFi update received");
                mainFragment.refresh();
                statsFragment.refresh();
            }

        }
    };


    public static boolean isAirplaneModeOn(Context context) {
       return Global.getInt(context.getContentResolver(),
               Global.AIRPLANE_MODE_ON, 0) != 0;
    }

}
