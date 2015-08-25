package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;
import java.util.List;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;

public class TwoColumnListAdapter extends ArrayAdapter<String> {
    private Context mContext = null;
    private List<String> values1 = null;
    private List<String> values2 = null;

    public TwoColumnListAdapter(Context context, int viewid, List<String> values1, List<String> values2) {
        super(context, viewid, R.id.text1, values1);
        mContext = context;
        this.values1 = values1;
        this.values2 = values2;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        super.getView(position, convertView, parent);
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View v = inflater.inflate(R.layout.list_2_columns, parent, false);
        TextView text1 = ( TextView ) v.findViewById ( R.id.text1 );
        TextView text2 = ( TextView ) v.findViewById ( R.id.text2 );
        if (position < values1.size()){
            text1.setText(values1.get(position));
            text2.setText(values2.get(position));
        }

        return v;

    }

}
