package com.firebirdberlin.tinytimetracker.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.firebirdberlin.tinytimetracker.R;
import com.firebirdberlin.tinytimetracker.models.LogDailySummary;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;

public class LogDailySummaryView extends View {
    private Context mContext;
    private TextView tvDate;
    private TextView tvDuration;
    private TextView tvSaldo;
    private TableLayout tlRoot;

    public LogDailySummaryView(Context context, ViewGroup root, boolean showSaldo) {
        super(context);
        mContext = context;
        tlRoot = (TableLayout) root;

        RelativeLayout relativeLayout = new RelativeLayout(context);

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        tvDate = new TextView(context);
        tvDuration = new TextView(context);
        tvSaldo = new TextView(context);
        tvDate.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
        tvDuration.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
        tvSaldo.setTextAppearance(mContext, android.R.style.TextAppearance_Small);

        TableRow tr = makeRow();
        tvDate.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tvDuration.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.RIGHT;
        tvSaldo.setLayoutParams(params);

        tr.addView(tvDate);
        tr.addView(tvDuration);
        if (showSaldo) tr.addView(tvSaldo);

        tlRoot.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

        if ( tlRoot.getChildCount() == 1 ) {
            addDividerAbove();
        }
    }

    private TableRow makeRow() {
        TableRow tr = new TableRow(mContext);
        tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                                                     TableRow.LayoutParams.WRAP_CONTENT));
        return tr;
    }

    public void set(LogDailySummary summary) {
        if ( summary.timestamp > 0L ) {
            UnixTimestamp timestamp = new UnixTimestamp(summary.timestamp);
            tvDate.setText(timestamp.toLongerDateString());
        } else {
            tvDate.setText("Summary");
        }


        UnixTimestamp duration = new UnixTimestamp(summary.duration);
        String hours = duration.durationAsHours();
        tvDuration.setText(duration.durationAsHours());

        UnixTimestamp saldo = new UnixTimestamp(summary.saldo);
        String sign = (summary.saldo < 0 ) ? "- ": "";
        tvSaldo.setText(sign + saldo.durationAsHours());
    }

    public void emphasize() {
        tvDate.setText("Total");
        tvDate.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
        tvDuration.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
        tvSaldo.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);

        addDividerAbove();
    }

    private void addDividerAbove() {
        TableRow tr = makeRow();
        View divider = LayoutInflater.from(mContext)
                                     .inflate(R.layout.divider, tr, false);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                5, 1.f );
        params.span = 3;
        divider.setLayoutParams(params);
        tr.addView(divider);
        int childCount = tlRoot.getChildCount();
        tlRoot.addView(tr, childCount - 1, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

    }
}
