package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class LogEntryListAdapter extends ArrayAdapter<LogEntry> {
    private Context mContext = null;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");


    public LogEntryListAdapter(Context context, int viewid, List<LogEntry> values1) {
        super(context, viewid, R.id.text1, values1);
        mContext = context;

        if ( DateFormat.is24HourFormat(context) ) {
            timeFormat = new SimpleDateFormat("HH:mm");
        }
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        super.getView(position, convertView, parent);
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View v = inflater.inflate(R.layout.list_2_columns, parent, false);
        TextView text1 = ( TextView ) v.findViewById ( R.id.text1 );
        TextView text2 = ( TextView ) v.findViewById ( R.id.text2 );

        if (position < getCount()) {
            String currDate = getItem(position).startAsDateString();
            text1.setText(currDate);
            if ( position > 0 ) {
                String prevDate = getItem(position - 1).startAsDateString();
                if ( prevDate.equals(currDate) ) text1.setText("");
            }

            text2.setText(getItem(position).toString(timeFormat));
        }

        return v;
    }
}
