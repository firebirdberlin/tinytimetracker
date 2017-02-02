package com.firebirdberlin.tinytimetracker.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.firebirdberlin.tinytimetracker.LogDataSource;
import com.firebirdberlin.tinytimetracker.WiFiService;
import com.firebirdberlin.tinytimetracker.models.AccessPoint;

public class AddAccessPointService extends IntentService {
    private static String TAG = "TinyTimeTracker.AddAccessPointService";
    private static String ACTION_ADD = "add";
    private static String ACTION_IGNORE = "ignore";
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

    @Override
    protected void onHandleIntent(Intent intent) {
        int notificationId = WiFiService.NOTIFICATION_ID_AP;

        String action = intent.getAction();

        long tracker_id = intent.getLongExtra(ExtraTrackerID, -1L);
        String ssid = intent.getStringExtra(ExtraSSID);
        String bssid = intent.getStringExtra(ExtraBSSID);

        AccessPoint ap = new AccessPoint(AccessPoint.NOT_SAVED, tracker_id, ssid, bssid);
        if (action.equals(ACTION_ADD)) {
            addAccessPoint(ap);
        }

        Log.i(TAG, String.format("%s %d %s %s", action, tracker_id, ssid, bssid));
        Log.i(TAG, String.format("%d", notificationId));

        // dismiss the notification
        if (notificationId > 0) {
            NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }

    private void addAccessPoint(AccessPoint ap) {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
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
        datasource.close();
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
}
