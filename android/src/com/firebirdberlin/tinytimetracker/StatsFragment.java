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
    ArrayAdapter<String> two_column_adapter = null;
    RadioGroup radio_group_aggregation = null;
    Context mContext = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        View v = inflater.inflate(R.layout.stats_fragment, container, false);

        two_column_adapter = new ArrayAdapter<String>(mContext, R.layout.list_2_columns, R.id.text1, svalues1) {
            public View getView( int position, View convertView, ViewGroup parent ) {
                super.getView(position, convertView, parent);
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View v = inflater.inflate(R.layout.list_2_columns, parent, false);
                TextView text1 = ( TextView ) v.findViewById ( R.id.text1 );
                TextView text2 = ( TextView ) v.findViewById ( R.id.text2 );
                if (position < svalues1.size()){
                    text1.setText(svalues1.get(position));
                    text2.setText(svalues2.get(position));
                }

                return v;

            }

        };
        setListAdapter(two_column_adapter);
        refresh();

        radio_group_aggregation = (RadioGroup) v.findViewById(R.id.radio_group_aggregation);
        radio_group_aggregation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.radio_aggregation_detail:
                        refresh();
                        break;
                    case R.id.radio_aggregation_day:
                        refresh(LogDataSource.AGGRETATION_DAY);
                        break;
                    case R.id.radio_aggregation_week:
                        refresh(LogDataSource.AGGRETATION_WEEK);
                        break;
                    case R.id.radio_aggregation_year:
                        refresh(LogDataSource.AGGRETATION_YEAR);
                        break;
                    default:
                        refresh();
                        break;
                }
            }
        });
        return v;
    }

    public void refresh(int aggregation_type) {
        if (mContext == null) {
            return;
        }
        if (two_column_adapter == null) {
            return;
        }

        two_column_adapter.clear();
        svalues1.clear();
        svalues2.clear();
        LogDataSource datasource = new LogDataSource(mContext);
        datasource.open();
        String ssid = Settings.getTrackedSSID(mContext);
        long tracker_id = datasource.getTrackerID(ssid, "WLAN");
        List< Pair<Long, Long> > values = datasource.getTotalDurationAggregated(tracker_id, aggregation_type);
        for (Pair<Long, Long> e : values) {
            UnixTimestamp timestamp = new UnixTimestamp(e.first.longValue());
            UnixTimestamp duration = new UnixTimestamp(e.second.longValue());
            String hours = duration.durationAsHours();
            svalues2.add(hours);
            switch (aggregation_type){
                case LogDataSource.AGGRETATION_DAY:
                    svalues1.add(timestamp.toDateString()); break;
                case LogDataSource.AGGRETATION_WEEK:
                    svalues1.add(timestamp.toWeekString()); break;
                case LogDataSource.AGGRETATION_YEAR:
                    svalues1.add(timestamp.toYearString()); break;
                default:
                    svalues1.add(timestamp.toDateString()); break;
            }
        }
        datasource.close();
        two_column_adapter.notifyDataSetChanged();
    }


    public void refresh() {
        if (mContext == null) {
            return;
        }
        if (two_column_adapter == null) {
            return;
        }

        two_column_adapter.clear();
        svalues1.clear();
        svalues2.clear();
        LogDataSource datasource = new LogDataSource(mContext);
        datasource.open();
        String ssid = Settings.getTrackedSSID(mContext);
        List<LogEntry> values = datasource.getAllEntries(ssid);
        String lastDate = "";
        for (LogEntry e : values) {
            String curDate = e.startAsDateString();
            if (! curDate.equals(lastDate)) {
                lastDate = curDate;
                svalues1.add(curDate);
            } else {
                svalues1.add("");
            }
            svalues2.add(e.toString());
        }
        datasource.close();
        two_column_adapter.notifyDataSetChanged();
    }
}
