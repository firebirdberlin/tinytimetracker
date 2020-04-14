package com.firebirdberlin.tinytimetracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.firebirdberlin.tinytimetracker.events.OnTrackerAdded;
import com.firebirdberlin.tinytimetracker.events.OnTrackerDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerSelected;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.ui.AddTimeBalanceDialogFragment;
import com.firebirdberlin.tinytimetracker.ui.CardFragment;
import com.firebirdberlin.tinytimetracker.ui.MainFragment;
import com.firebirdberlin.tinytimetracker.ui.StatsFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.firebirdberlin.pageindicator.PageIndicator;

public class TinyTimeTracker extends BillingHelperActivity
        implements AddTimeBalanceDialogFragment.AddTimeBalanceDialogListener {
    public static final String TAG = "TinyTimeTracker";
    public static final String NOTIFICATIONCHANNEL_SERVICE_STATUS = "NotificationChannel_Service_Status";
    public static final String NOTIFICATIONCHANNEL_TRACKER_STATUS = "NotificationChannel_Status_Notification";
    public static final String NOTIFICATIONCHANNEL_NEW_ACCESS_POINT = "NotificationChannel_new_access_point";
    public static TrackerEntry currentTracker = null;
    EventBus bus = EventBus.getDefault();
    private LinearLayout pagerLayout = null;
    private CustomViewPager pager = null;
    private PageIndicator pageIndicator = null;

    public static boolean startService(Context context) {
        Intent intent = new Intent(context, WiFiService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        return true;
    }

    public static void checkAndRequestPermission(AppCompatActivity activity, String permission,
                                                 int requestCode) {
        if (!hasPermission(activity, permission)) {
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
        if (am != null) {
            long ONE_MINUTE = 60000;
            long TWO_MINUTES = 2 * ONE_MINUTE;
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 5000, TWO_MINUTES, sender);
        }
    }

    @SuppressLint("NewApi")
    public static boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            return Global.getInt(context.getContentResolver(), Global.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return System.getInt(context.getContentResolver(), System.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Secure.getInt(context.getContentResolver(), Secure.LOCATION_MODE);

            } catch (SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != Secure.LOCATION_MODE_OFF;

        } else {
            String locationProviders = Secure.getString(context.getContentResolver(),
                    Secure.LOCATION_PROVIDERS_ALLOWED);
            return !locationProviders.isEmpty();
        }
    }


    @Override
    protected void onPurchasesInitialized() {
        super.onPurchasesInitialized();
        invalidateOptionsMenu();
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
        invalidateOptionsMenu();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(Settings.getDayNightTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        createNotificationChannels();

        // runtime permissions for the WifiService
        requestServicePermissions();

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        pagerLayout = findViewById(R.id.pager_layout);
        pager = findViewById(R.id.pager);
        pageIndicator = findViewById(R.id.page_indicator);
        pageIndicator.setPageCount(3);
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
        pager.setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                pageIndicator.setCurrentPage(position);
            }
        });
        bus.register(this);
        enableBootReceiver(this);
        scheduleWiFiService(this);
        startService(this);

    }

    @Override
    public void onResume() {
        super.onResume();

        OnTrackerAdded event = bus.getStickyEvent(OnTrackerAdded.class);
        if(event != null) {
            pager.setCurrentItem(0);
        }
    }

    @Override
    public void onPause() {
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        MenuItem item_edit = menu.findItem(R.id.action_edit);
        MenuItem item_delete = menu.findItem(R.id.action_delete);
        MenuItem item_add_time_balance = menu.findItem(R.id.action_add_time_balance);
        MenuItem item_donate = menu.findItem(R.id.action_donate);
        MenuItem item_pebble_app_store = menu.findItem(R.id.action_pebble_app_store);
        item_edit.setVisible(currentTracker != null);
        item_delete.setVisible(currentTracker != null);
        item_add_time_balance.setVisible(currentTracker != null);
        item_donate.setVisible(!isPurchased(ITEM_DONATION));

        boolean pebbleAppStoreIsInstalled =
            Utility.isPackageInstalled(this, "com.getpebble.android") ||
            Utility.isPackageInstalled(this, "com.getpebble.android.basalt");
        item_pebble_app_store.setVisible(pebbleAppStoreIsInstalled);

        pagerLayout.setVisibility(View.VISIBLE);
        pager.setPagingEnabled(true);
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
            onClickAddTracker();
            return true;
        case R.id.action_delete:
            confirmDeletion();
            return true;
        case R.id.action_add_time_balance:
            addTimeBalance();
            return true;
        case R.id.action_settings:
            Settings.openSettings(this);
            return true;
        case R.id.action_pebble_app_store:
            openPebbleAppStore();
            return true;
        case R.id.action_recommend:
            recommendApp();
            return true;
        case R.id.action_donate:
            launchBillingFlow(ITEM_DONATION);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void onClickAddTracker() {
        if (isPurchased(ITEM_PRO)) {
            AddTrackerActivity.open(this);
        } else {
            launchBillingFlow(ITEM_PRO);
        }
    }

    private void confirmDeletion() {
        if (currentTracker == null) {
            return;
        }

        final Context mContext = this;
        new AlertDialog.Builder(this)
                .setTitle(
                        this.getResources().getString(R.string.confirm_delete)
                        + " '" + currentTracker.verbose_name + "'"
                )
                .setMessage(this.getResources().getString(R.string.confirm_delete_question))
                .setIcon(R.drawable.ic_delete)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        LogDataSource datasource = new LogDataSource(mContext);
                        datasource.delete(currentTracker);

                        datasource.close();
                    }
                }).show();
    }

    private void addTimeBalance() {
        if (currentTracker == null) {
            return;
        }
        AddTimeBalanceDialogFragment dialogFragment = new AddTimeBalanceDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), null);
    }

    public void onTimeBalanceEntryAdded(int minutes) {
        final Context mContext = this;
        LogDataSource datasource = new LogDataSource(mContext);
        datasource.addTimeBalanceEntry(currentTracker, minutes);
        datasource.close();

    }


    private void openPebbleAppStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("pebble://appstore/55a573a4ba679a9523000071"));
            startActivity(intent);
        }
        catch ( ActivityNotFoundException e) {
        }
    }

    private void recommendApp() {
     String body = "https://play.google.com/store/apps/details?id=com.firebirdberlin.tinytimetracker";
     String subject = getResources().getString(R.string.recommend_app_subject);
     String description = getResources().getString(R.string.recommend_app_desc);
     Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
     sharingIntent.setType("text/plain");
     sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
     sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
     startActivity(Intent.createChooser(sharingIntent, description));
    }

    private void requestServicePermissions() {
        checkAndRequestPermission(this, Manifest.permission.WAKE_LOCK, 1);
        checkAndRequestPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED, 1);
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

    @Subscribe
    public void onEvent(OnTrackerSelected event) {
        invalidateOptionsMenu();
        Log.d(TAG, "currentTracker: " + currentTracker.toString());
    }

    @Subscribe
    public void onEvent(OnTrackerDeleted event) {
        invalidateOptionsMenu();
        pager.setCurrentItem(0);
        Log.d(TAG, "OnTrackerDeleted: currentTracker: null");
    }

    private void createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel mChannel = prepareNotificationChannel(
                    NOTIFICATIONCHANNEL_TRACKER_STATUS,
                    getString(R.string.channel_name_status),
                    getString(R.string.channel_description_status),
                    NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(mChannel);

            mChannel = prepareNotificationChannel(
                    NOTIFICATIONCHANNEL_SERVICE_STATUS,
                    getString(R.string.channel_name_service),
                    getString(R.string.channel_description_service),
                    NotificationManager.IMPORTANCE_MIN);
            mChannel.setShowBadge(false);
            mNotificationManager.createNotificationChannel(mChannel);

            mChannel = prepareNotificationChannel(
                    NOTIFICATIONCHANNEL_NEW_ACCESS_POINT,
                    getString(R.string.channel_name_new_access_points),
                    getString(R.string.channel_description_new_access_points),
                    NotificationManager.IMPORTANCE_LOW);
            mChannel.enableLights(true);
            mChannel.setLightColor(R.color.highlight);
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mChannel.setSound(uri, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    NotificationChannel prepareNotificationChannel(String channelname, String name, String desc, int importance) {
        NotificationChannel mChannel = new NotificationChannel(channelname, name, importance);
        mChannel.setDescription(desc);
        mChannel.enableLights(false);
        mChannel.enableVibration(false);
        mChannel.setSound(null, null);
        return mChannel;

    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int pos) {
            switch (pos) {
                case 0:
                default:
                    return new MainFragment();
                case 1:
                    return new CardFragment();
                case 2:
                    return new StatsFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
