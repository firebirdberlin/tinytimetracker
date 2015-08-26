package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.List;

public class StatsFragment extends ListFragment {
    final List<String> svalues1 = new ArrayList<String>();
    final List<String> svalues2 = new ArrayList<String>();
    TwoColumnListAdapter two_column_adapter = null;
    RadioGroup radio_group_aggregation = null;
    Context mContext = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();

        View v = inflater.inflate(R.layout.stats_fragment, container, false);
        radio_group_aggregation = (RadioGroup) v.findViewById(R.id.radio_group_aggregation);
        radio_group_aggregation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                refresh(checkedId);
            }
        });

        two_column_adapter = new TwoColumnListAdapter(mContext, R.layout.list_2_columns, svalues1, svalues2);
        setListAdapter(two_column_adapter);
        radio_group_aggregation.check(R.id.radio_aggregation_detail);
        refresh_detail();

        return v;
    }

    public void refresh_aggregated(int aggregation_type) {
        if (mContext == null) {
            return;
        }
        if (two_column_adapter == null) {
            return;
        }

        two_column_adapter.clear();
        LogDataSource datasource = new LogDataSource(mContext);
        datasource.open();
        String ssid = Settings.getTrackedSSID(mContext);
        long tracker_id = datasource.getTrackerID(ssid, "WLAN");
        List< Pair<Long, Long> > values = datasource.getTotalDurationAggregated(tracker_id, aggregation_type);
        for (Pair<Long, Long> e : values) {
            UnixTimestamp timestamp = new UnixTimestamp(e.first.longValue());
            UnixTimestamp duration = new UnixTimestamp(e.second.longValue());
            String hours = duration.durationAsHours();
            two_column_adapter.addRight(hours);
            switch (aggregation_type){
                case LogDataSource.AGGRETATION_DAY:
                default:
                    two_column_adapter.add(timestamp.toDateString()); break;
                case LogDataSource.AGGRETATION_WEEK:
                    two_column_adapter.add(timestamp.toWeekString()); break;
                case LogDataSource.AGGRETATION_YEAR:
                    two_column_adapter.add(timestamp.toYearString()); break;
            }
        }
        datasource.close();
        two_column_adapter.notifyDataSetChanged();
    }

    public void refresh_detail() {
        if (mContext == null) {
            return;
        }
        if (two_column_adapter == null) {
            return;
        }

        two_column_adapter.clear();
        LogDataSource datasource = new LogDataSource(mContext);
        datasource.open();
        String ssid = Settings.getTrackedSSID(mContext);
        List<LogEntry> values = datasource.getAllEntries(ssid);
        String lastDate = "";
        for (LogEntry e : values) {
            String curDate = e.startAsDateString();
            if (! curDate.equals(lastDate)) {
                lastDate = curDate;
                two_column_adapter.add(curDate);
            } else {
                two_column_adapter.add("");
            }
            two_column_adapter.addRight(e.toString());
        }
        datasource.close();
        two_column_adapter.notifyDataSetChanged();
    }

    public void refresh(int checkedId) {
        switch(checkedId){
            case R.id.radio_aggregation_detail:
            default:
                refresh_detail(); break;
            case R.id.radio_aggregation_day:
                refresh_aggregated(LogDataSource.AGGRETATION_DAY); break;
            case R.id.radio_aggregation_week:
                refresh_aggregated(LogDataSource.AGGRETATION_WEEK); break;
            case R.id.radio_aggregation_year:
                refresh_aggregated(LogDataSource.AGGRETATION_YEAR); break;
        }
    }

    public void refresh() {
        if (radio_group_aggregation == null) {
            return;
        }
        int checkedId = radio_group_aggregation.getCheckedRadioButtonId();
        refresh(checkedId);
    }
}
