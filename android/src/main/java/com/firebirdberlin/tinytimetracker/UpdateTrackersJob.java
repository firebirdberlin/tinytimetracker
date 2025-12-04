package com.firebirdberlin.tinytimetracker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import androidx.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.firebirdberlin.tinytimetracker.events.OnWifiUpdateCompleted;
import com.firebirdberlin.tinytimetracker.models.AccessPoint;
import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;
import com.firebirdberlin.tinytimetracker.services.AddAccessPointService;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class UpdateTrackersJob extends Worker {
    private static final String TAG = "UpdateTrackersJob";
    private static final int NOTIFICATION_ID_WIFI = 1338;
    private static final int NOTIFICATION_ID_ERROR = 1339;
    public static final int NOTIFICATION_ID_AP = 1340;

    private final Context context;
    private NotificationManager notificationManager = null;
    private WifiManager wifiManager = null;
    private TrackerEntry active_tracker = null;
    private boolean showNotifications = false;
    private boolean tracked_wifi_network_found = false;
    private boolean useAutoDetection = true;

    private final List<AccessPoint> activeAccessPoints = new ArrayList<>();

    public UpdateTrackersJob(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        showNotifications = Settings.showNotifications(context);
        useAutoDetection = Settings.useAutoDetection(context);
    }

    public static void start(Context context) {
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(UpdateTrackersJob.class)
                        .addTag(TAG)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build();

        WorkManager.getInstance(context).enqueue(request);
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        UpdateTrackersJob.class,
                        15, TimeUnit.MINUTES
                ).setConstraints(
                    new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).addTag(TAG).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "CheckWifi",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    @Override
    public @NotNull Result doWork() {
        if (TinyTimeTracker.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.d(TAG, "DEBUG: ACCESS_FINE_LOCATION is reported as GRANTED right now.");
        } else {
            Log.e(TAG, "DEBUG: ACCESS_FINE_LOCATION is reported as DENIED right now, despite user claims.");
        }
        // Do the work here--in this case, upload the images.
        updateTrackersInManualMode();

        if (TinyTimeTracker.isAirplaneModeOn(context) ||
                !TinyTimeTracker.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(TAG, "Airplane mode enabled or permission not granted.");

            long now = System.currentTimeMillis();
            saveTimestampLastRun(now);

            return stopUnsuccessfulStartAttempt();
        }
        Log.i(TAG, "WIFI SERVICE starts ...");
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (!wifiManager.isWifiEnabled() || wifiInfo == null) {
            Log.i(TAG, "WIFI is currently disabled");
            return stopUnsuccessfulStartAttempt();
        }

        Log.d(TAG, "Connected Wi-Fi BSSID: " + wifiInfo.getBSSID() + ", SSID: " + wifiInfo.getSSID());

        activeAccessPoints.clear();
        activeAccessPoints.add(new AccessPoint(wifiInfo));
        getWiFiNetworks();
        updateNotifications();

        Log.d(TAG, "WIFI SERVICE ends ...");
        return Result.success();
    }

    private void updateTrackersInManualMode() {
        LogDataSource datasource = new LogDataSource(context);
        datasource.open();
        Set<TrackerEntry> trackersToUpdate = datasource.getTrackersInManualMode();
        for (TrackerEntry tracker : trackersToUpdate) {
            Log.i(TAG, "Updating tracker in manual mode: " + tracker.verbose_name);
            datasource.updateTrackerInManualMode(tracker);
        }
        if (!trackersToUpdate.isEmpty()) {
            active_tracker = trackersToUpdate.iterator().next();
        }
        datasource.close();
    }

    private void getWiFiNetworks() {
        LogDataSource datasource = new LogDataSource(context);
        datasource.open();
        Set<TrackerEntry> trackersToUpdate = getTrackersToUpdate(datasource);
        updateTrackers(datasource, trackersToUpdate);
        findNewAccessPointsBySSID(datasource);
        datasource.close();

        tracked_wifi_network_found = !trackersToUpdate.isEmpty();

        if (tracked_wifi_network_found && active_tracker == null) {
            // use the first result for notifications
            active_tracker = trackersToUpdate.iterator().next();
            if (active_tracker != null) {
                Log.i(TAG, "Network found for " + active_tracker.verbose_name);
            }
        } else {
            Log.i(TAG, "No network found.");
        }

        EventBus bus = EventBus.getDefault();
        bus.post(new OnWifiUpdateCompleted());
    }

    private Set<TrackerEntry> getTrackersToUpdate(LogDataSource datasource) {
        Set<TrackerEntry> trackersToUpdate = new HashSet<>();
        Set<String> trackedBSSIDs = datasource.getTrackedBSSIDs();
        for (AccessPoint accessPoint : activeAccessPoints) {
            Log.d(TAG, "Current active AccessPoint BSSID: " + accessPoint.bssid);
            if (trackedBSSIDs.contains(accessPoint.bssid)) {
                Log.d(TAG, "Match found for BSSID: " + accessPoint.bssid);
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
            if (knownSet != null) {
                knownSet.add(ap);
            }
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
        if (!unknownAccessPointsByTrackerId.isEmpty()) {
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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int absenceTimeInMinutes = settings.getInt("pref_key_absence_time", 20);
        long millisConnectionLost = 1000L * 60L * Math.max(15, absenceTimeInMinutes);
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

    private Result stopUnsuccessfulStartAttempt() {
        Log.w(TAG, "Unsuccessfully quitting WiFi service.");
        notificationManager.cancel(NOTIFICATION_ID_WIFI);

        return Result.success();
    }

    private Notification buildNotification(String title, String text) {

        Intent intent = new Intent(context, TinyTimeTracker.class);
        PendingIntent pIntent = Utility.getImmutableActivity(
                context, 0, intent
        );

        int highlightColor = Utility.getColor(context, R.color.highlight);

        return new NotificationCompat.Builder(context, TinyTimeTracker.NOTIFICATIONCHANNEL_TRACKER_STATUS)
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
        String message = context.getString(R.string.new_access_point_message);
        String string_action_add = context.getString(R.string.new_access_point_action_add);
        String string_action_ignore = context.getString(R.string.new_access_point_action_ignore);
        String string_action_auto_discover = context.getString(R.string.activate_wifi_auto_discover);

        Intent intent = new Intent(context, TinyTimeTracker.class);

        PendingIntent pIntent = Utility.getImmutableActivity(
                context, 1, intent
        );
        int highlightColor = Utility.getColor(context, R.color.highlight);

        NotificationCompat.Builder note =
                new NotificationCompat.Builder(context, TinyTimeTracker.NOTIFICATIONCHANNEL_NEW_ACCESS_POINT)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSmallIcon(R.drawable.ic_wifi_add)
                        .setColor(highlightColor)
                        .setContentIntent(pIntent);

        Intent addIntent = AddAccessPointService.addIntent(context, tracker.id, ssid, bssid);
        PendingIntent pAddIntent = Utility.getImmutableService(
                context, 2, addIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender().setHintHideIcon(true);

        NotificationCompat.Action addAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_add, string_action_add, pAddIntent).build();

        Intent ignoreIntent = AddAccessPointService.ignoreIntent(context, tracker.id, ssid, bssid);
        PendingIntent pIgnoreIntent = Utility.getImmutableService(
                context, 3, ignoreIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Action ignoreAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_dismiss, string_action_ignore, pIgnoreIntent)
                        .build();

        Intent autoDiscoverIntent = AddAccessPointService.autoDiscoverIntent(context, tracker.id);
        PendingIntent pAutoDiscoverIntent = Utility.getImmutableService(
                context, 4, autoDiscoverIntent, PendingIntent.FLAG_UPDATE_CURRENT
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
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
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
    }

    private void saveTimestampLastSeen(TrackerEntry tracker, long now) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_seen", now);
        if (tracker != null) {
            editor.putLong("last_tracker_id", tracker.id);
        }
        editor.apply();
    }

    private void saveTimestampLastRun(long now) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_wifi_detection_timestamp", now);
        editor.apply();
    }

    private UnixTimestamp evaluateDurationToday(TrackerEntry tracker) {
        LogDataSource datasource = new LogDataSource(context);
        datasource.open();

        UnixTimestamp today = UnixTimestamp.startOfToday();
        UnixTimestamp duration_today = datasource.getTotalDurationSince(today.getTimestamp(), tracker.id);
        datasource.close();
        return duration_today;
    }

    private TrackerEntry fetchTrackerByID(long tracker_id) {
        LogDataSource datasource = new LogDataSource(context);
        datasource.open();
        TrackerEntry tracker = datasource.getTracker(tracker_id);
        datasource.close();
        return tracker;
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

    private boolean isAutoDiscoverActive(long tracker_id) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        long now = System.currentTimeMillis();
        String key = "wifi_auto_discover_%d" + tracker_id;
        return now < settings.getLong(key, -1L);
    }

}
