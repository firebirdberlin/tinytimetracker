package com.firebirdberlin.tinytimetracker.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebirdberlin.tinytimetracker.LogDataSource;
import com.firebirdberlin.tinytimetracker.WiFiService;
import com.firebirdberlin.tinytimetracker.models.AccessPoint;

public class AddAccessPointService extends IntentService {
    private static String TAG = "TinyTimeTracker.AddAccessPointService";
    private static String ACTION_ADD = "add";
    private static String ACTION_IGNORE = "ignore";
    private static String ACTION_ACTIVATE_AUTO_DISCOVER = "activate_auto_discover";
    private static String ExtraTrackerID = "tracker_id";
    private static String ExtraSSID = "SSID";
    private static String ExtraBSSID = "BSSID";

    public AddAccessPointService() {
        super("AddAccessPointService");
    }

    public AddAccessPointService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static Intent autoDiscoverIntent(Context context, long tracker_id) {
        Intent intent = new Intent(context, AddAccessPointService.class);
        intent.setAction(ACTION_ACTIVATE_AUTO_DISCOVER);
        intent.putExtra(ExtraTrackerID, tracker_id);
        return intent;
    }

    private void addAccessPoint(AccessPoint ap) {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
        Log.i(TAG, " > adding access point ");
        AccessPoint storedAccessPoint = datasource.getAccessPoint(ap.tracker_id, ap.ssid, ap.bssid);
        if (storedAccessPoint == null) {
            datasource.save(ap);
        }

        datasource.deleteAccessPointWithoutBSSID(ap.tracker_id, ap.ssid);
        datasource.close();
    }

    private void ignoreAccessPoint(AccessPoint ap) {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
        Log.i(TAG, " > adding ap to ignore list");
        datasource.addToIgnoreList(ap);
        datasource.close();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int notificationId = WiFiService.NOTIFICATION_ID_AP;

        String action = intent.getAction();

        long tracker_id = intent.getLongExtra(ExtraTrackerID, -1L);
        String ssid = intent.getStringExtra(ExtraSSID);
        String bssid = intent.getStringExtra(ExtraBSSID);

        Log.i(TAG, String.format("%s %d %s %s", action, tracker_id, ssid, bssid));

        AccessPoint ap = new AccessPoint(AccessPoint.NOT_SAVED, tracker_id, ssid, bssid);
        if (ACTION_ADD.equals(action)) {
            addAccessPoint(ap);
        } else if (ACTION_IGNORE.equals(action)) {
            ignoreAccessPoint(ap);
        } else if (ACTION_ACTIVATE_AUTO_DISCOVER.equals(action)) {
            activateAutoDiscover(tracker_id);
        }

        // dismiss the notification
        if (notificationId > 0) {
            NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }

    public static Intent addIntent(Context context, long tracker_id, String ssid, String bssid) {
        Intent intent = new Intent(context, AddAccessPointService.class);
        intent.setAction(ACTION_ADD);
        intent.putExtra(ExtraTrackerID, tracker_id);
        intent.putExtra(ExtraSSID, ssid);
        intent.putExtra(ExtraBSSID, bssid);
        return intent;
    }

    public static Intent ignoreIntent(Context context, long tracker_id, String ssid, String bssid) {
        Intent intent = new Intent(context, AddAccessPointService.class);
        intent.setAction(ACTION_IGNORE);
        intent.putExtra(ExtraTrackerID, tracker_id);
        intent.putExtra(ExtraSSID, ssid);
        intent.putExtra(ExtraBSSID, bssid);
        return intent;
    }

    private void activateAutoDiscover(long tracker_id) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        long now = System.currentTimeMillis();
        editor.putLong(String.format("wifi_auto_discover_%d", tracker_id), now + 1000 * 60 * 60);
        editor.apply();
    }
}
