package com.firebirdberlin.tinytimetracker.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.RadioGroup;
import de.greenrobot.event.EventBus;

import com.firebirdberlin.tinytimetracker.R;
import com.firebirdberlin.tinytimetracker.LogDataSource;
import com.firebirdberlin.tinytimetracker.LogEntryListAdapter;
import com.firebirdberlin.tinytimetracker.TinyTimeTracker;
import com.firebirdberlin.tinytimetracker.TwoColumnListAdapter;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryChanged;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerSelected;
import com.firebirdberlin.tinytimetracker.events.OnWifiUpdateCompleted;
import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;


public class StatsFragment extends ListFragment {
    private static String TAG = TinyTimeTracker.TAG + ".StatsFragment";
    final List<LogEntry> log_entries = new ArrayList<LogEntry>();
    final List<String> svalues1 = new ArrayList<String>();
    final List<String> svalues2 = new ArrayList<String>();
    TwoColumnListAdapter two_column_adapter = null;
    LogEntryListAdapter log_entry_adapter = null;
    RadioGroup radio_group_aggregation = null;
    Context mContext = null;
    TrackerEntry currentTracker = null;
    EventBus bus = EventBus.getDefault();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mContext = (Context) getActivity();
        View v = inflater.inflate(R.layout.stats_fragment, container, false);
        radio_group_aggregation = (RadioGroup) v.findViewById(R.id.radio_group_aggregation);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        registerForContextMenu(getListView());

        radio_group_aggregation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                refresh(checkedId);
            }
        });

        two_column_adapter = new TwoColumnListAdapter(mContext, R.layout.list_2_columns, svalues1,
                                                      svalues2);
        log_entry_adapter = new LogEntryListAdapter(mContext, R.layout.list_2_columns, log_entries);

        radio_group_aggregation.check(R.id.radio_aggregation_detail);
        refresh_detail();
    }

    @Override
    public void onPause() {
        super.onPause();
        bus.unregister(this);

    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "onResume()");
        bus.register(this);

        OnTrackerSelected event = bus.getStickyEvent(OnTrackerSelected.class);
        if ( event != null ) {
            this.currentTracker = event.newTracker;
        }
        refresh();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_log_entries, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        LogEntry entry = log_entry_adapter.getItem(info.position);

        LogDataSource datasource = new LogDataSource(mContext);
        switch (item.getItemId()) {
        case R.id.action_delete:
            datasource.delete(entry);
            log_entry_adapter.remove(entry);
            datasource.close();
            return true;
        case R.id.action_edit:
            EditLogEntryDialogFragment dialogFragment = new EditLogEntryDialogFragment();
            dialogFragment.entry = entry;
            dialogFragment.mContext = mContext;
            if ( info.position > 0 ) {
                dialogFragment.prevEntry = log_entry_adapter.getItem(info.position - 1);
            }
            dialogFragment.show(getFragmentManager(), "edit_log_entry_dialog");
            datasource.close();
            return true;
        case R.id.action_join:
            if ( info.position < log_entry_adapter.getCount() ) {
                long new_end = entry.getTimestampEnd();
                LogEntry nextEntry = log_entry_adapter.getItem(info.position + 1);
                nextEntry.setTimestampEnd(new_end);
                datasource.save(nextEntry);
                datasource.delete(entry);
                log_entry_adapter.remove(entry);
            }
            datasource.close();
            return true;
        default:
            datasource.close();
            return super.onContextItemSelected(item);
        }
    }

    public void refresh_aggregated(int aggregation_type) {
        if (mContext == null) {
            return;
        }

        if (two_column_adapter == null) {
            return;
        }

        two_column_adapter.clear();
        setListAdapter(two_column_adapter);

        if (currentTracker != null) {
            LogDataSource datasource = new LogDataSource(mContext);
            List< Pair<Long, Long> > values = datasource.getTotalDurationAggregated(currentTracker.id, aggregation_type);
            datasource.close();

            for (Pair<Long, Long> e : values) {
                UnixTimestamp timestamp = new UnixTimestamp(e.first.longValue());
                UnixTimestamp duration = new UnixTimestamp(e.second.longValue());
                String hours = duration.durationAsHours();
                two_column_adapter.addRight(hours);

                switch (aggregation_type) {
                case LogDataSource.AGGRETATION_DAY:
                default:
                    two_column_adapter.add(timestamp.toDateString());
                    break;
                case LogDataSource.AGGRETATION_WEEK:
                    two_column_adapter.add(timestamp.toWeekString());
                    break;
                case LogDataSource.AGGRETATION_YEAR:
                    two_column_adapter.add(timestamp.toYearString());
                    break;
                }
            }
        }

        two_column_adapter.notifyDataSetChanged();
    }

    public void refresh_detail() {
        if (mContext == null || log_entry_adapter == null) {
            return;
        }

        log_entry_adapter.clear();
        setListAdapter(log_entry_adapter);

        if (currentTracker != null) {
            LogDataSource datasource = new LogDataSource(mContext);
            long startOfMonth = UnixTimestamp.startOfLastMonth().getTimestamp();
            List<LogEntry> values = datasource.getAllEntries(currentTracker.id, startOfMonth);
            datasource.close();
            log_entry_adapter.addAll(values);
        }

        log_entry_adapter.notifyDataSetChanged();
    }

    public void refresh(int checkedId) {

        try {
            unregisterForContextMenu(getListView());
        } catch (IllegalStateException e) {
            // pass
        }

        switch(checkedId) {
        case R.id.radio_aggregation_detail:
        default:
            registerForContextMenu(getListView());
            refresh_detail();
            break;
        case R.id.radio_aggregation_day:
            refresh_aggregated(LogDataSource.AGGRETATION_DAY);
            break;
        case R.id.radio_aggregation_week:
            refresh_aggregated(LogDataSource.AGGRETATION_WEEK);
            break;
        case R.id.radio_aggregation_year:
            refresh_aggregated(LogDataSource.AGGRETATION_YEAR);
            break;
        }
    }

    public void refresh() {
        if (radio_group_aggregation == null) {
            return;
        }

        int checkedId = radio_group_aggregation.getCheckedRadioButtonId();
        refresh(checkedId);
    }

    public void onEvent(OnWifiUpdateCompleted event) {
        if ( currentTracker == null ) return;
        if (event.success && currentTracker.equals(event.tracker)) {
            refresh();
        }
    }

    public void onEvent(OnTrackerSelected event) {
        Log.i(TAG, "OnTrackerSelected");
        this.currentTracker = event.newTracker;
        refresh();
    }

    public void onEvent(OnTrackerDeleted event) {
        this.currentTracker = null;
        refresh();
    }

    public void onEvent(OnLogEntryChanged event) {
        if ( currentTracker != null && currentTracker.id == event.entry.tracker_id ) {
            refresh();
        }
    }

    public void onEvent(OnLogEntryDeleted event) {
        if ( currentTracker != null ) {
            refresh();
        }
    }
}
