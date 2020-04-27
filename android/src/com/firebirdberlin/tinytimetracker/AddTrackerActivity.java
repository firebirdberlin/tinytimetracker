package com.firebirdberlin.tinytimetracker;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.firebirdberlin.tinytimetracker.models.AccessPoint;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static com.firebirdberlin.tinytimetracker.Utility.equal;

public class AddTrackerActivity extends AppCompatActivity {
    private static String TAG = "AddTrackerActivity";
    private TrackerEntry tracker = null;
    private AccessPointAdapter accessPointAdapter = null;
    private ArrayList<AccessPoint> accessPoints = new ArrayList<>();
    private EditText edit_tracker_verbose_name = null;
    private EditText edit_tracker_working_hours = null;
    private ListView listView = null;
    private WifiManager wifiManager = null;
    private ProgressDialog progress = null;

    private final int RED = Color.parseColor("#f44336");
    private final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_tracker_activity);

        this.getApplicationContext();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.action_edit));

        setSupportActionBar(toolbar);

        // Enable the Up button
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

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
        edit_tracker_verbose_name = findViewById(R.id.edit_tracker_verbose_name);
        edit_tracker_working_hours = findViewById(R.id.edit_tracker_working_hours);
        listView = findViewById(R.id.wifi_list_view);
        if (tracker != null) {
            edit_tracker_verbose_name.setText("");
            edit_tracker_verbose_name.append(tracker.verbose_name);
            edit_tracker_working_hours.setText("");
            edit_tracker_working_hours.append(String.valueOf(tracker.working_hours));
            accessPoints = (ArrayList<AccessPoint>) datasource.getAllAccessPoints(tracker.id);
            HashSet<String> ssids = new HashSet<>();
            for (AccessPoint ap : accessPoints) ssids.add(ap.ssid);
            for (String ssid : ssids) accessPoints.add(new AccessPoint(ssid, ""));
            sort(accessPoints);
        } else {
            edit_tracker_working_hours.setText("");
            edit_tracker_working_hours.append("8");
        }
        edit_tracker_verbose_name.requestFocus();

        accessPointAdapter = new AccessPointAdapter(this, R.layout.list_2_lines, accessPoints);
        listView.setAdapter(accessPointAdapter);
        determineActiveNetworks();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterWifiReceiver();
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            Log.i(TAG, "WiFi Scan successfully completed");
            unregisterWifiReceiver();
            showAddWifiDialog();
        }
    };

    private void registerWifiReceiver(IntentFilter filter) {
        registerReceiver(wifiReceiver, filter);
        Log.i(TAG, "Receiver registered.");
    }

    private void unregisterWifiReceiver() {
        removeWifiReceiverTimeout();
        unregister(wifiReceiver);
        if (progress != null) {
            progress.dismiss();
        }
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
            removeAccessPoint(info.position);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    void removeAccessPoint(int position) {
        AccessPoint accessPoint = accessPoints.remove(position);
        HashSet<AccessPoint> toDelete = new HashSet<>();
        toDelete.add(accessPoint);

        if (accessPoint.bssid.isEmpty()) {
            String ssid = accessPoint.ssid;
            while (position < accessPoints.size()) {
                AccessPoint ap = accessPoints.get(position);
                if (!equal(ap.ssid, ssid)) break;
                accessPoints.remove(position);
                toDelete.add(ap);
            }
        }
        LogDataSource datasource = new LogDataSource(this);
        for (AccessPoint ap : toDelete) {
            datasource.delete(ap);
        }
        datasource.close();
        accessPointAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "permission ACCESS_FINE_LOCATION granted");
                onChooseWifi(null);
            } else {
                Log.e(TAG, "permission ACCESS_FINE_LOCATION denied");
            }
        }
    }

    public void onChooseWifi(View v) {

        if (Build.VERSION.SDK_INT >= 23){
            if ( ! TinyTimeTracker.isLocationEnabled(this) ) {
                showLocationProviderDisabledWarning();
                return;
            }
        }

        boolean hasPermission = TinyTimeTracker.checkAndRequestPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION,
                PERMISSIONS_REQUEST_FINE_LOCATION
        );
        if (!hasPermission) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerWifiReceiver(filter);

            boolean res = wifiManager.setWifiEnabled(true);
            Log.i(TAG, "Wifi was " + ((res) ? "" : "not") + " enabled ");
            wifiManager.startScan();
            String title = getResources().getString(R.string.dialog_title_wifi_networks_progress);
            String msg = getResources().getString(R.string.dialog_msg_wifi_networks_progress);
            progress = ProgressDialog.show(this, title, msg, true);
            setWifiReceiverTimeout(60000);
        } else {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                AccessPoint accessPoint = new AccessPoint(wifiInfo);
                accessPointAdapter.addUnique(accessPoint);
                accessPointAdapter.addUnique(new AccessPoint(accessPoint.ssid, ""));
                sort(accessPoints);
                accessPointAdapter.notifyDataSetChanged();
            }
        }
    }

    public void setWifiReceiverTimeout(long time) {
        new Handler().postDelayed(wifiReceiverTimeout, time);
    }

    public void removeWifiReceiverTimeout() {
        new Handler().removeCallbacks(wifiReceiverTimeout);
    }

    Runnable wifiReceiverTimeout = new Runnable() {
        public void run() {
            unregisterWifiReceiver();
        }
    };

    private void determineActiveNetworks() {
        if (!TinyTimeTracker.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) return;

        List<ScanResult> networkList = wifiManager.getScanResults();

        accessPointAdapter.clearActiveNetworks();
        if (networkList != null) {
            for (ScanResult network : networkList) {
                accessPointAdapter.setActive(network.SSID, network.BSSID);
            }
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            String bssid = wifiInfo.getBSSID();
            String ssid = wifiInfo.getSSID();
            if (ssid != null) ssid = ssid.replace("\"", "");
            accessPointAdapter.setActive(bssid, ssid);
        }
        accessPointAdapter.notifyDataSetChanged();
    }

    private void showAddWifiDialog() {
        final LinkedList<AccessPoint> activeAccessPoints = new LinkedList<>();
        final AccessPointAdapter adapter = new AccessPointAdapter(
                this, R.layout.list_2_lines, activeAccessPoints
        );

        List<ScanResult> networkList = wifiManager.getScanResults();
        if (networkList != null) {
            for (ScanResult network : networkList) {
                if (accessPointAdapter.indexOfBSSID(network.SSID, network.BSSID) == -1) {
                    AccessPoint accessPoint = new AccessPoint(network.SSID, network.BSSID);
                    adapter.addUnique(accessPoint);
                    adapter.addUnique(new AccessPoint(network.SSID, ""));
                }
            }
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            String bssid = wifiInfo.getBSSID();
            String ssid = wifiInfo.getSSID();
            if (ssid != null) ssid = ssid.replace("\"", "");
            if (accessPointAdapter.indexOfBSSID(ssid, bssid) == -1) {
                AccessPoint accessPoint = new AccessPoint(ssid, bssid);
                adapter.addUnique(accessPoint);
                adapter.addUnique(new AccessPoint(ssid, ""));
            }
        }

        sort(activeAccessPoints);
        determineActiveNetworks();

        if (adapter.getCount() == 0) {
            return;
        }

        new AlertDialog.Builder(this)
        .setTitle(getResources().getString(R.string.dialog_title_wifi_networks))
        .setIcon(R.drawable.ic_wifi_blue_24dp)
        .setSingleChoiceItems(adapter, 1,
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                AccessPoint accessPoint = adapter.getItem(item);

                if (edit_tracker_verbose_name.length() == 0) {
                    edit_tracker_verbose_name.append(accessPoint.ssid);
                }

                if (accessPoint.bssid.isEmpty()) {
                    adapter.toggleActive(accessPoint.ssid);
                } else {
                    adapter.toggleActive(accessPoint.ssid, accessPoint.bssid);
                }
                adapter.notifyDataSetChanged();

                edit_tracker_working_hours.requestFocus();
                if (edit_tracker_working_hours.length() == 0) {
                    showSoftKeyboard(edit_tracker_working_hours);
                } else { // hide the soft keyboard
                    hideSoftKeyboard();
                }

                if ( adapter.getCount() == 0 ) {
                    dialog.dismiss();
                }
            }
        })
        .setNegativeButton(android.R.string.no,null)
        .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        for (int i = 0; i < adapter.getCount() ; i++ ) {
                            AccessPoint accessPoint = adapter.getItem(i);
                            if ( adapter.isActive(accessPoint.ssid, accessPoint.bssid) ) {
                                accessPointAdapter.addUnique(new AccessPoint(accessPoint.ssid, ""));
                                accessPointAdapter.addUnique(accessPoint);
                            }
                        }
                        sort(accessPoints);
                        accessPointAdapter.notifyDataSetChanged();
                    }
        })
        .show();
    }

    private void showLocationProviderDisabledWarning() {
        new AlertDialog.Builder(this)
            .setMessage(R.string.warning_location_services_off)
            .setTitle(getResources().getString(R.string.title_warning_location_services_off))
            .setIcon(R.drawable.ic_wifi_blue_24dp)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void hideSoftKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void showSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        view.requestFocus();
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public void onClickOk(View v) {
        String verbose_name = edit_tracker_verbose_name.getText().toString();
        String working_hours = edit_tracker_working_hours.getText().toString();

        if (!validateInputs(verbose_name)) {
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
            if (ap.bssid.isEmpty()) continue;
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
        if (working_hours.isEmpty() ||
             Float.parseFloat(working_hours) < 0.f || Float.parseFloat(working_hours) > 24.f) {
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

    void sort(List<AccessPoint> accessPoints) {
        Collections.sort(accessPoints, new Comparator<AccessPoint>() {
            @Override
            public int compare(AccessPoint item1, AccessPoint item2) {
                String ssid1 = (item1.ssid == null) ? "" : item1.ssid;
                String ssid2 = (item2.ssid == null) ? "" : item2.ssid;
                int comp1 = ssid1.compareTo(ssid2);
                if (comp1 == 0) {
                    String bssid1 = (item1.bssid == null) ? "" : item1.bssid;
                    String bssid2 = (item2.bssid == null) ? "" : item2.bssid;

                    return bssid1.compareTo(bssid2);
                }
                return comp1;
            }
        });
    }
}
