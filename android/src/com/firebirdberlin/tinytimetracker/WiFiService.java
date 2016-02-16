package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import de.greenrobot.event.EventBus;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class WiFiService extends Service {
    private static String TAG = TinyTimeTracker.TAG + ".WiFiService";
    private final static UUID PEBBLE_APP_UUID = UUID.fromString("7100dca9-2d97-4ea9-a1a9-f27aae08d144");
    private NotificationManager notificationManager = null;
    private WifiManager wifiManager = null;
    private WifiLock wifiLock = null;
    private Context mContext = null;
    private int NOTIFICATION_ID = 1337;
    private int NOTIFICATION_ID_WIFI = 1338;
    private int NOTIFICATION_ID_ERROR = 1339;
    private SharedPreferences settings = null;

    private Long SECONDS_CONNECTION_LOST = 20 * 60L;
    private boolean showNotifications = false;
    private LogDataSource datasource;
    private boolean wifiWasEnabled = false;


    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        datasource = new LogDataSource(this);
        datasource.open();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "WIFI SERVICE init ...");
        mContext = this;
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "WIFI_MODE_SCAN_ONLY");

        if ( ! wifiLock.isHeld()) {
            wifiLock.acquire();
        }

        if ( TinyTimeTracker.isAirplaneModeOn(mContext) ) {
            Log.i(TAG, "Airplane mode enabled");
            stopUnsuccessfulStartAttempt();
            return Service.START_NOT_STICKY;
        }

        Log.i(TAG, "WIFI SERVICE starts ...");
        if ( ! wifiManager.isWifiEnabled() ) {
            Log.i(TAG, "WIFI is currently disabled");
            wifiWasEnabled = wifiManager.setWifiEnabled(true);
        }

        final IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiReceiver, filter);
        boolean success = wifiManager.startScan();

        if (! success) {
            if ( isPebbleConnected()) {
                sendDataToPebble("");
            }

            stopUnsuccessfulStartAttempt();
            return Service.START_NOT_STICKY;
        }

        showNotifications = Settings.showNotifications(mContext);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        SECONDS_CONNECTION_LOST = 60L * settings.getInt("pref_key_absence_time", 20);
        return Service.START_NOT_STICKY;
    }

    private void stopUnsuccessfulStartAttempt() {
        Log.w(TAG, "Unsuccessfully quiting WiFi service.");
        notificationManager.cancel(NOTIFICATION_ID_WIFI);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        datasource.close();

        try {
            unregisterReceiver(wifiReceiver);
        }
        catch(IllegalArgumentException e) {
            // receiver was not registered
        }

        if ( wifiWasEnabled && wifiManager.isWifiEnabled() ) {
            wifiManager.setWifiEnabled(false);
        }

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }

        wifiLock = null;
        Log.i(TAG, "Bye bye.");
    }


    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            getWiFiNetworks();
        }
    };


    private Notification buildNotification(String title, String text) {

        Intent intent = new Intent(mContext, TinyTimeTracker.class);
        PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

        Notification note = new Notification.Builder(this).setContentTitle(title)
                                                          .setContentText(text)
                                                          .setSmallIcon(R.drawable.ic_hourglass)
                                                          .setOngoing(true)
                                                          .setContentIntent(pIntent)
                                                          .setPriority(Notification.PRIORITY_MAX)
                                                          .build();
        return note;
    }

    private void getWiFiNetworks() {
        EventBus bus = EventBus.getDefault();
        String formattedWorkTime = "";
        String trackerVerboseName = "";
        long now = System.currentTimeMillis();
        Set<TrackerEntry> trackersToUpdate = getTrackersToUpdate();

        for (TrackerEntry tracker: trackersToUpdate) {
            LogEntry log_entry = datasource.addTimeStamp(tracker, now, SECONDS_CONNECTION_LOST);
            bus.post(new OnWifiUpdateCompleted(tracker, log_entry));
        }

        boolean network_found = (trackersToUpdate.size() > 0);

        if ( !network_found ) {
            Log.i(TAG, "No network found.");
            // user has left the office for less than 90 mins
            long last_tracker_id = settings.getLong("last_tracker_id", -1L);
            if ( last_tracker_id != -1L ) {
                TrackerEntry tracker = datasource.getTracker(last_tracker_id);
                if ( tracker != null ) {
                    long last_seen = settings.getLong("last_seen", 0L);
                    long delta = (now - last_seen) / 1000L;
                    long seconds_today = settings.getLong("seconds_today", 0L);
                    long workingSeconds = (long) (3600 * tracker.working_hours);

                    if ( seconds_today > 0 &&  delta < 90 * 60 && seconds_today < workingSeconds) {
                        formattedWorkTime = new UnixTimestamp(delta * 1000L).durationAsMinutes();
                    }
                }
            }
        }
        else {
            // use the first result for notifications
            TrackerEntry tracker = trackersToUpdate.iterator().next();
            UnixTimestamp duration_today = evaluateDurationToday(tracker);
            saveTimestampLastSeen(tracker, duration_today, now);
            formattedWorkTime = duration_today.durationAsHours();
            trackerVerboseName = tracker.verbose_name;
        }

        bus.post(new OnWifiUpdateCompleted());
        updateNotification(formattedWorkTime, trackerVerboseName);

        if ( isPebbleConnected() ) {
            sendDataToPebble(formattedWorkTime);
        }

        stopSelf();
    }

    private Set<TrackerEntry> getTrackersToUpdate() {
        Set<TrackerEntry> trackersToUpdate = new HashSet<TrackerEntry>();
        List<ScanResult> networkList = wifiManager.getScanResults();
        Set<String> trackedBSSIDs = datasource.getTrackedBSSIDs();

        for (ScanResult network : networkList) {
            if (trackedBSSIDs.contains(network.BSSID)) {
                Log.d(TAG, network.BSSID);
                Set<TrackerEntry> trackers = datasource.getTrackersByBSSID(network.BSSID);
                trackersToUpdate.addAll(trackers);
            }
        }

        /* Legacy code; support the old database format which does not consider BSSIDs
         * (< version 3)
         * Newer database versions still have the attribute 'name' but it contains the value
         * '_deprecated_'
         */
        Set<String> trackedSSIDs = datasource.getTrackedSSIDs("WLAN");

        for (ScanResult network : networkList) {
            if (trackedSSIDs.contains(network.SSID)) {
                Log.d(TAG, network.SSID);
                TrackerEntry tracker = datasource.getTrackerBySSID(network.SSID);

                if (tracker != null) {
                    trackersToUpdate.add(tracker);
                }
            }
        }

        return trackersToUpdate;
    }

    private UnixTimestamp evaluateDurationToday(TrackerEntry tracker) {
        UnixTimestamp today = UnixTimestamp.startOfToday();
        UnixTimestamp duration_today = datasource.getTotalDurationSince(today.getTimestamp(), tracker.id);
        return duration_today;
    }

    private void saveTimestampLastSeen(TrackerEntry tracker, UnixTimestamp duration_today, long now) {
        long seconds_today = duration_today.getTimestamp() / 1000L;
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_seen", now);
        editor.putLong("seconds_today", seconds_today);
        if ( tracker != null ) {
            editor.putLong("last_tracker_id", tracker.id);
        }
        editor.commit();
    }

    public void updateNotification(String title, String text) {
        if ( !showNotifications || title.isEmpty() ) {
            notificationManager.cancel(NOTIFICATION_ID_WIFI);
            return;
        }

        Notification note = buildNotification(title, text);
        notificationManager.notify(NOTIFICATION_ID_WIFI, note);
    }

    private boolean isPebbleConnected() {
        boolean connected = PebbleKit.isWatchConnected(mContext);
        Log.i(TAG, "Pebble is " + (connected ? "connected" : "not connected"));
        return connected;
    }

    private void sendDataToPebble(String formattedWorktime) {
        PebbleDictionary data = new PebbleDictionary();
        data.addString(2, formattedWorktime);
        data.addInt32(3, getBatteryLevel());
        PebbleKit.sendDataToPebble(mContext, PEBBLE_APP_UUID, data);
    }

    public int getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if(level == -1 || scale == -1) {
            return 50;
        }

        return (int) ((float)level / (float)scale * 100.0f);
    }
}
