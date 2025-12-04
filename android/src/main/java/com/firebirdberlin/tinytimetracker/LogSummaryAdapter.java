package com.firebirdberlin.tinytimetracker;

import java.util.List;

import android.content.Context;
import android.widget.TableLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebirdberlin.tinytimetracker.models.LogSummary;
import com.firebirdberlin.tinytimetracker.models.LogDailySummary;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;
import com.firebirdberlin.tinytimetracker.ui.LogDailySummaryView;


public class LogSummaryAdapter extends RecyclerView.Adapter<LogSummaryAdapter.LogSummaryViewHolder> {
    private List<LogSummary> weeklyLogsList;

    public LogSummaryAdapter(List<LogSummary> weeklyLogsList) {
        this.weeklyLogsList = weeklyLogsList;
    }

    public void add(int index, LogSummary summary) {
        this.weeklyLogsList.add(index, summary);
    }

    public int replace(LogSummary summary) {

        LogDailySummary first = summary.dailySummaries.get(0);
        UnixTimestamp firstTimestamp = new UnixTimestamp(first.timestamp);
        String newWeekString = firstTimestamp.toWeekStringVerbose();

        int index = 0;
        for (LogSummary s: weeklyLogsList) {
            first = s.dailySummaries.get(0);
            firstTimestamp = new UnixTimestamp(first.timestamp);
            String weekString = firstTimestamp.toWeekStringVerbose();
            if ( weekString.equals(newWeekString) ) {
                weeklyLogsList.set(index, summary);
                return index;
            }
            index++;
        }

        return -1;
    }

    @Override
    public int getItemCount() {
        return weeklyLogsList.size();
    }

    @Override
    public void onBindViewHolder(LogSummaryViewHolder viewHolder, int i) {
        LogSummary ci = weeklyLogsList.get(i);
        viewHolder.clearRows();
        boolean showSaldo = ci.tracker.working_hours > 0.f;
        if (ci.dailySummaries.size() > 0 ) {
            LogDailySummary first = ci.dailySummaries.get(0);
            UnixTimestamp firstTimestamp = new UnixTimestamp(first.timestamp);
            String week = viewHolder.context.getResources().getString(R.string.aggregation_week);
            viewHolder.tvTitle.setText(week + " " + firstTimestamp.toWeekStringVerbose());
            // reverse order, because the list of durations is retrieved in descending order
            LogDailySummary summed = new LogDailySummary();
            for (LogDailySummary s : ci.dailySummaries ) {
                viewHolder.addRow(s, showSaldo);
                summed.duration += s.duration;
                summed.saldo += s.saldo;
            }
            viewHolder.addSummary(summed, showSaldo);
        }
    }

    @Override
    public LogSummaryViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
            from(viewGroup.getContext()).
            inflate(R.layout.cardview, viewGroup, false);

        return new LogSummaryViewHolder(itemView);
    }

    public static class LogSummaryViewHolder extends RecyclerView.ViewHolder {
        protected Context context;
        protected TableLayout layout;
        protected TextView tvTitle;

        public LogSummaryViewHolder(View v) {
            super(v);
            context = v.getContext();
            tvTitle = (TextView) v.findViewById(R.id.title);
            layout = (TableLayout) v.findViewById(R.id.layout);
        }

        protected void clearRows() {
            layout.removeAllViews();
        }

        protected void addRow(LogDailySummary dailySummary, boolean showSaldo) {
            LogDailySummaryView view = new LogDailySummaryView(context, layout, showSaldo);
            view.set(dailySummary);
        }

        protected void addSummary(LogDailySummary summary, boolean showSaldo) {
            LogDailySummaryView view = new LogDailySummaryView(context, layout, showSaldo);
            view.set(summary);
            view.emphasize();
        }
    }
}
