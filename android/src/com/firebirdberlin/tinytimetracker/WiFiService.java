package com.firebirdberlin.tinytimetracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebirdberlin.tinytimetracker.events.OnWifiUpdateCompleted;
import com.firebirdberlin.tinytimetracker.models.AccessPoint;
import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;
import com.firebirdberlin.tinytimetracker.services.AddAccessPointService;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.greenrobot.eventbus.EventBus;

public class WiFiService extends Service {
    private final static UUID PEBBLE_APP_UUID = UUID.fromString("7100dca9-2d97-4ea9-a1a9-f27aae08d144");
    public static int NOTIFICATION_ID_AP = 1340;
    private static String TAG = "WiFiService";
    private static int NOTIFICATION_ID = 1337;
    private static int NOTIFICATION_ID_WIFI = 1338;
    private static int NOTIFICATION_ID_ERROR = 1339;
    private final Handler handler = new Handler();
    boolean tracked_wifi_network_found = false;
    private NotificationManager notificationManager = null;
    private WifiManager wifiManager = null;
    private WifiLock wifiLock = null;
    private Context mContext = null;
    private boolean showNotifications = false;
    private boolean useAutoDetection = true;
    private boolean wifiWasEnabled = false;
    private boolean service_is_running = false;
    private TrackerEntry active_tracker = null;
    private Runnable stopOnTimeout = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "WIFI timeout");
            stopSelf();
        }
    };
    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            Log.i(TAG, "WiFi Scan successfully completed");
            handler.removeCallbacks(stopOnTimeout);
            getWiFiNetworks();
        }
    };

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "WIFI SERVICE init ...");
        if (service_is_running) {
            unregister(wifiReceiver);
        }

        mContext = this;
        service_is_running = true;
        showNotifications = Settings.showNotifications(mContext);
        useAutoDetection = Settings.useAutoDetection(mContext);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Notification note = buildServiceNotification();
            startForeground(NOTIFICATION_ID, note);
        }


        if (TinyTimeTracker.hasPermission(mContext, Manifest.permission.WAKE_LOCK) ) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "WIFI_MODE_SCAN_ONLY");

            if (!wifiLock.isHeld()) {
                wifiLock.acquire();
            }
        }

        // manually tracked accounts can be updated even if the device is complety in flight mode
        updateTrackersInManualMode();

        if ( TinyTimeTracker.isAirplaneModeOn(mContext) ||
                !TinyTimeTracker.hasPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) ) {
            Log.i(TAG, "Airplane mode enabled or permission not granted.");

            long now = System.currentTimeMillis();
            saveTimestampLastRun(now);

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
        Log.i(TAG, "Receiver registerd.");

        boolean success = wifiManager.startScan();
        if (! success) {
            if ( isPebbleConnected()) {
                sendDataToPebble("");
            }

            stopUnsuccessfulStartAttempt();
            return Service.START_NOT_STICKY;
        }

        handler.postDelayed(stopOnTimeout, 30000);
        return Service.START_NOT_STICKY;
    }

    private void updateTrackersInManualMode() {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
        Set<TrackerEntry> trackersToUpdate = datasource.getTrackersInManualMode();
        for (TrackerEntry tracker : trackersToUpdate ) {
            Log.i(TAG, "Updating tracker in manual mode: " + tracker.verbose_name);
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

        if (wifiLock != null && wifiLock.isHeld()) {
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

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
            Log.i(TAG, "Receiver unregistered.");
        }
        catch( IllegalArgumentException e) {
            // receiver was not registered
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private Notification buildServiceNotification() {
        int highlightColor = Utility.getColor(this, R.color.highlight);
        return new NotificationCompat.Builder(this, TinyTimeTracker.NOTIFICATIONCHANNEL_SERVICE_STATUS)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_in_progress))
                .setColor(highlightColor)
                .setSmallIcon(R.drawable.ic_hourglass)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .build();
    }

    @SuppressLint("NewApi")
    private Notification buildNotification(String title, String text) {

        Intent intent = new Intent(mContext, TinyTimeTracker.class);
        PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

        int highlightColor = Utility.getColor(this, R.color.highlight);

        return new NotificationCompat.Builder(this, TinyTimeTracker.NOTIFICATIONCHANNEL_TRACKER_STATUS)
                                     .setContentTitle(title)
                                     .setContentText(text)
                                     .setColor(highlightColor)
                                     .setSmallIcon(R.drawable.ic_hourglass)
                                     .setOngoing(true)
                                     .setContentIntent(pIntent)
                                     .setPriority(Notification.PRIORITY_MAX)
                                     .build();
    }

    private Notification buildNotificationNewAccessPoint(TrackerEntry tracker, String ssid, String bssid) {

        String title = tracker.verbose_name;
        String text = String.format("%s (%s)", ssid, bssid);
        String message = mContext.getString(R.string.new_access_point_message);
        String string_action_add = mContext.getString(R.string.new_access_point_action_add);
        String string_action_ignore = mContext.getString(R.string.new_access_point_action_ignore);

        Intent intent = new Intent(mContext, TinyTimeTracker.class);
        PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        int highlightColor = Utility.getColor(this, R.color.highlight);

        NotificationCompat.Builder note = new NotificationCompat.Builder(this, TinyTimeTracker.NOTIFICATIONCHANNEL_NEW_ACCESS_POINT)
                                                          .setContentTitle(title)
                                                          .setContentText(text)
                                                          .setSmallIcon(R.drawable.ic_wifi_add)
                                                          .setColor(highlightColor)
                                                          .setContentIntent(pIntent);

        Intent addIntent = AddAccessPointService.addIntent(this, tracker.id, ssid, bssid);
        PendingIntent pAddIntent = PendingIntent.getService(this, 0, addIntent,
                                                            PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.WearableExtender wearableExtender =
            new NotificationCompat.WearableExtender().setHintHideIcon(true);

        NotificationCompat.Action addAction =
            new NotificationCompat.Action.Builder(R.drawable.ic_add, string_action_add, pAddIntent).build();
        note.addAction(addAction);
        wearableExtender.addAction(addAction);

        Intent ignoreIntent = AddAccessPointService.ignoreIntent(this, tracker.id, ssid, bssid);
        PendingIntent pIgnoreIntent = PendingIntent.getService(this, 0, ignoreIntent,
                                                               PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action ignoreAction =
            new NotificationCompat.Action.Builder(R.drawable.ic_dismiss, string_action_ignore, pIgnoreIntent)
                                         .build();
        note.addAction(ignoreAction);
        wearableExtender.addAction(ignoreAction);

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(String.format(message, tracker.verbose_name, ssid, bssid));
        note.setStyle(bigStyle);

        note.extend(wearableExtender);

        return note.build();
    }

    private void getWiFiNetworks() {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
        Set<TrackerEntry> trackersToUpdate = getTrackersToUpdate(datasource);
        updateTrackers(datasource, trackersToUpdate);
        findNewAccessPointsBySSID(datasource);
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
        Set<TrackerEntry> trackersToUpdate = new HashSet<>();
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
                    long graceTime = getGraceTime(datasource, tracker, now);
                    log_entry = datasource.addTimeStamp(tracker, now, graceTime);
                    break;
                case TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED:
                    log_entry = datasource.addTimeStamp(tracker, now, now);
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
        saveTimestampLastRun(now);
    }

    private void findNewAccessPointsBySSID(LogDataSource datasource) {
        if ( !useAutoDetection) return;
        ArrayList<AccessPoint> accessPoints =
            (ArrayList<AccessPoint>) datasource.getAllAccessPoints();

        List<ScanResult> networkList = wifiManager.getScanResults();

        for (ScanResult network : networkList) {
            String ssid = network.SSID;
            String bssid = network.BSSID;
            ArrayList<AccessPoint> accessPointsWithSSID = new ArrayList<>();
            Set<Long> tracker_ids = new HashSet<>();
            // collect tracker_ids for this SSID
            for (AccessPoint ap : accessPoints) {
                if (ap.ssid.equals(ssid)) {
                    accessPointsWithSSID.add(ap);
                    tracker_ids.add(ap.getTrackerID());
                }
            }

            // remove tracker_ids if the BSSID already exists
            for (AccessPoint ap : accessPointsWithSSID) {
                if (ap.bssid.equals(bssid) ) {
                    tracker_ids.remove(ap.getTrackerID());
                }
            }
            // tracker_ids now only contains items which do not track this BSSID
            if (tracker_ids.size() > 0 ) {
                TrackerEntry tracker = datasource.getTracker(tracker_ids.iterator().next());
                Log.i(TAG, String.format("New AP found %d %s %s", tracker.id, ssid, bssid));
                if (! datasource.accessPointIsIgnored(tracker.id, ssid, bssid)) {
                    Notification note = buildNotificationNewAccessPoint(tracker, ssid, bssid);
                    notificationManager.notify(NOTIFICATION_ID_AP, note);
                    break;
                }
            }
        }
    }

    private long getGraceTime(LogDataSource datasource, TrackerEntry tracker, long now) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        long millisConnectionLost = 1000L * 60L * settings.getInt("pref_key_absence_time", 20);
        long lastRunTime = settings.getLong("last_wifi_detection_timestamp", -1L);

        LogEntry latestLogEntry = datasource.getLatestLogEntry(tracker.id);
        if ( latestLogEntry != null ) {
            if ( lastRunTime > 0L && latestLogEntry.getTimestampEnd() == lastRunTime ) {
                return lastRunTime;
            } else {
                return now - millisConnectionLost;
            }
        }

        // In all other cases a new entry is created anyway.
        // Return an arbitrary value.
        return 0L;
    }

    private void updateNotifications() {
        long now = System.currentTimeMillis();
        String formattedWorkTime = "";
        String trackerVerboseName = "";
        if (active_tracker != null) {
            Log.d(TAG, "Updating notification for tracker " + active_tracker.verbose_name);
            saveTimestampLastSeen(active_tracker, now);
            UnixTimestamp duration_today = evaluateDurationToday(active_tracker);
            formattedWorkTime = duration_today.durationAsHours();
            trackerVerboseName = active_tracker.verbose_name;
        } else {
            // user has left the office for less than 90 mins
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_seen", now);
        if ( tracker != null ) {
            editor.putLong("last_tracker_id", tracker.id);
        }
        editor.apply();
    }

    private void saveTimestampLastRun(long now) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_wifi_detection_timestamp", now);
        editor.apply();
    }

    public void updateNotification(String title, String text) {
        if ( !showNotifications || title.isEmpty() ) {
            notificationManager.cancel(NOTIFICATION_ID_WIFI);
            return;
        }

        Log.d(TAG, "Notification: " + title + " : " + text);
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
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            if (level == -1 || scale == -1) {
                return 50;
            }

            return (int) ((float) level / (float) scale * 100.0f);
        }
        return 50;
    }
}
