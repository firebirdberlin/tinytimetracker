package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class MainFragment extends Fragment {
    private static String TAG = TinyTimeTracker.TAG + ".MainFragment";
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
        loadTrackers();
        ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                                                R.layout.main_spinner,
                                                trackers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        long lastTrackerID = Settings.getLastTrackerID(getActivity());
        if (trackerIDToSelectionIDMap.containsKey(lastTrackerID)) {
            int item = trackerIDToSelectionIDMap.get(lastTrackerID);
            spinner.setSelection(item);
        }

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

    public void onEvent(OnTrackerAdded event) {
        Log.i(TAG, "OnTrackerAdded");
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.add(event.tracker);
        adapter.notifyDataSetChanged();
    }

    public void onEvent(OnTrackerChanged event) {
        Log.i(TAG, "OnTrackerChanged");
        loadTrackers();
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.notifyDataSetChanged();
    }

    public void onEvent(OnTrackerDeleted event) {
        Log.i(TAG, "OnTrackerDeleted");
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.remove(event.tracker);
        adapter.notifyDataSetChanged();
    }

    public void onEvent(OnDatabaseImported event) {
        Log.i(TAG, "OnDatabaseImported");
        loadTrackers();
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.notifyDataSetChanged();
    }
}
