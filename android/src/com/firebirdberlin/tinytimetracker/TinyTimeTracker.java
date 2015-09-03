package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import de.greenrobot.event.EventBus;


public class TinyTimeTracker extends FragmentActivity {
    public static final String TAG = "TinyTimeTracker";
    EventBus bus = EventBus.getDefault();
    private TrackerEntry currentTracker = null;
    private static LogDataSource datasource = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        bus.register(this);
        if (datasource == null) datasource = new LogDataSource(this);
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
                case 0:
                default:
                    return new MainFragment();
                case 1:
                    return new StatsFragment();
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
    }

    @Override
    public void onPause() {
        datasource.close();
        super.onPause();
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
            case R.id.action_edit:
                AddTrackerActivity.open(this, currentTracker.getID());
                return true;
            case R.id.action_add:
                AddTrackerActivity.open(this);
                return true;
            case R.id.action_delete:
                confirmDeletion();
                return true;
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

    private void confirmDeletion() {
        if (currentTracker == null) {
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Delete " + currentTracker.getSSID())
            .setMessage("Do you really want to delete this tracker and all data ?")
            .setIcon(R.drawable.ic_delete)
            .setNegativeButton(android.R.string.no, null)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    datasource.delete(currentTracker);
                }}).show();
    }

    private void openDonationPage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=5PX9XVHHE6XP8"));
        startActivity(browserIntent);
    }

    private void openPebbleAppStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("pebble://appstore/55a573a4ba679a9523000071"));
            startActivity(intent);
        } catch ( ActivityNotFoundException e) {

        }
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

    public static boolean isAirplaneModeOn(Context context) {
       return Global.getInt(context.getContentResolver(),
               Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public void onEvent(OnTrackerSelected event) {
        currentTracker = event.newTracker;
        Log.d(TAG, "currentTracker: " + currentTracker.toString());
    }

    public LogDataSource getDataSource() {
        datasource.open();
        return datasource;
    }
}
