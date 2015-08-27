package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainFragment extends Fragment {
    private Spinner spinner = null;
    private MainView timeView = null;
    private TinyTimeTracker mContext = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setRetainInstance(true);
        mContext = (TinyTimeTracker) getActivity();
        View v = inflater.inflate(R.layout.main_fragment, container, false);
        spinner = (Spinner) v.findViewById(R.id.spinner_trackers);

        LogDataSource datasource = new LogDataSource(getActivity());
        List<TrackerEntry> trackers = datasource.getTrackers();
        datasource.close();

        ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                                                R.layout.main_spinner,
                                                trackers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                TrackerEntry tracker = (TrackerEntry) parentView.getItemAtPosition(position);
                mContext.setCurrentTracker(tracker);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        timeView = (MainView) v.findViewById(R.id.main_time_view);
        return v;
    }

    public void refresh(Context context) {
        if (timeView == null || spinner == null) {
            return;
        }
        timeView.invalidate();
        String ssid = Settings.getTrackedSSID(context);
        int spinnerPosition = ((ArrayAdapter) spinner.getAdapter()).getPosition(ssid);
        spinner.setSelection(spinnerPosition);
    }
}
