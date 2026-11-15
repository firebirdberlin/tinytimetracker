package com.firebirdberlin.tinytimetracker.ui;

import static com.firebirdberlin.tinytimetracker.TinyTimeTrackerApp.getContext;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.firebirdberlin.tinytimetracker.LogDataSource;
import com.firebirdberlin.tinytimetracker.R;
import com.firebirdberlin.tinytimetracker.Settings;
import com.firebirdberlin.tinytimetracker.TinyTimeTracker;
import com.firebirdberlin.tinytimetracker.Utility;
import com.firebirdberlin.tinytimetracker.events.OnDatabaseImported;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryChanged;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerAdded;
import com.firebirdberlin.tinytimetracker.events.OnTrackerChanged;
import com.firebirdberlin.tinytimetracker.events.OnTrackerDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerSelected;
import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;
import com.firebirdberlin.tinytimetracker.models.WorkTimeStatistics;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private Button button_buy = null;
    private Button button_later = null;
    private Spinner spinner = null;
    private TextView textviewCummulatedTime = null;
    private TextView textviewMeanDuration = null;
    private TextView textviewSaldo = null;
    private TextView textviewWhen = null;
    private TextView textviewUpgradePro = null;
    private CardView cardviewProVersion = null;
    private MainView timeView = null;
    private final List<TrackerEntry> trackers = new ArrayList<>();
    private final Map<Long, Integer> trackerIDToSelectionIDMap = new HashMap<>();
    private FloatingActionButton actionButtonStart;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.main_fragment, container, false);
        actionButtonStart = v.findViewById(R.id.action_button_start);
        actionButtonStart.setOnClickListener(this);
        spinner = v.findViewById(R.id.spinner_trackers);
        textviewMeanDuration = v.findViewById(R.id.textview_mean_value);
        textviewSaldo = v.findViewById(R.id.textview_saldo);
        textviewWhen = v.findViewById(R.id.textview_when);
        textviewCummulatedTime = v.findViewById(R.id.textview_cummulated_time);
        cardviewProVersion = v.findViewById(R.id.cardview_upgrade_pro);
        textviewUpgradePro = v.findViewById(R.id.textview_upgrade_pro);
            button_buy = v.findViewById(R.id.button_update_pro_version);
        button_buy.setOnClickListener(this);
        button_later = v.findViewById(R.id.button_update_later);
        button_later.setOnClickListener(this);

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
        timeView.setOnLongClickListener(v1 -> {
            handleClockinStateChange();
            return true;
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
        TinyTimeTracker mainActivity = (TinyTimeTracker) getActivity();
        if (
                mainActivity != null
                && !(mainActivity.isPurchased(TinyTimeTracker.ITEM_PRO)
                        || mainActivity.isPurchased(TinyTimeTracker.ITEM_DONATION))
                && Settings.shallAskForUpgrade(getContext())
        ) {
            int backgroundColor = Utility.getRandomMaterialColor(getContext());
            int textColor = Utility.getContrastColor(backgroundColor);
            cardviewProVersion.setBackgroundColor(backgroundColor);
            textviewUpgradePro.setTextColor(textColor);
            button_buy.setTextColor(textColor);
            button_later.setTextColor(textColor);
            cardviewProVersion.setVisibility(View.VISIBLE);
        } else {
            cardviewProVersion.setVisibility(View.GONE);
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

        if (v.equals(actionButtonStart)) {
            handleClockinStateChange();
        } else
        if (v.equals(button_buy)) {
            TinyTimeTracker mainActivity = (TinyTimeTracker) getActivity();
            mainActivity.launchBillingFlow(TinyTimeTracker.ITEM_PRO);
            cardviewProVersion.setVisibility(View.GONE);
        } else
        if (v.equals(button_later)) {
            Settings.setNextUpgradeRequestTime(getContext());
            cardviewProVersion.setVisibility(View.GONE);
        }
    }

    private void handleClockinStateChange() {
        TrackerEntry tracker = (TrackerEntry) spinner.getSelectedItem();
        if (tracker == null) return;
        LogDataSource datasource = new LogDataSource(getActivity());
        long now = System.currentTimeMillis();
        switch (tracker.operation_state) {
            case TrackerEntry.OPERATION_STATE_AUTOMATIC:
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_RESUMED:
            case TrackerEntry.OPERATION_STATE_AUTOMATIC_PAUSED:
                // set start timestamp
                datasource.addTimeStamp(tracker, now, now);
                tracker.operation_state = TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE;
                break;
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE:
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI:
                // set end timestamp and return to previous mode
                LogEntry logEntry = datasource.getLatestLogEntry(tracker.id);
                if (logEntry != null) {
                    logEntry.setTimestampEnd(now);
                    datasource.save(logEntry);
                }
                tracker.operation_state = TrackerEntry.OPERATION_STATE_AUTOMATIC;
                break;
            default:
                break;
        }
        datasource.save(tracker);
        datasource.close();

        setClockinStateIndicator(tracker);
    }

    private void setClockinStateIndicator(TrackerEntry tracker) {
        switch (tracker.operation_state) {
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE:
            case TrackerEntry.OPERATION_STATE_MANUAL_ACTIVE_NO_WIFI:
                timeView.setActivated();
                int colorActive = getResources().getColor(R.color.highlightActive);
                actionButtonStart.setBackgroundTintList(ColorStateList.valueOf(colorActive));
                actionButtonStart.setImageResource(R.drawable.ic_stop_blue_24dp);
                break;
            default:
                timeView.setDeactivated();
                int color = getResources().getColor(R.color.highlight);
                actionButtonStart.setBackgroundTintList(ColorStateList.valueOf(color));
                actionButtonStart.setImageResource(R.drawable.ic_play_blue_24dp);
                break;
        }
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

    @Subscribe(threadMode = ThreadMode.MAIN)
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnTrackerSelected event) {
        Log.i(TAG, "OnTrackerSelected");
        if ( event == null || event.newTracker == null) return;

        setClockinStateIndicator(event.newTracker);
        updateStatisticalValues(event.newTracker);
    }

    @SuppressWarnings("unchecked")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnTrackerDeleted event) {
        Log.i(TAG, "OnTrackerDeleted");
        textviewCummulatedTime.setText("");
        textviewMeanDuration.setText("");
        textviewSaldo.setText("");
        textviewWhen.setText("");
        ArrayAdapter<TrackerEntry> adapter = (ArrayAdapter<TrackerEntry>) spinner.getAdapter();
        adapter.remove(event.tracker);

        if (adapter.getCount() > 0) {
            spinner.setSelection(0, true);
            TrackerEntry tracker = (TrackerEntry) spinner.getItemAtPosition(0);
            postTrackerSelected(tracker);
        }

        adapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnLogEntryDeleted event) {
        if ( TinyTimeTracker.currentTracker != null && TinyTimeTracker.currentTracker.id == event.tracker_id ) {
            updateStatisticalValues(TinyTimeTracker.currentTracker);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnLogEntryChanged event) {
        if ( TinyTimeTracker.currentTracker != null && TinyTimeTracker.currentTracker.id == event.entry.tracker_id ) {
            updateStatisticalValues(TinyTimeTracker.currentTracker);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnDatabaseImported event) {
        Log.i(TAG, "OnDatabaseImported");
        loadTrackers();
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        adapter.notifyDataSetChanged();
    }

    private void updateStatisticalValues(TrackerEntry tracker){
        if (tracker == null) return;

        int reference_months = Settings.getReferenceMonths(getContext());
        UnixTimestamp reference = UnixTimestamp.nMonthsAgo(reference_months);
        LogDataSource datasource = new LogDataSource(getActivity());
        WorkTimeStatistics statistics = datasource.getTotalDurationPairSince(reference.getTimestamp(), tracker.id);

        long meanDurationMillis = statistics.getMeanDurationMillis();
        UnixTimestamp meanDuration = new UnixTimestamp(meanDurationMillis);
        String text = meanDuration.durationAsHours();
        textviewMeanDuration.setText("⌀ " + text);

        String totalDuration = new UnixTimestamp(statistics.getTotalDurationMills()).durationAsHours();
        textviewCummulatedTime.setText("Σ " + totalDuration);

        String since = getString(R.string.since);
        textviewWhen.setText(String.format("%s %s", since, reference.toDateString()));

        int workingHoursInSeconds = (int) (tracker.working_hours * 3600.f);
        if ( workingHoursInSeconds > 0 ) {
            Long overTimeMillis = statistics.getOvertimeMillis(tracker.working_hours);

            int timeBalanceInMinutes = datasource.getManualTimeBalanceInMinutes(tracker, reference.toCalendar().getTimeInMillis());
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
        prefEditor.apply();
    }
}
