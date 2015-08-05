package com.firebirdberlin.tinytimetracker;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.os.Handler;
import java.lang.Runnable;

public class MainFragment extends Fragment {

    private Handler viewHandler = new Handler();
    private MainView timeView = null;

    Runnable updateView = new Runnable(){
        @Override
        public void run(){
            timeView.invalidate();
            viewHandler.postDelayed(updateView, 20000);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.main_fragment, container, false);
        timeView = (MainView) v.findViewById(R.id.main_time_view);
        updateView.run();
        return v;
    }

}
