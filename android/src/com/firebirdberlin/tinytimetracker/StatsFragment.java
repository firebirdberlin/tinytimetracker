package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.TextView;
import de.greenrobot.event.EventBus;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.List;

public class StatsFragment extends ListFragment {
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
        bus.register(this);
        View v = inflater.inflate(R.layout.stats_fragment, container, false);
        radio_group_aggregation = (RadioGroup) v.findViewById(R.id.radio_group_aggregation);
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
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        registerForContextMenu(getListView());
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

        switch (item.getItemId()) {
        case R.id.action_delete:
            LogEntry entry = log_entry_adapter.getItem(info.position);
            LogDataSource datasource = new LogDataSource(mContext);
            datasource.deleteLogEntry(entry.id);
            log_entry_adapter.remove(entry);
            return true;
        default:
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
        LogDataSource datasource = new LogDataSource(mContext);

        if (currentTracker != null) {
            List< Pair<Long, Long> > values = datasource.getTotalDurationAggregated(currentTracker.id, aggregation_type);

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

        LogDataSource datasource = new LogDataSource(mContext);
        if (currentTracker != null) {
            List<LogEntry> values = datasource.getAllEntries(currentTracker.id);
            log_entry_adapter.addAll(values);
            //for (LogEntry e : values) {
                //log_entry_adapter.add(e);
            //}
        }

        log_entry_adapter.notifyDataSetChanged();
    }

    public void refresh(int checkedId) {
        unregisterForContextMenu(getListView());

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
        if (event.success && currentTracker.equals(event.tracker)) {
            int checkedId = radio_group_aggregation.getCheckedRadioButtonId();
            refresh(checkedId);
        }
    }

    public void onEvent(OnTrackerSelected event) {
        this.currentTracker = event.newTracker;

        if (radio_group_aggregation == null) {
            return;
        }

        int checkedId = radio_group_aggregation.getCheckedRadioButtonId();
        refresh(checkedId);
    }

    public void onEvent(OnTrackerDeleted event) {
        this.currentTracker = null;

        if (radio_group_aggregation == null) {
            return;
        }

        int checkedId = radio_group_aggregation.getCheckedRadioButtonId();
        refresh(checkedId);
    }
}
