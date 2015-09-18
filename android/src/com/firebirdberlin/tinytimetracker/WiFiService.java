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
import java.util.Calendar;
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
    private Notification note;
    private int NOTIFICATION_ID = 1337;
    private int NOTIFICATION_ID_WIFI = 1338;
    private int NOTIFICATION_ID_ERROR = 1339;
    private PendingIntent pendingIntent;
    private SharedPreferences settings = null;

    private Long SECONDS_CONNECTION_LOST = 20 * 60L;
    private boolean showNotifications = false;
    private int notificationInterval = 60 * 60;
    private LogDataSource datasource;


    @Override
    public void onCreate(){
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        datasource = new LogDataSource(this);
        datasource.open();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mContext = this;
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "WIFI_MODE_SCAN_ONLY");
        if ( ! wifiLock.isHeld()) {
            wifiLock.acquire();
        }
        showNotification();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        showNotifications = Settings.showNotifications(mContext);
        notificationInterval = 60 * Settings.getNotificationInterval(mContext);

        SECONDS_CONNECTION_LOST = 60L * settings.getInt("pref_key_absence_time", 20);

        if ( TinyTimeTracker.isAirplaneModeOn(mContext) ){
            Log.d(TAG, "Airplane mode enabled");
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        if ( ! wifiManager.isWifiEnabled() ){
            Log.d(TAG, "WIFI disabled");
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        boolean success = wifiManager.startScan();
        if (! success){
            if ( isPebbleConnected()) {
                sendDataToPebble("");
             }
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        final IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiReceiver, filter);
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        datasource.close();
        try {
            unregisterReceiver(wifiReceiver);
        } catch(IllegalArgumentException e) {
            // receiver was not registered
        }

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
        wifiLock = null;
    }


    private BroadcastReceiver wifiReceiver = new BroadcastReceiver(){
        public void onReceive(Context c, Intent i){
            getWiFiNetworks();
        }
    };


    private Notification buildNotification(String title, String text) {
         note  = new Notification.Builder(this)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true).build();
//            .addAction(R.drawable.ic_launcher, "unmute", pIntentUnmute).build();
//            .addAction(R.drawable.icon, "More", pIntent)
//            .addAction(R.drawable.icon, "And more", pIntent).build();
        return note;
    }

    public void showNotification(){

        Intent intent = new Intent(this, WiFiService.class);
        intent.putExtra("action", "click");
        pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        note = buildNotification("TinyTimeTracker", "... is running.");
        note.setLatestEventInfo(this, "TinyTimeTracker", "... is running", pendingIntent);
        note.flags|=Notification.FLAG_FOREGROUND_SERVICE;
        note.flags|=Notification.FLAG_NO_CLEAR;
        startForeground(1337, note);
    }

    public void updateNotification(String title, String text){
        if (! showNotifications) {
            return;
        }

        note = buildNotification(title, text);
        notificationManager.notify(NOTIFICATION_ID_WIFI, note);
    }


    public void showNotificationError(String title, String text){
        note = buildNotification(title, text);
        notificationManager.notify(NOTIFICATION_ID_ERROR, note);
    }

    private void getWiFiNetworks(){

        EventBus bus = EventBus.getDefault();

        Set<String> trackedSSIDs = datasource.getTrackedSSIDs("WLAN");
        String formattedWorkTime = "";
        boolean network_found = false;
        long now = System.currentTimeMillis();
        List<ScanResult> networkList = wifiManager.getScanResults();
        if (networkList != null) {
            for (ScanResult network : networkList) {
                if (trackedSSIDs.contains(network.SSID)) {
                    network_found = true;
                    Log.d(TAG, network.SSID);

                    TrackerEntry tracker = datasource.getTrackerBySSID(network.SSID);
                    if (tracker == null) {
                        continue;
                    }

                    LogEntry log_entry = datasource.addTimeStamp(tracker, now, SECONDS_CONNECTION_LOST);

                    long tracker_id = tracker.getID();
                    UnixTimestamp today = UnixTimestamp.startOfToday();
                    UnixTimestamp duration_today = datasource.getTotalDurationSince(today.getTimestamp(), tracker_id);
                    long seconds_today = duration_today.getTimestamp() / 1000L;
                    long last_notification = settings.getLong("last_notification", 0L);
                    SharedPreferences.Editor editor = settings.edit();

                    if (seconds_today < last_notification) {
                        Log.w(TAG, "date changed");
                        last_notification = 0L;
                        editor.putLong("last_notification", 0L);
                    }

                    if (seconds_today - last_notification >= notificationInterval){
                        updateNotification(duration_today.durationAsHours(), network.SSID);
                        editor.putLong("last_notification", seconds_today);
                    }

                    editor.putLong("last_seen", now);
                    editor.putLong("seconds_today", seconds_today);
                    editor.commit();

                    formattedWorkTime = duration_today.durationAsHours();
                    bus.post(new OnWifiUpdateCompleted(tracker, log_entry));
                }
            }
        }

        if ( !network_found) {
            // user has left the office for less than 90 mins
            long seconds_today = settings.getLong("seconds_today", 0L);
            long last_seen = settings.getLong("last_seen", 0L);
            long delta = (now - last_seen)/1000L;
            long workingSeconds = 3600 * Long.parseLong(settings.getString("pref_key_working_hours", "8"));
            if ( seconds_today > 0 &&  delta < 90 * 60 && seconds_today < workingSeconds) {
                formattedWorkTime = new UnixTimestamp(delta * 1000L).durationAsMinutes();
            }
        }

        bus.post(new OnWifiUpdateCompleted());

        if ( isPebbleConnected()) {
            sendDataToPebble(formattedWorkTime);
        }
        stopSelf();
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
