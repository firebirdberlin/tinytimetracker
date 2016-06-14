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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import de.greenrobot.event.EventBus;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class WiFiService extends Service {
    private static String TAG = TinyTimeTracker.TAG + ".WiFiService";
    private final static UUID PEBBLE_APP_UUID = UUID.fromString("7100dca9-2d97-4ea9-a1a9-f27aae08d144");
    private final Handler handler = new Handler();
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
    private boolean wifiWasEnabled = false;
    private boolean service_is_running = false;
    boolean tracked_wifi_network_found = false;
    private TrackerEntry active_tracker = null;

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "WIFI SERVICE init ...");
        if (service_is_running) {
            unregister(wifiReceiver);
        }
        service_is_running = true;

        mContext = this;
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "WIFI_MODE_SCAN_ONLY");

        if ( ! wifiLock.isHeld()) {
            wifiLock.acquire();
        }

        // manually tracked accounts can be updated even if the device is complety in flight mode
        updateTrackersInManualMode();

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

        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiReceiver, filter);
        Log.i(TAG, "Receiver registered.");

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

        handler.postDelayed(stopOnTimeout, 30000);
        return Service.START_NOT_STICKY;
    }

    private void updateTrackersInManualMode() {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
        Set<TrackerEntry> trackersToUpdate = datasource.getTrackersInManualMode();
        for (TrackerEntry tracker : trackersToUpdate ) {
            datasource.updateTrackerInManualMode(tracker);
        }
        if (trackersToUpdate.size() > 0 ) {
            active_tracker = trackersToUpdate.iterator().next();
        }
        datasource.close();
    }

    private void stopUnsuccessfulStartAttempt() {
        Log.w(TAG, "Unsuccessfully quitting WiFi service.");
        notificationManager.cancel(NOTIFICATION_ID_WIFI);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(stopOnTimeout);
        unregister(wifiReceiver);

        if ( shallDisableWifi() ) {
            wifiManager.setWifiEnabled(false);
        }

        updateNotifications();

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }

        wifiLock = null;
        Log.i(TAG, "Bye bye.");
    }

    private boolean shallDisableWifi() {
        boolean autoDisableWifi = Settings.autoDisableWifi(mContext);

        if ( autoDisableWifi && !tracked_wifi_network_found ) {
            return true;
        } else
        if ( !autoDisableWifi && wifiWasEnabled && wifiManager.isWifiEnabled() ) {
            return true;
        }
        return false;
    }


    private Runnable stopOnTimeout = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "WIFI timeout");
            stopSelf();
        }
    };

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(wifiReceiver);
            Log.i(TAG, "Receiver unregistered.");
        }
        catch( IllegalArgumentException e) {
            // receiver was not registered
        }
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            Log.i(TAG, "WiFi Scan successfully completed");
            handler.removeCallbacks(stopOnTimeout);
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
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
        Set<TrackerEntry> trackersToUpdate = getTrackersToUpdate(datasource);
        updateTrackers(datasource, trackersToUpdate);
        datasource.close();

        this.tracked_wifi_network_found = (trackersToUpdate.size() > 0);

        if ( this.tracked_wifi_network_found && active_tracker == null) {
            // use the first result for notifications
            active_tracker = trackersToUpdate.iterator().next();
        } else {
            Log.i(TAG, "No network found.");
        }

        EventBus bus = EventBus.getDefault();
        bus.post(new OnWifiUpdateCompleted());

        stopSelf();
    }

    private Set<TrackerEntry> getTrackersToUpdate(LogDataSource datasource) {
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

    private void updateTrackers(LogDataSource datasource, Set<TrackerEntry> trackersToUpdate) {
        EventBus bus = EventBus.getDefault();
        long now = System.currentTimeMillis();
        for (TrackerEntry tracker: trackersToUpdate) {
            LogEntry log_entry = null;
            switch (tracker.operation_state) {

                case TrackerEntry.OPERATION_STATE_AUTOMATIC:
                    log_entry = datasource.addTimeStamp(tracker, now, SECONDS_CONNECTION_LOST);
                    break;
                case TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED:
                    log_entry = datasource.addTimeStamp(tracker, now, 0);
                    tracker.operation_state = TrackerEntry.OPERATION_STATE_AUTOMATIC;
                    datasource.save(tracker);
                    break;
                default: // obviously the operation state is 'manual'
                    break;
            }

            if (log_entry != null) {
                bus.post(new OnWifiUpdateCompleted(tracker, log_entry));
            }
        }
    }

    private void updateNotifications() {
        long now = System.currentTimeMillis();
        String formattedWorkTime = "";
        String trackerVerboseName = "";
        if (active_tracker != null) {
            saveTimestampLastSeen(active_tracker, now);
            UnixTimestamp duration_today = evaluateDurationToday(active_tracker);
            formattedWorkTime = duration_today.durationAsHours();
            trackerVerboseName = active_tracker.verbose_name;

        } else {
            // user has left the office for less than 90 mins
            long last_tracker_id = settings.getLong("last_tracker_id", -1L);
            if ( last_tracker_id != -1L ) {
                TrackerEntry tracker = fetchTrackerByID(last_tracker_id);
                if ( tracker != null ) {
                    long last_seen = settings.getLong("last_seen", 0L);
                    long delta = (now - last_seen) / 1000L;
                    long seconds_today = evaluateDurationToday(tracker).toSeconds();
                    long workingSeconds = (long) (3600 * tracker.working_hours);

                    if ( seconds_today > 0 &&  delta < 90 * 60 && seconds_today < workingSeconds) {
                        formattedWorkTime = new UnixTimestamp(delta * 1000L).durationAsMinutes();
                    }
                }
            }
        }

        updateNotification(formattedWorkTime, trackerVerboseName);

        if ( isPebbleConnected() ) {
            sendDataToPebble(formattedWorkTime);
        }
    }

    private UnixTimestamp evaluateDurationToday(TrackerEntry tracker) {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();

        UnixTimestamp today = UnixTimestamp.startOfToday();
        UnixTimestamp duration_today = datasource.getTotalDurationSince(today.getTimestamp(), tracker.id);
        datasource.close();
        return duration_today;
    }

    private TrackerEntry fetchTrackerByID(long tracker_id) {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
        TrackerEntry tracker = datasource.getTracker(tracker_id);
        datasource.close();
        return tracker;
    }

    private void saveTimestampLastSeen(TrackerEntry tracker, long now) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_seen", now);
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
