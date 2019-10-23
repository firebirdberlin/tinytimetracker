package com.firebirdberlin.tinytimetracker.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.firebirdberlin.tinytimetracker.LogDataSource;
import com.firebirdberlin.tinytimetracker.R;
import com.firebirdberlin.tinytimetracker.Settings;
import com.firebirdberlin.tinytimetracker.TinyTimeTracker;
import com.firebirdberlin.tinytimetracker.events.OnDatabaseImported;
import com.firebirdberlin.tinytimetracker.events.OnLocationModeChanged;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryChanged;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerAdded;
import com.firebirdberlin.tinytimetracker.events.OnTrackerChanged;
import com.firebirdberlin.tinytimetracker.events.OnTrackerDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerSelected;
import com.firebirdberlin.tinytimetracker.events.OnWifiUpdateCompleted;
import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainFragment extends Fragment implements View.OnClickListener {
    private static String TAG = "MainFragment";
    EventBus bus = EventBus.getDefault();
    private Button button_toggle_wifi = null;
    private Button button_toggle_clockin_state = null;
    private Spinner spinner = null;
    private TextView textviewMeanDuration = null;
    private TextView textviewSaldo = null;
    private CardView cardviewLocationProviderOff = null;
    private CardView cardviewLocationPermission = null;
    private View trackerToolbar = null;
    private MainView timeView = null;
    private List<TrackerEntry> trackers = new ArrayList<>();
    private Map<Long, Integer> trackerIDToSelectionIDMap = new HashMap<Long, Integer>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.main_fragment, container, false);
        spinner = v.findViewById(R.id.spinner_trackers);
        textviewMeanDuration = v.findViewById(R.id.textview_mean_value);
        textviewSaldo = v.findViewById(R.id.textview_saldo);
        cardviewLocationProviderOff = v.findViewById(R.id.cardview_warn_gps_off);
        cardviewLocationPermission = v.findViewById(R.id.cardview_warn_location_permission_not_granted);
        trackerToolbar = v.findViewById(R.id.tracker_toolbar);
        button_toggle_wifi = v.findViewById(R.id.button_toggle_wifi);
        button_toggle_clockin_state = v.findViewById(R.id.button_toggle_clockin_state);
        button_toggle_wifi.setOnClickListener(this);
        button_toggle_clockin_state.setOnClickListener(this);
        trackerToolbar.setVisibility(View.GONE);

        loadTrackers();
        ArrayAdapter<TrackerEntry> adapter = new ArrayAdapter<>(getActivity(),
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
                postTrackerSelected(tracker);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        timeView = v.findViewById(R.id.main_time_view);
        timeView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                handleClockinStateChange();
                return true;
            }
        });

        final Button buttonLocationProviders = v.findViewById(R.id.button_location_providers);
        buttonLocationProviders.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent viewIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(viewIntent);
            }
        });

        final Button buttonLocationPermission = v.findViewById(R.id.button_grant_location_permission);
        buttonLocationPermission.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TinyTimeTracker.checkAndRequestPermission(
                        (AppCompatActivity) getActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        1
                );
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        bus.register(this);

        setupWarnings();
        loadTrackers();

        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.notifyDataSetChanged();
        updateStatisticalValues(TinyTimeTracker.currentTracker);
    }

    private void setupWarnings() {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean shallShowWifiCard = false;
            for (TrackerEntry tracker : trackers) {
                if (tracker.operation_state == TrackerEntry.OPERATION_STATE_AUTOMATIC
                        || tracker.operation_state == TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED) {
                    shallShowWifiCard = true;
                    break;
                }
            }

            if (shallShowWifiCard && !TinyTimeTracker.isLocationEnabled(getActivity())) {
                cardviewLocationProviderOff.setVisibility(View.VISIBLE);
            } else {
                cardviewLocationProviderOff.setVisibility(View.GONE);
            }


            if (shallShowWifiCard
                    && !TinyTimeTracker.hasPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                cardviewLocationPermission.setVisibility(View.VISIBLE);
            } else {
                cardviewLocationPermission.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        bus.unregister(this);
    }

    private void setSelection(long trackerID) {
        int item = 0;
        if (trackerIDToSelectionIDMap.containsKey(trackerID)) {
            item = trackerIDToSelectionIDMap.get(trackerID);
        }
        int count = spinner.getAdapter().getCount();
        if ( count > 0 && item < count ) {
            spinner.setSelection(item);
            TrackerEntry tracker = (TrackerEntry) spinner.getItemAtPosition(item);
            postTrackerSelected(tracker);
        }
    }

    private void loadTrackers() {
        LogDataSource datasource = new LogDataSource(getActivity());
        datasource.open();
        List<TrackerEntry> trackers_loaded = datasource.getTrackers();
        trackers.clear();

        trackers.addAll(trackers_loaded);
        sortTrackers();

        datasource.close();
    }

    private void sortTrackers() {
        Collections.sort(trackers, new Comparator<TrackerEntry>() {
            @Override
            public int compare(TrackerEntry e1, TrackerEntry e2) {
                return e1.verbose_name.compareTo(e2.verbose_name);
            }
        });

        trackerIDToSelectionIDMap.clear();
        for (int i = 0; i < trackers.size(); i++) {
            TrackerEntry e = trackers.get(i);
            trackerIDToSelectionIDMap.put(e.id, i);
        }
    }

    @Override
    public void onClick(View v) {
        TrackerEntry tracker = (TrackerEntry) spinner.getSelectedItem();
        if (tracker == null) return;

        if ( v.equals(button_toggle_wifi) ) {
            Log.i(TAG, "button_toggle_wifi clicked");
            boolean shallCheckPerms =
                    (tracker.operation_state == TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED);

            switch (tracker.operation_state) {
                case TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED:
                    tracker.operation_state = TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED;
                    break;
                case TrackerEntry.OPERATION_STATE_AUTOMATIC:
                case TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED:
                    tracker.operation_state = TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED;
                default:
                    break;
            }

            LogDataSource datasource = new LogDataSource(getActivity());
            datasource.save(tracker);
            datasource.close();
            setWifiIndicator(tracker);

            if (shallCheckPerms) {
                TinyTimeTracker.checkAndRequestPermission(
                        (AppCompatActivity) getActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        1
                );
            }
            setupWarnings();
            Log.i(TAG, "button_toggle_wifi click done ...");
        } else
        if ( v.equals(button_toggle_clockin_state) ) {
            handleClockinStateChange();
        }
    }

    private void handleClockinStateChange() {
        TrackerEntry tracker = (TrackerEntry) spinner.getSelectedItem();
        if (tracker == null) return;
        Log.i(TAG, "button_toggle_clockin_state clicked");
        LogDataSource datasource = new LogDataSource(getActivity());
        long now = System.currentTimeMillis();
        switch (tracker.operation_state) {
            case TrackerEntry.OPERATION_STATE_AUTOMATIC:
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED:
                // set start timestamp
                datasource.addTimeStamp(tracker, now, now);
                tracker.operation_state = TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE;
                break;
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED:
                // set start timestamp
                datasource.addTimeStamp(tracker, now, now);
                tracker.operation_state = TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI;
                break;
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE:
                // set end timestamp and return to previous mode
                LogEntry logEntry = datasource.getLatestLogEntry(tracker.id);
                logEntry.setTimestampEnd(now);
                datasource.save(logEntry);
                tracker.operation_state = TrackerEntry.OPERATION_STATE_AUTOMATIC;
                break;
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI:
                // set end timestamp and return to previous mode
                LogEntry logEntry2 = datasource.getLatestLogEntry(tracker.id);
                logEntry2.setTimestampEnd(now);
                datasource.save(logEntry2);
                tracker.operation_state = TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED;
                break;
            default:
                break;
        }
        datasource.save(tracker);
        datasource.close();

        setClockinStateIndicator(tracker);
        setWifiIndicator(tracker);
        Log.i(TAG, "button_toggle_clockin_state click done ...");
    }

    private void setWifiIndicator(TrackerEntry tracker) {
        boolean visible = true;
        switch (tracker.operation_state) {
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE:
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI:
                visible = false;
                break;
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED:
                button_toggle_wifi.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_no_wifi_blue_24dp, 0, 0, 0);
                button_toggle_wifi.setText(R.string.label_auto_detection_off);
                break;
            case TrackerEntry.OPERATION_STATE_AUTOMATIC:
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED:
                button_toggle_wifi.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_blue_24dp, 0, 0, 0);
                button_toggle_wifi.setText(R.string.label_auto_detection_on);
            default:
                break;
        }

        if (visible) {
            ViewPropertyAnimator animator = button_toggle_wifi.animate().setStartDelay(300).setDuration(300).alpha(1.f);
            animator.setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    button_toggle_wifi.setVisibility(View.VISIBLE);
                }
            }).start();
        } else {
            ViewPropertyAnimator animator = button_toggle_wifi.animate().setDuration(300).alpha(0.f);
            animator.setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    button_toggle_wifi.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                }
            }).start();
        }
        button_toggle_wifi.invalidate();
    }


    private void setClockinStateIndicator(TrackerEntry tracker) {
        switch (tracker.operation_state) {
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE:
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI:
                View parent = (View) button_toggle_clockin_state.getParent();
                int parent_width = parent.getWidth();
                int new_x = (parent_width - button_toggle_clockin_state.getWidth()) / 2;
                button_toggle_clockin_state.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop_blue_24dp, 0, 0, 0);
                button_toggle_clockin_state.setText(R.string.label_toggle_clockin_state_end);
                button_toggle_clockin_state.animate().setStartDelay(600).setDuration(300).x(new_x);
                timeView.setActivated();
                break;
            default:
                button_toggle_clockin_state.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_blue_24dp, 0, 0, 0);
                button_toggle_clockin_state.setText(R.string.label_toggle_clockin_state_start);
                button_toggle_clockin_state.animate().setStartDelay(0).setDuration(300).x(0);
                timeView.setDeactivated();
                break;
        }
        button_toggle_clockin_state.invalidate();
    }

    // UI updates must run on MainThread
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(OnTrackerAdded event) {
        Log.d(TAG, "OnTrackerAdded: a tracker was added: " + event.tracker.verbose_name);
        handleOnTrackerAdded(event);
        bus.removeStickyEvent(OnTrackerAdded.class);
    }

    @SuppressWarnings("unchecked")
    private void handleOnTrackerAdded(OnTrackerAdded event) {
        ArrayAdapter<TrackerEntry> adapter = (ArrayAdapter<TrackerEntry>) spinner.getAdapter();
        adapter.add(event.tracker);
        sortTrackers();
        adapter.notifyDataSetChanged();
        setSelection(event.tracker.id);
    }

    @Subscribe
    public void onEvent(OnTrackerChanged event) {
        Log.i(TAG, "OnTrackerChanged");
        loadTrackers();
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.notifyDataSetChanged();
        if (TinyTimeTracker.currentTracker != null &&
                TinyTimeTracker.currentTracker.id == event.tracker.id) {
            updateStatisticalValues(TinyTimeTracker.currentTracker);
        }
    }

    @Subscribe
    public void onEvent(OnTrackerSelected event) {
        Log.i(TAG, "OnTrackerSelected");
        if ( event == null || event.newTracker == null) return;
        trackerToolbar.setVisibility(View.VISIBLE);

        setWifiIndicator(event.newTracker);
        setClockinStateIndicator(event.newTracker);
        updateStatisticalValues(event.newTracker);
    }

    @Subscribe
    public void onEvent(OnWifiUpdateCompleted event) {
        if ( TinyTimeTracker.currentTracker == null ) return;
        if (event.success && TinyTimeTracker.currentTracker.equals(event.tracker)) {
            updateStatisticalValues(event.tracker);
        }
    }

    @SuppressWarnings("unchecked")
    @Subscribe
    public void onEvent(OnTrackerDeleted event) {
        Log.i(TAG, "OnTrackerDeleted");
        textviewMeanDuration.setText("");
        textviewSaldo.setText("");
        ArrayAdapter<TrackerEntry> adapter = (ArrayAdapter<TrackerEntry>) spinner.getAdapter();
        adapter.remove(event.tracker);
        trackerToolbar.setVisibility(View.GONE);

        if (adapter.getCount() > 0) {
            spinner.setSelection(0, true);
            TrackerEntry tracker = (TrackerEntry) spinner.getItemAtPosition(0);
            postTrackerSelected(tracker);
        }

        adapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onEvent(OnLogEntryDeleted event) {
        if ( TinyTimeTracker.currentTracker != null && TinyTimeTracker.currentTracker.id == event.tracker_id ) {
            updateStatisticalValues(TinyTimeTracker.currentTracker);
        }
    }

    @Subscribe
    public void onEvent(OnLogEntryChanged event) {
        if ( TinyTimeTracker.currentTracker != null && TinyTimeTracker.currentTracker.id == event.entry.tracker_id ) {
            updateStatisticalValues(TinyTimeTracker.currentTracker);
        }
    }

    @Subscribe
    public void onEvent(OnDatabaseImported event) {
        Log.i(TAG, "OnDatabaseImported");
        loadTrackers();
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onEvent(OnLocationModeChanged event) {
        onResume();
    }

    private void updateStatisticalValues(TrackerEntry tracker){
        if (tracker == null) return;

        UnixTimestamp todayThreeYearsAgo = UnixTimestamp.todayThreeYearsAgo();
        LogDataSource datasource = new LogDataSource(getActivity());
        Pair<Long, Long> totalDurationPair = datasource.getTotalDurationPairSince(todayThreeYearsAgo.getTimestamp(), tracker.id);

        long meanDurationMillis = tracker.getMeanDurationMillis(totalDurationPair.first, totalDurationPair.second);
        UnixTimestamp meanDuration = new UnixTimestamp(meanDurationMillis);
        String text = meanDuration.durationAsHours();
        textviewMeanDuration.setText(text);


        int workingHoursInSeconds = (int) (tracker.working_hours * 3600.f);
        if ( workingHoursInSeconds > 0 ) {
            Long overTimeMillis = tracker.getOvertimeMillis(totalDurationPair.first, totalDurationPair.second);

            int timeBalanceInMinutes = datasource.getManualTimeBalanceInMinutes(tracker);
            overTimeMillis += timeBalanceInMinutes * 60L * 1000L;

            UnixTimestamp overtime = new UnixTimestamp(overTimeMillis);
            String sign = (overTimeMillis < 0 ) ? "- ": "+ ";
            String textSaldo = sign + overtime.durationAsHours();
            textviewSaldo.setText(textSaldo);
            textviewSaldo.setVisibility(View.VISIBLE);
        } else {
            textviewSaldo.setVisibility(View.INVISIBLE);
        }
        datasource.close();
    }

    private void postTrackerSelected(TrackerEntry tracker) {
        Log.i(TAG, "Tracker selected " + tracker.verbose_name);
        EventBus bus = EventBus.getDefault();
        TinyTimeTracker.currentTracker = tracker;
        bus.postSticky(new OnTrackerSelected(tracker));

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("currentTrackerId", tracker.id);
        prefEditor.commit();
    }
}
