package com.firebirdberlin.tinytimetracker;

import android.app.ListActivity;
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
import java.util.LinkedList;
import java.util.List;

public class AddTrackerActivity extends ListActivity {
    private static String TAG = TinyTimeTracker.TAG + ".AddTrackerActivity";
    private TrackerEntry tracker = null;
    private LogDataSource datasource = null;
    private AccessPointAdapter accessPointAdapter = null;
    private EditText edit_tracker_verbose_name = null;
    private EditText edit_tracker_name = null;
    final private LinkedList<AccessPoint> accessPoints = new LinkedList<AccessPoint>();

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
        edit_tracker_name = (EditText) findViewById(R.id.edit_tracker_name);
        edit_tracker_verbose_name = (EditText) findViewById(R.id.edit_tracker_verbose_name);


        accessPointAdapter = new AccessPointAdapter(this, R.layout.list_2_lines, accessPoints);
        setListAdapter(accessPointAdapter);

        if (tracker != null) {
            edit_tracker_name.setText(tracker.getSSID());
            edit_tracker_verbose_name.setText(tracker.getVerboseName());
        }
    }

    public void onChooseWifi(View v) {
        final LinkedList<AccessPoint> accessPoints = new LinkedList<AccessPoint>();
        final AccessPointAdapter adapter = new AccessPointAdapter(this, R.layout.list_2_lines,
                                                                  accessPoints);

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> networkList = wifiManager.getScanResults();
        if (networkList == null) {
            return;
        }

        for (ScanResult network : networkList) {
            if (accessPointAdapter.indexOfBSSID(network.BSSID) == -1) {
                AccessPoint accessPoint = new AccessPoint(network.SSID, network.BSSID);
                adapter.add(accessPoint);
            }
        }

        new AlertDialog.Builder(this)
            .setTitle("Active WiFi networks")
            .setIcon(R.drawable.ic_wifi)
            .setAdapter(adapter,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                AccessPoint accessPoint = adapter.getItem(item);
                                String ssid = accessPoint.ssid;
                                String bssid = accessPoint.bssid;

                                Log.i(TAG, ssid);

                                if (edit_tracker_name.length() == 0) {
                                    edit_tracker_name.setText(ssid);
                                }

                                if (edit_tracker_verbose_name.length() == 0) {
                                    edit_tracker_verbose_name.setText(ssid);
                                }

                                accessPointAdapter.add(accessPoint);

                                dialog.dismiss();
                            }
                        })
            .setNegativeButton(android.R.string.no, null).show();
    }

    public void onClickOk(View v) {
        String verbose_name = edit_tracker_verbose_name.getText().toString();

        if (validateInputs(verbose_name) == false) {
            return;
        }

        if (tracker == null) {
            tracker = new TrackerEntry(verbose_name, "WLAN");
        } else {
            tracker.setVerboseName(verbose_name);
        }

        datasource.save(tracker);
        long tracker_id = tracker.getID();
        for (int i=0; i < accessPoints.size(); i++ ) {
            AccessPoint ap = accessPoints.get(i);
            ap.setTrackerID(tracker_id);
            datasource.save(ap);

        }
        datasource.close();
        this.finish();
    }

    private boolean validateInputs(String verbose_name) {
        if (accessPoints.size() == 0 || verbose_name.isEmpty()) {
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
