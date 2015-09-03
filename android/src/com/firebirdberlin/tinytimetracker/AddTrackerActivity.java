package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import java.util.LinkedHashSet;
import java.util.List;

public class AddTrackerActivity extends Activity {
    private static String TAG = TinyTimeTracker.TAG + ".AddTrackerActivity";
    private TrackerEntry tracker = null;
    private LogDataSource datasource = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_tracker_activity);

        datasource = new LogDataSource(this);
        Intent intent = getIntent();
        long tracker_id = intent.getLongExtra("tracker_id", -1L);
        if (tracker_id > -1L) {
            tracker = datasource.getTracker(tracker_id);
        }
        init();
    }

    private void init() {
        if (tracker != null) {
            EditText edit_tracker_name = (EditText) findViewById(R.id.edit_tracker_name);
            EditText edit_tracker_verbose_name = (EditText) findViewById(R.id.edit_tracker_verbose_name);
            edit_tracker_name.setText(tracker.getSSID());
            edit_tracker_verbose_name.setText(tracker.getVerboseName());
        }
    }

    public void onChooseWifi(View v) {
        LinkedHashSet<String> ssids = new LinkedHashSet<String>();

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> networkList = wifiManager.getScanResults();
        if (networkList != null) {
            for (ScanResult network : networkList) {
                Log.i(TAG, network.SSID);
                ssids.add(network.SSID);
            }
        }
        if (ssids.size() > 0) {
            final CharSequence[] ssid_seq = ssids.toArray(new CharSequence[ssids.size()]);

            new AlertDialog.Builder(this)
                .setTitle("Active WiFi networks")
                .setIcon(R.drawable.ic_wifi)
                .setSingleChoiceItems(ssid_seq, 0, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        Log.i(TAG, String.valueOf(ssid_seq[item]));
                        String ssid = String.valueOf(ssid_seq[item]);
                        EditText edit_name = (EditText) findViewById(R.id.edit_tracker_name);
                        EditText edit_tracker_verbose_name = (EditText) findViewById(R.id.edit_tracker_verbose_name);

                        edit_name.setText(ssid);
                        edit_tracker_verbose_name.setText(ssid);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
        }
    }

    public void onClickOk(View v) {
        EditText edit_name = (EditText) findViewById(R.id.edit_tracker_name);
        EditText edit_tracker_verbose_name = (EditText) findViewById(R.id.edit_tracker_verbose_name);
        String ssid = edit_name.getText().toString();
        String verbose_name = edit_tracker_verbose_name.getText().toString();

        if (validateInputs(ssid, verbose_name) == false) {
            return;
        }

        if (! ssid.isEmpty() && ! verbose_name.isEmpty()) {
            if (tracker == null) {
                tracker = new TrackerEntry( ssid, verbose_name, "WLAN");
            } else {
                tracker.setSSID(ssid);
                tracker.setVerboseName(verbose_name);
            }
            datasource.save(tracker);
            datasource.close();
            this.finish();
        }
    }

    private boolean validateInputs(String name, String verbose_name) {
        if (name.isEmpty() || verbose_name.isEmpty()) {
            return false;
        }

        TrackerEntry other = datasource.getTracker(verbose_name);
        if (tracker == null && other != null) { // a new tracker would be created
            return false;
        }

        if (tracker != null && other != null && other.getID() != tracker.getID()) {
            return false;
        }
        return true;
    }

    public static void open(Context context) {
        Intent myIntent = new Intent(context, AddTrackerActivity.class);
        context.startActivity(myIntent);
    }

    public static void open(Context context, long tracker_id) {
        Intent myIntent = new Intent(context, AddTrackerActivity.class);
        myIntent.putExtra("tracker_id", tracker_id);
        context.startActivity(myIntent);
    }

}
