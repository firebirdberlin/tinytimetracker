package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFragment extends Fragment {
    private MainView timeView = null;
    private TextView textViewCurrentTracker = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.main_fragment, container, false);
        timeView = (MainView) v.findViewById(R.id.main_time_view);
        textViewCurrentTracker = (TextView) v.findViewById(R.id.textview_current_tracker);
        textViewCurrentTracker.setText(Settings.getTrackedSSID(getActivity()));
        return v;
    }

    public void refresh(Context context) {
        if (timeView == null || textViewCurrentTracker == null) {
            return;
        }
        timeView.invalidate();
        textViewCurrentTracker.setText(Settings.getTrackedSSID(context));
    }
}
