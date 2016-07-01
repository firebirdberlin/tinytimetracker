package com.firebirdberlin.tinytimetracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import android.view.inputmethod.InputMethodManager;

public class AddTrackerActivity extends AppCompatActivity {
    private static String TAG = TinyTimeTracker.TAG + ".AddTrackerActivity";
    private TrackerEntry tracker = null;
    private AccessPointAdapter accessPointAdapter = null;
    private ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
    private EditText edit_tracker_verbose_name = null;
    private EditText edit_tracker_working_hours = null;
    private ListView listView = null;
    private WifiManager wifiManager = null;

    private final int RED = Color.parseColor("#AAC0392B");
    private final int BLUE = Color.parseColor("#3498db");
    private final int PERMISSIONS_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_tracker_activity);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.action_edit));

        setSupportActionBar(toolbar);

        // Enable the Up button
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        long tracker_id = intent.getLongExtra("tracker_id", -1L);

        LogDataSource datasource = new LogDataSource(this);
        if (tracker_id > -1L) {
            tracker = datasource.getTracker(tracker_id);
        }

        init(datasource);
        datasource.close();
        registerForContextMenu(listView);
    }

    private void init(LogDataSource datasource) {
        edit_tracker_verbose_name = (EditText) findViewById(R.id.edit_tracker_verbose_name);
        edit_tracker_working_hours = (EditText) findViewById(R.id.edit_tracker_working_hours);
        listView = (ListView) findViewById(R.id.wifi_list_view);
        if (tracker != null) {
            edit_tracker_verbose_name.setText(tracker.verbose_name);
            edit_tracker_working_hours.setText(String.valueOf(tracker.working_hours));
            accessPoints = (ArrayList<AccessPoint>) datasource.getAllAccessPoints(tracker.id);
        }

        accessPointAdapter = new AccessPointAdapter(this, R.layout.list_2_lines, accessPoints);
        listView.setAdapter(accessPointAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregister(wifiReceiver);
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            Log.i(TAG, "WiFi Scan successfully completed");
            unregister(wifiReceiver);
            showAddWifiDialog();
        }
    };

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
            Log.i(TAG, "Receiver unregistered.");
        }
        catch( IllegalArgumentException e) {
            // receiver was not registered
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_tracker_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_ok:
                onClickOk(null);
                return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_access_points, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
        case R.id.action_add:
            onChooseWifi(null);
            return true;
        case R.id.action_delete:
            AccessPoint accessPoint = accessPoints.remove(info.position);
            LogDataSource datasource = new LogDataSource(this);
            datasource.delete(accessPoint);
            datasource.close();
            accessPointAdapter.notifyDataSetChanged();
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission ACCESS_COARSE_LOCATION granted");
                    onChooseWifi(null);
                } else {
                    Log.e(TAG, "permission ACCESS_COARSE_LOCATION denied");
                }
                return;
            }
        }
    }

    public void onChooseWifi(View v) {
        TinyTimeTracker.checkAndRequestPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION,
                                                  PERMISSIONS_REQUEST_COARSE_LOCATION);
        if (! TinyTimeTracker.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) return;

        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiReceiver, filter);
        Log.i(TAG, "Receiver registered.");

        boolean res = wifiManager.setWifiEnabled(true);
        Log.i(TAG, "Wifi was " + ((res) ? "" : "not") + " enabled ");
        boolean success = wifiManager.startScan();
    }

    private void showAddWifiDialog() {
        final LinkedList<AccessPoint> accessPoints = new LinkedList<AccessPoint>();
        final AccessPointAdapter adapter = new AccessPointAdapter(this, R.layout.list_2_lines,
                accessPoints);

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
        .setTitle(getResources().getString(R.string.dialog_title_wifi_networks))
        .setIcon(R.drawable.ic_wifi)
        .setSingleChoiceItems(adapter, 1,
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                AccessPoint accessPoint = adapter.getItem(item);

                if (edit_tracker_verbose_name.length() == 0) {
                    edit_tracker_verbose_name.setText(accessPoint.ssid);
                }

                accessPointAdapter.add(accessPoint);
                adapter.remove(accessPoint);
                adapter.notifyDataSetChanged();

                if (edit_tracker_working_hours.length() == 0) {
                    edit_tracker_working_hours.requestFocus();
                } else { // hide the soft keyboard
                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }

                if ( adapter.getCount() == 0 ) {
                    dialog.dismiss();
                }
            }
        })
        .setNegativeButton(android.R.string.no, null)
        .setPositiveButton(android.R.string.ok, null)
        .show();
    }

    public void onClickOk(View v) {
        String verbose_name = edit_tracker_verbose_name.getText().toString();
        String working_hours = edit_tracker_working_hours.getText().toString();

        if (validateInputs(verbose_name) == false) {
            return;
        }

        if (tracker == null) {
            tracker = new TrackerEntry(verbose_name, "WLAN");
        }


        // when saving the account the deprecated fields shall no longer contain useful data
        tracker.ssid = "_deprecated_";
        tracker.verbose_name = verbose_name.trim();
        tracker.working_hours = Float.parseFloat(working_hours);

        LogDataSource datasource = new LogDataSource(this);
        datasource.save(tracker);

        long tracker_id = tracker.id;

        for (int i = 0; i < accessPoints.size(); i++ ) {
            AccessPoint ap = accessPoints.get(i);
            ap.setTrackerID(tracker_id);
            datasource.save(ap);
        }

        datasource.close();
        this.finish();
    }

    private boolean validateInputs(String verbose_name) {
        if (verbose_name.isEmpty()) {
            edit_tracker_verbose_name.setBackgroundColor(RED);
            edit_tracker_verbose_name.requestFocus();
            edit_tracker_verbose_name.invalidate();
            return false;
        }

        String working_hours = edit_tracker_working_hours.getText().toString();
        if (working_hours.isEmpty()) {
            edit_tracker_working_hours.setBackgroundColor(RED);
            edit_tracker_working_hours.requestFocus();
            edit_tracker_working_hours.invalidate();
            return false;
        }

        LogDataSource datasource = new LogDataSource(this);
        TrackerEntry other = datasource.getTracker(verbose_name);
        datasource.close();
        if (tracker == null && other != null) { // a tracker with this name already exists
            edit_tracker_verbose_name.setBackgroundColor(RED);
            edit_tracker_verbose_name.invalidate();
            return false;
        }

        if (tracker != null && other != null && other.id != tracker.id) {
            edit_tracker_verbose_name.setBackgroundColor(RED);
            edit_tracker_verbose_name.invalidate();
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
