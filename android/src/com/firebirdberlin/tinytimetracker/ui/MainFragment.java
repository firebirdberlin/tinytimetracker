package com.firebirdberlin.tinytimetracker.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import android.support.v7.widget.CardView;
import de.greenrobot.event.EventBus;

import com.firebirdberlin.tinytimetracker.R;
import com.firebirdberlin.tinytimetracker.CustomViewPager;
import com.firebirdberlin.tinytimetracker.LogDataSource;
import com.firebirdberlin.tinytimetracker.TinyTimeTracker;
import com.firebirdberlin.tinytimetracker.Settings;
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


public class MainFragment extends Fragment implements View.OnClickListener {
    private static String TAG = TinyTimeTracker.TAG + ".MainFragment";
    private Button button_toggle_wifi = null;
    private Button button_toggle_clockin_state = null;
    private Spinner spinner = null;
    private TextView textviewMeanDuration = null;
    private TextView textviewSaldo = null;
    private CustomViewPager pager = null;
    private CardView cardviewLocationProviderOff = null;
    private TrackerEntry currentTracker = null;
    private View trackerToolbar = null;
    private MainView timeView = null;
    private List<TrackerEntry> trackers = new ArrayList<TrackerEntry>();
    private Map<Long, Integer> trackerIDToSelectionIDMap = new HashMap<Long, Integer>();
    EventBus bus = EventBus.getDefault();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        pager = (CustomViewPager) container;
        View v = inflater.inflate(R.layout.main_fragment, container, false);
        spinner = (Spinner) v.findViewById(R.id.spinner_trackers);
        textviewMeanDuration = (TextView) v.findViewById(R.id.textview_mean_value);
        textviewSaldo = (TextView) v.findViewById(R.id.textview_saldo);
        cardviewLocationProviderOff = (CardView) v.findViewById(R.id.cardview_warn_gps_off);
        trackerToolbar = (View) v.findViewById(R.id.tracker_toolbar);
        button_toggle_wifi = (Button) v.findViewById(R.id.button_toggle_wifi);
        button_toggle_clockin_state = (Button) v.findViewById(R.id.button_toggle_clockin_state);
        button_toggle_wifi.setOnClickListener(this);
        button_toggle_clockin_state.setOnClickListener(this);
        trackerToolbar.setVisibility(View.GONE);
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
                bus.postSticky(new OnTrackerSelected(tracker));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        timeView = (MainView) v.findViewById(R.id.main_time_view);
        timeView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                handleClockinStateChange();
                return true;
            }
        });

        final Button buttonLocationProviders = (Button) v.findViewById(R.id.button_location_providers);
        buttonLocationProviders.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent viewIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(viewIntent);
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        bus.register(this);

        if (Build.VERSION.SDK_INT >= 23){
            if ( ! isLocationEnabled(getActivity()) ) {
                cardviewLocationProviderOff.setVisibility(View.VISIBLE);
            } else {
                cardviewLocationProviderOff.setVisibility(View.GONE);
            }
        }

        OnTrackerAdded event = bus.removeStickyEvent(OnTrackerAdded.class);
        if(event != null) {
            handleOnTrackerAdded(event);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        bus.unregister(this);
    }

    void setSelection(long trackerID) {
        if (trackerIDToSelectionIDMap.containsKey(trackerID)) {
            int item = trackerIDToSelectionIDMap.get(trackerID);
            spinner.setSelection(item);
            TrackerEntry tracker = (TrackerEntry) spinner.getItemAtPosition(item);
            Log.i(TAG, "Tracker selected " + tracker.verbose_name);
            EventBus bus = EventBus.getDefault();
            bus.postSticky(new OnTrackerSelected(tracker));
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
        TrackerEntry tracker = (TrackerEntry) spinner.getSelectedItem();
        if (tracker == null) return;

        if ( v.equals(button_toggle_wifi) ) {
            Log.i(TAG, "button_toggle_wifi clicked");
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
            Log.i(TAG, "button_toggle_wifi click done ...");
            return;
        }

        if ( v.equals(button_toggle_clockin_state) ) {
            handleClockinStateChange();
            return;
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
                datasource.addTimeStamp(tracker, now, 0);
                tracker.operation_state = TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE;
                break;
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED:
                // set start timestamp
                datasource.addTimeStamp(tracker, now, 0);
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
        return;
    }
    private void setWifiIndicator(TrackerEntry tracker) {
        boolean visible = true;
        switch (tracker.operation_state) {
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE:
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI:
                visible = false;
                break;
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED:
                button_toggle_wifi.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_no_wifi, 0, 0, 0);
                button_toggle_wifi.setText(R.string.label_auto_detection_off);
                break;
            case TrackerEntry.OPERATION_STATE_AUTOMATIC:
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED:
                button_toggle_wifi.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi, 0, 0, 0);
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
                button_toggle_clockin_state.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0);
                button_toggle_clockin_state.setText(R.string.label_toggle_clockin_state_end);
                button_toggle_clockin_state.animate().setStartDelay(600).setDuration(300).x(new_x);
                timeView.setActivated();
                break;
            default:
                button_toggle_clockin_state.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
                button_toggle_clockin_state.setText(R.string.label_toggle_clockin_state_start);
                button_toggle_clockin_state.animate().setStartDelay(0).setDuration(300).x(0);
                timeView.setDeactivated();
                break;
        }
        button_toggle_clockin_state.invalidate();
    }

    public void handleOnTrackerAdded(OnTrackerAdded event) {
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
        if (currentTracker != null && currentTracker.id == event.tracker.id) {
            updateStatisticalValues(currentTracker);
        }
    }

    public void onEvent(OnTrackerSelected event) {
        Log.i(TAG, "OnTrackerSelected");
        if ( event == null || event.newTracker == null) return;
        currentTracker = event.newTracker;
        trackerToolbar.setVisibility(View.VISIBLE);

        setWifiIndicator(event.newTracker);
        setClockinStateIndicator(event.newTracker);
        updateStatisticalValues(event.newTracker);
    }

    public void onEvent(OnWifiUpdateCompleted event) {
        if ( currentTracker == null ) return;
        if (event.success && currentTracker.equals(event.tracker)) {
            updateStatisticalValues(event.tracker);
        }
    }

    @SuppressWarnings("unchecked")
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
            Log.i(TAG, "Tracker selected " + tracker.verbose_name);
            EventBus bus = EventBus.getDefault();
            bus.postSticky(new OnTrackerSelected(tracker));
        }

        adapter.notifyDataSetChanged();
    }

    public void onEvent(OnLogEntryDeleted event) {
        if ( currentTracker != null && currentTracker.id == event.tracker_id ) {
            updateStatisticalValues(currentTracker);
        }
    }

    public void onEvent(OnLogEntryChanged event) {
        if ( currentTracker != null && currentTracker.id == event.entry.tracker_id ) {
            updateStatisticalValues(currentTracker);
        }
    }

    public void onEvent(OnDatabaseImported event) {
        Log.i(TAG, "OnDatabaseImported");
        loadTrackers();
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.notifyDataSetChanged();
    }

    public void onEvent(OnLocationModeChanged event) {
        onResume();
    }

    private void updateStatisticalValues(TrackerEntry tracker){
        UnixTimestamp todayThreeYearsAgo = UnixTimestamp.todayThreeYearsAgo();
        LogDataSource datasource = new LogDataSource(getActivity());
        Pair<Long, Long> totalDurationPair = datasource.getTotalDurationPairSince(todayThreeYearsAgo.getTimestamp(), tracker.id);
        datasource.close();
        long meanDurationMillis = tracker.getMeanDurationMillis(totalDurationPair.first, totalDurationPair.second);
        UnixTimestamp meanDuration = new UnixTimestamp(meanDurationMillis);
        String text = meanDuration.durationAsHours();
        textviewMeanDuration.setText(text);


        int workingHoursInSeconds = (int) (tracker.working_hours * 3600.f);
        if ( workingHoursInSeconds > 0 ) {
            Long overTimeMillis = tracker.getOvertimeMillis(totalDurationPair.first, totalDurationPair.second);
            UnixTimestamp overtime = new UnixTimestamp(overTimeMillis);

            String sign = (overTimeMillis < 0 ) ? "- ": "+ ";
            String textSaldo = sign + overtime.durationAsHours();
            textviewSaldo.setText(textSaldo);
            textviewSaldo.setVisibility(View.VISIBLE);
        } else {
            textviewSaldo.setVisibility(View.INVISIBLE);
        }
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Secure.getInt(context.getContentResolver(), Secure.LOCATION_MODE);

            } catch (SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != Secure.LOCATION_MODE_OFF;

        }else{
            String locationProviders = Secure.getString(context.getContentResolver(),
                                                        Secure.LOCATION_PROVIDERS_ALLOWED);
            return !locationProviders.isEmpty();
        }
    }
}
