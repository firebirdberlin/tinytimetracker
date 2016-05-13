package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;


public class MainFragment extends Fragment implements View.OnClickListener {
    private static String TAG = TinyTimeTracker.TAG + ".MainFragment";
    private ImageButton button_toggle_wifi = null;
    private Spinner spinner = null;
    private MainView timeView = null;
    private List<TrackerEntry> trackers = new ArrayList<TrackerEntry>();
    private Map<Long, Integer> trackerIDToSelectionIDMap = new HashMap<Long, Integer>();
    EventBus bus = EventBus.getDefault();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        bus.register(this);
        View v = inflater.inflate(R.layout.main_fragment, container, false);
        spinner = (Spinner) v.findViewById(R.id.spinner_trackers);
        button_toggle_wifi = (ImageButton) v.findViewById(R.id.button_toggle_wifi);
        button_toggle_wifi.setOnClickListener(this);
        loadTrackers();
        ArrayAdapter<TrackerEntry> adapter = new ArrayAdapter<TrackerEntry>(getActivity(),
                                                                            R.layout.main_spinner,
                                                                            trackers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        long lastTrackerID = Settings.getLastTrackerID(getActivity());
        setSelection(lastTrackerID);

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                TrackerEntry tracker = (TrackerEntry) parentView.getItemAtPosition(position);
                Log.i(TAG, "Tracker selected " + tracker.verbose_name);
                EventBus bus = EventBus.getDefault();
                bus.post(new OnTrackerSelected(tracker));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        timeView = (MainView) v.findViewById(R.id.main_time_view);
        return v;
    }

    void setSelection(long trackerID) {
        if (trackerIDToSelectionIDMap.containsKey(trackerID)) {
            int item = trackerIDToSelectionIDMap.get(trackerID);
            spinner.setSelection(item);
        }
    }

    private void loadTrackers() {
        LogDataSource datasource = new LogDataSource(getActivity());
        datasource.open();
        List<TrackerEntry> trackers_loaded = datasource.getTrackers();
        trackers.clear();
        trackerIDToSelectionIDMap.clear();

        for (TrackerEntry e : trackers_loaded) {
            trackerIDToSelectionIDMap.put(e.id, trackers.size());
            trackers.add(e);
        }

        datasource.close();
    }

    @Override
    public void onClick(View v) {
        if ( v.equals(button_toggle_wifi) ) {
            Log.i(TAG, "onClickToggleWifi clicked");
            TrackerEntry tracker = (TrackerEntry) spinner.getSelectedItem();
            if (tracker == null) return;

            switch (tracker.operation_state) {
                case TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED:
                    button_toggle_wifi.setImageResource(R.drawable.ic_wifi);
                    tracker.operation_state = TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED;
                    break;
                case TrackerEntry.OPERATION_STATE_AUTOMATIC:
                case TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED:
                    button_toggle_wifi.setImageResource(R.drawable.ic_no_wifi);
                    tracker.operation_state = TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED;
                default:
                    break;
            }
            LogDataSource datasource = new LogDataSource(getActivity());
            datasource.save(tracker);
            datasource.close();
            button_toggle_wifi.invalidate();
            Log.i(TAG, "onClickToggleWifi click done ...");
        }
    }

    private void setWifiIndicator(TrackerEntry tracker) {
        switch (tracker.operation_state) {
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED:
                button_toggle_wifi.setImageResource(R.drawable.ic_no_wifi);
                break;
            case TrackerEntry.OPERATION_STATE_AUTOMATIC:
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED:
                button_toggle_wifi.setImageResource(R.drawable.ic_wifi);
            default:
                break;
        }
        button_toggle_wifi.invalidate();
    }

    @SuppressWarnings("unchecked")
    public void onEvent(OnTrackerAdded event) {
        Log.i(TAG, "OnTrackerAdded");
        ArrayAdapter<TrackerEntry> adapter = (ArrayAdapter<TrackerEntry>) spinner.getAdapter();
        trackerIDToSelectionIDMap.put(event.tracker.id, trackers.size());
        adapter.add(event.tracker);
        adapter.notifyDataSetChanged();
        setSelection(event.tracker.id);
    }

    public void onEvent(OnTrackerChanged event) {
        Log.i(TAG, "OnTrackerChanged");
        loadTrackers();
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.notifyDataSetChanged();
    }

    public void onEvent(OnTrackerSelected event) {
        Log.i(TAG, "OnTrackerSelected");
        if ( event == null || event.newTracker == null) return;
        setWifiIndicator(event.newTracker);
    }

    @SuppressWarnings("unchecked")
    public void onEvent(OnTrackerDeleted event) {
        Log.i(TAG, "OnTrackerDeleted");
        ArrayAdapter<TrackerEntry> adapter = (ArrayAdapter<TrackerEntry>) spinner.getAdapter();
        adapter.remove(event.tracker);

        if (adapter.getCount() > 0) {
            spinner.setSelection(0, true);
            TrackerEntry tracker = (TrackerEntry) spinner.getItemAtPosition(0);
            Log.i(TAG, "Tracker selected " + tracker.verbose_name);
            EventBus bus = EventBus.getDefault();
            bus.post(new OnTrackerSelected(tracker));
        }

        adapter.notifyDataSetChanged();
    }

    public void onEvent(OnDatabaseImported event) {
        Log.i(TAG, "OnDatabaseImported");
        loadTrackers();
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.notifyDataSetChanged();
    }
}
