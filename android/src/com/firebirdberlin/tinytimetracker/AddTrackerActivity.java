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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_tracker_activity);
    }

    public static void open(Context context) {
        Intent myIntent = new Intent(context, AddTrackerActivity.class);
        context.startActivity(myIntent);
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

                        edit_name.setText(ssid);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
        }
    }

    public void onClickOk(View v) {
        EditText edit_name = (EditText) findViewById(R.id.edit_tracker_name);
        String ssid = edit_name.getText().toString();
        if (! ssid.isEmpty()) {
            saveNewSSID(ssid);
        }
        this.finish();
    }

    private void saveNewSSID(String new_ssid) {
        LogDataSource datasource = new LogDataSource(this);
        datasource.open();
        datasource.getOrCreateTrackerID(new_ssid, "WLAN");
        datasource.close();
    }
}
