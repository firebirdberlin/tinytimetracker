package com.firebirdberlin.tinytimetracker;

import android.app.ListActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import android.view.inputmethod.InputMethodManager;

public class AddTrackerActivity extends ListActivity {
    private static String TAG = TinyTimeTracker.TAG + ".AddTrackerActivity";
    private TrackerEntry tracker = null;
    private LogDataSource datasource = null;
    private AccessPointAdapter accessPointAdapter = null;
    private ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
    private Button button_wifi = null;
    private EditText edit_tracker_verbose_name = null;
    private EditText edit_tracker_working_hours = null;

    private final int RED = Color.parseColor("#AAC0392B");
    private final int BLUE = Color.parseColor("#3498db");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_tracker_activity);
        registerForContextMenu(this.getListView());
        datasource = new LogDataSource(this);
        Intent intent = getIntent();
        long tracker_id = intent.getLongExtra("tracker_id", -1L);

        if (tracker_id > -1L) {
            tracker = datasource.getTracker(tracker_id);
        }

        init();
    }

    private void init() {
        edit_tracker_verbose_name = (EditText) findViewById(R.id.edit_tracker_verbose_name);
        edit_tracker_working_hours = (EditText) findViewById(R.id.edit_tracker_working_hours);
        button_wifi = (Button) findViewById(R.id.button_wifi);
        setWifiIconColor(BLUE);

        if (tracker != null) {
            edit_tracker_verbose_name.setText(tracker.verbose_name);
            edit_tracker_working_hours.setText(String.valueOf(tracker.working_hours));
            accessPoints = (ArrayList<AccessPoint>) datasource.getAllAccessPoints(tracker.id);
        }

        accessPointAdapter = new AccessPointAdapter(this, R.layout.list_2_lines, accessPoints);
        setListAdapter(accessPointAdapter);
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
            onChooseWifi(button_wifi);
            return true;
        case R.id.action_delete:
            AccessPoint accessPoint = accessPoints.remove(info.position);
            datasource.delete(accessPoint);
            accessPointAdapter.notifyDataSetChanged();
            return true;
        default:
            return super.onContextItemSelected(item);
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
                setWifiIconColor(BLUE);

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
        tracker.verbose_name = verbose_name;
        tracker.working_hours = Float.parseFloat(working_hours);

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

        if (accessPoints.size() == 0) {
            setWifiIconColor(RED);
            return false;
        }

        TrackerEntry other = datasource.getTracker(verbose_name);

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

    private void setWifiIconColor(int color) {
        Resources res = getResources();
        Drawable icon = res.getDrawable(R.drawable.ic_wifi_add);
        icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        button_wifi.setBackgroundResource(R.drawable.ic_wifi_add);
        button_wifi.invalidate();
        edit_tracker_verbose_name.setBackgroundColor(Color.TRANSPARENT);
        edit_tracker_working_hours.setBackgroundColor(Color.TRANSPARENT);
        edit_tracker_verbose_name.invalidate();
        edit_tracker_working_hours.invalidate();
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
