package com.firebirdberlin.tinytimetracker;

import android.Manifest;
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
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.widget.Toolbar;
import de.greenrobot.event.EventBus;
import java.util.Calendar;
import java.util.List;


public class TinyTimeTracker extends AppCompatActivity {
    public static final String TAG = "TinyTimeTracker";
    EventBus bus = EventBus.getDefault();
    private TrackerEntry currentTracker = null;
    private static LogDataSource datasource = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // runtime permissions for the WifiService
        requestServicePermissions();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        bus.register(this);
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

        if (datasource == null) {
            datasource = new LogDataSource(this);
        }

        List<TrackerEntry> trackers = datasource.getTrackers();
    }

    @Override
    public void onPause() {
        datasource.close();
        datasource = null;
        super.onPause();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        }
        else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        MenuItem item_edit = menu.findItem(R.id.action_edit);
        MenuItem item_delete = menu.findItem(R.id.action_delete);
        MenuItem item_pebble_app_store = menu.findItem(R.id.action_pebble_app_store);
        item_edit.setVisible(currentTracker != null);
        item_delete.setVisible(currentTracker != null);
        boolean pebbleAppStoreIsInstalled =
            Utility.isPackageInstalled(this, "com.getpebble.android") ||
            Utility.isPackageInstalled(this, "com.getpebble.android.basalt");
        item_pebble_app_store.setVisible(pebbleAppStoreIsInstalled);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_edit:

            if (currentTracker != null) {
                AddTrackerActivity.open(this, currentTracker.id);
            }

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
        .setTitle(this.getResources().getString(R.string.confirm_delete)
                  + " '" + currentTracker.verbose_name + "'")
        .setMessage(this.getResources().getString(R.string.confirm_delete_question))
        .setIcon(R.drawable.ic_delete)
        .setNegativeButton(android.R.string.no, null)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                datasource.delete(currentTracker);
            }
        }).show();
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
        }
        catch ( ActivityNotFoundException e) {
        }
    }

    private void openGitHub() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/firebirdberlin/tinytimetracker"));
        startActivity(intent);
    }

    public static boolean startService(Context context) {
        if (hasPermission(context, Manifest.permission.WAKE_LOCK) 
                && hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            Intent intent = new Intent(context, WiFiService.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(intent);
            return true;
        } 
        return false;
    }

    private void requestServicePermissions() {
        checkAndRequestPermission(this, Manifest.permission.WAKE_LOCK, 1);
        checkAndRequestPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED, 1);

        try {
            long installed = getPackageManager().getPackageInfo(getPackageName(), 0).firstInstallTime;
            if (installed < getDateAsLong(2016, 6, 1)) {
                checkAndRequestPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION, 1);
            }
        }
        catch (NameNotFoundException e ) {
        }
    }

    public long getDateAsLong(int year, int month, int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.YEAR, year);
            return calendar.getTimeInMillis();
    }

    public static void checkAndRequestPermission(Activity activity, String permission, 
                                                 int requestCode) {
        if (! hasPermission((Context) activity, permission) ) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
        }
    }

    public static boolean hasPermission(Context context, String permission) {
        return (ContextCompat.checkSelfPermission(context, permission)
                 == PackageManager.PERMISSION_GRANTED);
    }

    public static void scheduleWiFiService(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 5000, 120000, sender);
    }

    public void enableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                                      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                      PackageManager.DONT_KILL_APP);
    }

    public void disableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                                      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                      PackageManager.DONT_KILL_APP);
    }

    public static boolean isAirplaneModeOn(Context context) {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            return Global.getInt(context.getContentResolver(), Global.AIRPLANE_MODE_ON, 0) != 0;
        }
        else {
            return System.getInt(context.getContentResolver(), System.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public void onEvent(OnTrackerSelected event) {
        currentTracker = event.newTracker;
        invalidateOptionsMenu();
        Log.d(TAG, "currentTracker: " + currentTracker.toString());
    }

    public void onEvent(OnTrackerDeleted event) {
        currentTracker = null;
        invalidateOptionsMenu();
        Log.d(TAG, "currentTracker: null");
    }
}
