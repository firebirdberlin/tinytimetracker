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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;
import java.util.List;


public class WiFiService extends Service {
	private static String TAG = TinyTimeTracker.TAG + ".WiFiService";
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

    private Long SECONDS_CONNECTION_LOST = 300L; 
    private String TRACKED_SSID = ""; 
    private boolean showNotifications = false;
    private int notificationInterval = 60 * 60;


	@Override
	public void onCreate(){
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
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

        TRACKED_SSID = settings.getString("pref_key_wifi_ssid", "");
        SECONDS_CONNECTION_LOST = Long.parseLong(settings.getString("pref_key_seconds_connection_lost", "300"));

        final IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiReceiver, filter);
        boolean success = wifiManager.startScan();
        if (! success){
            stopSelf();
        }
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy(){
        unregisterReceiver(wifiReceiver);

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
//			.addAction(R.drawable.ic_launcher, "unmute", pIntentUnmute).build();
//			.addAction(R.drawable.icon, "More", pIntent)
//			.addAction(R.drawable.icon, "And more", pIntent).build();
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

        if ( ! wifiManager.isWifiEnabled() ){
            Log.d(TAG, "WIFI disabled");
            showNotificationError("WIFI disabled", "... enabling WIFi.");
            boolean success = wifiManager.setWifiEnabled(true);
            if (! success){
                stopSelf();
                return;
            }
        }

        List<ScanResult> networkList = wifiManager.getScanResults();
        if (networkList != null) {
            for (ScanResult network : networkList) {
                if (TRACKED_SSID.equals(network.SSID)) {

                    Log.w (TAG, network.SSID + " " + network.capabilities);
                    
                    Long seconds_today = settings.getLong("seconds_today", 0L);
                    Long last_seen = settings.getLong("last_seen", 0L);
                    Long last_notification = settings.getLong("last_notification", 0L);
                    Long now = System.currentTimeMillis() / 1000;

                    Log.w(TAG, "now: " + String.valueOf(now));
                    Log.w(TAG, "last_seen: " + String.valueOf(last_seen));

                    Long date_last_seen = start_of_day(last_seen); 
                    Long date_now = start_of_day(now); 
                    Log.w(TAG, "now: " + String.valueOf(date_now));
                    Log.w(TAG, "last_seen: " + String.valueOf(date_last_seen));

                    SharedPreferences.Editor editor = settings.edit();

                    Long delta = now - last_seen;
                    if ( ! date_now.equals(date_last_seen)) {
                        Log.w(TAG, "date changed");
                        seconds_today = 0L;
                        editor.putLong("last_notification", 0L);
                    } else if (delta < SECONDS_CONNECTION_LOST) {
                        seconds_today += delta;
                        Log.d(TAG, "seconds update: " + String.valueOf(delta));
                    }

                    if (seconds_today - last_notification >= notificationInterval){ 
                        editor.putLong("last_notification", seconds_today);
                        updateNotification(format_seconds(seconds_today.intValue()), network.SSID);
                    }

                    editor.putLong("last_seen", now);
                    editor.putLong("seconds_today", seconds_today);
                    editor.commit();

                }
                Log.w (TAG, "'"+network.SSID+"'");
                // Calendar cal = Calendar.getInstance();
                // cal.setTimeInMillis(network.timestamp / 1000L);
                // Log.i(TAG, cal.toString());
                // Long seconds = network.timestamp / 1000L / 1000L;
                // showNotificationError("WIFI", format_seconds(seconds.intValue()));
            }
        } else {
            // showNotificationError("WIFI", "no networks  found");
        }
        stopSelf();
    } 


    private Long start_of_day(Long timestamp){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp * 1000L);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        return cal.getTimeInMillis() / 1000;
    }

    public static String format_seconds(int seconds){
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int sec = seconds % 60;

        //return String.format("%02d:%02d:%02d", hours, minutes, sec);
        return String.format("%02d:%02d", hours, minutes);
    }
}
