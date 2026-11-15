package com.firebirdberlin.tinytimetracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class UpdateTrackersJob extends Worker {
    private static final String TAG = "UpdateTrackersJob";
    private static final int NOTIFICATION_ID_WIFI = 1338;

    private final Context context;
    private NotificationManager notificationManager = null;
    private TrackerEntry active_tracker = null;
    private boolean showNotifications = false;

    public UpdateTrackersJob(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        showNotifications = Settings.showNotifications(context);
    }

    public static void start(Context context) {
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(UpdateTrackersJob.class)
                        .addTag(TAG)
                        .build();

        WorkManager.getInstance(context).enqueue(request);
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        UpdateTrackersJob.class,
                        15, TimeUnit.MINUTES
                ).addTag(TAG).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "CheckTrackers",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    @Override
    public @NotNull Result doWork() {
        updateTrackersInManualMode();
        updateNotifications();
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
}
