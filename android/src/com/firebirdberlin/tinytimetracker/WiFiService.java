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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.firebirdberlin.tinytimetracker.events.OnWifiUpdateCompleted;
import com.firebirdberlin.tinytimetracker.models.AccessPoint;
import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;
import com.firebirdberlin.tinytimetracker.services.AddAccessPointService;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private List<AccessPoint> activeAccessPoints = new ArrayList<>();
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
            getActiveAccessPoints();
            getWiFiNetworks();
        }
    };

    private void getActiveAccessPoints() {
        List<ScanResult> networkList = wifiManager.getScanResults();
        activeAccessPoints.clear();
        for (ScanResult network : networkList) {
            activeAccessPoints.add(new AccessPoint(network.SSID, network.BSSID));
        }
    }

    @Override
    public void onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Notification note = buildServiceNotification();
            startForeground(NOTIFICATION_ID, note);
        }

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

        if (
                TinyTimeTracker.hasPermission(mContext, Manifest.permission.WAKE_LOCK)
                        && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        ) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "WIFI_MODE_SCAN_ONLY");

            if (!wifiLock.isHeld()) {
                wifiLock.acquire();
            }
        }

        // manually tracked accounts can be updated even if the device is completely in flight mode
        updateTrackersInManualMode();

        if (TinyTimeTracker.isAirplaneModeOn(mContext) ||
                !TinyTimeTracker.hasPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(TAG, "Airplane mode enabled or permission not granted.");

            long now = System.currentTimeMillis();
            saveTimestampLastRun(now);

            stopUnsuccessfulStartAttempt();
            return Service.START_NOT_STICKY;
        }

        Log.i(TAG, "WIFI SERVICE starts ...");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WIFI is currently disabled");
                wifiWasEnabled = wifiManager.setWifiEnabled(true);
            }

            final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerReceiver(wifiReceiver, filter);
            Log.i(TAG, "Receiver registered.");

            boolean success = wifiManager.startScan();
            if (!success) {
                if (isPebbleConnected()) {
                    sendDataToPebble("");
                }

                stopUnsuccessfulStartAttempt();
                return Service.START_NOT_STICKY;
            }

            handler.postDelayed(stopOnTimeout, 30000);
        } else { // android 10 and above

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (!wifiManager.isWifiEnabled() || wifiInfo == null) {
                Log.i(TAG, "WIFI is currently disabled");
                if (isPebbleConnected()) {
                    sendDataToPebble("");
                }

                stopUnsuccessfulStartAttempt();
                return Service.START_NOT_STICKY;
            }
            activeAccessPoints.clear();
            activeAccessPoints.add(new AccessPoint(wifiInfo));
            getWiFiNetworks();
        }
        return Service.START_NOT_STICKY;
    }

    private void updateTrackersInManualMode() {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
        Set<TrackerEntry> trackersToUpdate = datasource.getTrackersInManualMode();
        for (TrackerEntry tracker : trackersToUpdate) {
            Log.i(TAG, "Updating tracker in manual mode: " + tracker.verbose_name);
            datasource.updateTrackerInManualMode(tracker);
        }
        if (trackersToUpdate.size() > 0) {
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (shallDisableWifi()) {
                wifiManager.setWifiEnabled(false);
            }
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

        if (autoDisableWifi && !tracked_wifi_network_found) {
            return true;
        } else return !autoDisableWifi && wifiWasEnabled && wifiManager.isWifiEnabled();
    }

    private boolean isAutoDiscoverActive(long tracker_id) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        long now = System.currentTimeMillis();
        return now < settings.getLong(String.format("wifi_auto_discover_%d", tracker_id), -1L);
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
            Log.i(TAG, "Receiver unregistered.");
        } catch (IllegalArgumentException e) {
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
        String string_action_auto_discover = mContext.getString(R.string.activate_wifi_auto_discover);

        Intent intent = new Intent(mContext, TinyTimeTracker.class);

        PendingIntent pIntent = PendingIntent.getActivity(mContext, 1, intent, 0);
        int highlightColor = Utility.getColor(this, R.color.highlight);

        NotificationCompat.Builder note =
                new NotificationCompat.Builder(this, TinyTimeTracker.NOTIFICATIONCHANNEL_NEW_ACCESS_POINT)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSmallIcon(R.drawable.ic_wifi_add)
                        .setColor(highlightColor)
                        .setContentIntent(pIntent);

        Intent addIntent = AddAccessPointService.addIntent(this, tracker.id, ssid, bssid);
        PendingIntent pAddIntent = PendingIntent.getService(this, 2, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender().setHintHideIcon(true);

        NotificationCompat.Action addAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_add, string_action_add, pAddIntent).build();

        Intent ignoreIntent = AddAccessPointService.ignoreIntent(this, tracker.id, ssid, bssid);
        PendingIntent pIgnoreIntent = PendingIntent.getService(this, 3, ignoreIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action ignoreAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_dismiss, string_action_ignore, pIgnoreIntent)
                        .build();

        Intent autoDiscoverIntent = AddAccessPointService.autoDiscoverIntent(this, tracker.id);
        PendingIntent pAutoDiscoverIntent = PendingIntent.getService(
                this, 4, autoDiscoverIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Action autoDiscoverAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_globe, string_action_auto_discover, pAutoDiscoverIntent)
                        .build();

        note.addAction(autoDiscoverAction);
        note.addAction(addAction);
        note.addAction(ignoreAction);
        wearableExtender.addAction(addAction);
        wearableExtender.addAction(ignoreAction);
        wearableExtender.addAction(autoDiscoverAction);

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

        if (this.tracked_wifi_network_found && active_tracker == null) {
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
        Set<String> trackedBSSIDs = datasource.getTrackedBSSIDs();

        for (AccessPoint accessPoint : activeAccessPoints) {
            if (trackedBSSIDs.contains(accessPoint.bssid)) {
                Log.d(TAG, accessPoint.bssid);
                Set<TrackerEntry> trackers = datasource.getTrackersByBSSID(accessPoint.bssid);
                trackersToUpdate.addAll(trackers);
            }
        }

        return trackersToUpdate;
    }

    private void updateTrackers(LogDataSource datasource, Set<TrackerEntry> trackersToUpdate) {
        EventBus bus = EventBus.getDefault();
        long now = System.currentTimeMillis();
        for (TrackerEntry tracker : trackersToUpdate) {
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
        if (!useAutoDetection) return;
        ArrayList<AccessPoint> knownAccessPoints =
                (ArrayList<AccessPoint>) datasource.getAllAccessPoints();

        HashMap<Long, HashSet<AccessPoint>> knownAccessPointsByTrackerId = new HashMap<>();
        for (AccessPoint ap : knownAccessPoints) {
            long tracker_id = ap.getTrackerID();
            HashSet<AccessPoint> knownSet;
            if (knownAccessPointsByTrackerId.containsKey(tracker_id)) {
                knownSet = knownAccessPointsByTrackerId.get(tracker_id);
            } else {
                knownSet = new HashSet<>();
                knownAccessPointsByTrackerId.put(tracker_id, knownSet);
            }
            knownSet.add(ap);
        }

        HashMap<Long, ArrayList<AccessPoint>> unknownAccessPointsByTrackerId = new HashMap<>();
        for (AccessPoint accessPoint : activeAccessPoints) {
            String ssid = accessPoint.ssid;
            String bssid = accessPoint.bssid;
            Log.i(TAG, "? " + ssid + " " + bssid);

            for (long tracker_id : knownAccessPointsByTrackerId.keySet()) {
                HashSet<String> ssids = new HashSet<>();
                for (AccessPoint ap : knownAccessPointsByTrackerId.get(tracker_id)) {
                    ssids.add(ap.ssid);
                }
                if (!ssids.contains(ssid)) continue;

                boolean found = false;
                for (AccessPoint ap : knownAccessPointsByTrackerId.get(tracker_id)) {
                    found = (ap.ssid.equals(ssid) && ap.bssid.equals(bssid));
                    if (found) break;
                }
                if (!found && !datasource.accessPointIsIgnored(tracker_id, ssid, bssid)) {
                    AccessPoint unknownAccessPoint = new AccessPoint(ssid, bssid);
                    unknownAccessPoint.tracker_id = tracker_id;
                    ArrayList<AccessPoint> list;
                    if (unknownAccessPointsByTrackerId.containsKey(tracker_id)) {
                        list = unknownAccessPointsByTrackerId.get(tracker_id);
                    } else {
                        list = new ArrayList<>();
                        unknownAccessPointsByTrackerId.put(tracker_id, list);
                    }
                    list.add(unknownAccessPoint);
                }
            }
        }

        for (long trackerId : unknownAccessPointsByTrackerId.keySet()) {
            Log.i(TAG, String.format("Unknown APs for Tracker %d", trackerId));
            for (AccessPoint ap : unknownAccessPointsByTrackerId.get(trackerId)) {
                Log.i(TAG, String.format(" > %s (%s)", ap.ssid, ap.bssid));
            }
        }

        // tracker_ids now only contains items which do not track this BSSID
        if (unknownAccessPointsByTrackerId.size() > 0) {
            Notification note = null;
            for (long tracker_id : unknownAccessPointsByTrackerId.keySet()) {

                ArrayList<AccessPoint> accessPoints = unknownAccessPointsByTrackerId.get(tracker_id);
                AccessPoint ap = accessPoints.get(0);
                if (isAutoDiscoverActive(tracker_id)) {
                    for (AccessPoint newAP : accessPoints) {
                        AccessPoint storedAccessPoint = datasource.getAccessPoint(newAP.tracker_id, newAP.ssid, newAP.bssid);
                        if (storedAccessPoint == null) {
                            datasource.save(newAP);
                        }
                    }
                } else if (note == null) {
                    TrackerEntry tracker = datasource.getTracker(tracker_id);
                    Log.i(TAG, String.format("New AP found %d %s %s", tracker.id, ap.ssid, ap.bssid));
                    note = buildNotificationNewAccessPoint(tracker, ap.ssid, ap.bssid);
                }
            }
            if (note != null) {
                notificationManager.notify(NOTIFICATION_ID_AP, note);
            }
        }
    }

    private long getGraceTime(LogDataSource datasource, TrackerEntry tracker, long now) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        long millisConnectionLost = 1000L * 60L * settings.getInt("pref_key_absence_time", 20);
        long lastRunTime = settings.getLong("last_wifi_detection_timestamp", -1L);

        LogEntry latestLogEntry = datasource.getLatestLogEntry(tracker.id);
        if (latestLogEntry != null) {
            if (lastRunTime > 0L && latestLogEntry.getTimestampEnd() == lastRunTime) {
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
            if (last_tracker_id != -1L) {
                TrackerEntry tracker = fetchTrackerByID(last_tracker_id);
                if (tracker != null) {
                    long last_seen = settings.getLong("last_seen", 0L);
                    long delta = (now - last_seen) / 1000L;
                    long seconds_today = evaluateDurationToday(tracker).toSeconds();
                    long workingSeconds = (long) (3600 * tracker.working_hours);

                    if (seconds_today > 0 && delta < 90 * 60 && seconds_today < workingSeconds) {
                        formattedWorkTime = new UnixTimestamp(delta * 1000L).durationAsMinutes();
                    }
                }
            }
        }

        updateNotification(formattedWorkTime, trackerVerboseName);

        if (isPebbleConnected()) {
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
        if (tracker != null) {
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
        if (!showNotifications || title.isEmpty()) {
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
