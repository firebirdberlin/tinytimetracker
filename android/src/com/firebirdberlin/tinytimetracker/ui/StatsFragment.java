package com.firebirdberlin.tinytimetracker.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import com.firebirdberlin.tinytimetracker.CSVExport;
import com.firebirdberlin.tinytimetracker.LogDataSource;
import com.firebirdberlin.tinytimetracker.LogEntryListAdapter;
import com.firebirdberlin.tinytimetracker.R;
import com.firebirdberlin.tinytimetracker.TinyTimeTracker;
import com.firebirdberlin.tinytimetracker.Utility;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryAdded;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryChanged;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerSelected;
import com.firebirdberlin.tinytimetracker.events.OnWifiUpdateCompleted;
import com.firebirdberlin.tinytimetracker.models.LogEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class StatsFragment extends ListFragment implements View.OnClickListener {
    private static String TAG = TinyTimeTracker.TAG + ".StatsFragment";
    final List<LogEntry> log_entries = new ArrayList<>();
    LogEntryListAdapter log_entry_adapter = null;
    RadioGroup radio_group_aggregation = null;
    Button btnCSVExport = null;
    Context mContext = null;
    EventBus bus = EventBus.getDefault();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mContext = getActivity();
        View v = inflater.inflate(R.layout.stats_fragment, container, false);
        radio_group_aggregation = v.findViewById(R.id.radio_group_aggregation);
        btnCSVExport = v.findViewById(R.id.button_csv_export);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        registerForContextMenu(getListView());

        radio_group_aggregation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                refresh();
            }
        });

        btnCSVExport.setOnClickListener(this);

        radio_group_aggregation.check(R.id.radio_detail_this_month);
        log_entry_adapter = new LogEntryListAdapter(mContext, R.layout.list_2_columns, log_entries);
        setListAdapter(log_entry_adapter);
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

        refresh();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_log_entries, menu);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        int position = info.position;
        if ( position == log_entry_adapter.getCount() - 1 ) {
            MenuItem itemActionJoin = menu.findItem(R.id.action_join);
            itemActionJoin.setVisible(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        final LogEntry entry = log_entry_adapter.getItem(info.position);

        final LogDataSource datasource = new LogDataSource(mContext);
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
        case R.id.action_add:

            final DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    final Calendar now = Calendar.getInstance();
                    now.set(Calendar.YEAR, year);
                    now.set(Calendar.MONTH, monthOfYear);
                    now.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                            Log.d(TAG, "onTimeSet");
                            now.set(Calendar.HOUR_OF_DAY, hour);
                            now.set(Calendar.MINUTE, minute);
                            Toast.makeText(mContext, String.valueOf(now.getTimeInMillis()), Toast.LENGTH_LONG);
                            final LogDataSource datasource = new LogDataSource(mContext);
                            LogEntry e = new LogEntry(LogEntry.NOT_SAVED, entry.tracker_id, now.getTimeInMillis(), now.getTimeInMillis());
                            datasource.save(e);
                            datasource.close();

                        }
                    };
                    new TimePickerDialog(getContext(), onTimeSetListener, 9, 0, true).show();
                }

            };

            Calendar now = Calendar.getInstance();
            new DatePickerDialog(getContext(), onDateSetListener,
                                 now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                                 now.get(Calendar.DAY_OF_MONTH))
                    .show();

            return true;
        default:
            datasource.close();
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        if ( v.equals(btnCSVExport) ) {
            TinyTimeTracker mainActivity = (TinyTimeTracker) getActivity();
            if ( mainActivity.purchased_csv_data_export ) {
                exportCSV();
            } else {
                mainActivity.purchaseIntent(TinyTimeTracker.ITEM_CSV_DATA_EXPORT,
                        TinyTimeTracker.REQUEST_CODE_PURCHASE_CSV_DATA_EXPORT);
            }
        }
    }

    private void exportCSV() {
        if (TinyTimeTracker.currentTracker == null) return;
        if (log_entry_adapter.getCount() == 0) return;

        LogEntry firstEntry = log_entry_adapter.getItem(0);
        SimpleDateFormat df = new SimpleDateFormat("MMMM yyyy");
        String monthString = firstEntry.timestamp_start.toTimeString(df);

        String title = TinyTimeTracker.currentTracker.verbose_name + " " + monthString;
        String filename = title + ".csv";

        String data = "";
        SimpleDateFormat timeFormat = Utility.getTimeFormat(mContext);
        for (int i = log_entry_adapter.getCount() - 1 ; i  >= 0 ; i-- ) {
            LogEntry entry = log_entry_adapter.getItem(i);
            data += entry.toCSVString(timeFormat);
        }

        CSVExport csv = new CSVExport(mContext, filename);
        csv.save(data);
        csv.share(title);
    }

    public void refresh(LogEntry logEntry) {
        for (int i = 0; i < log_entry_adapter.getCount() ; i++ ) {
            LogEntry entry = log_entry_adapter.getItem(i);
            if ( entry.id == logEntry.id ) {
                entry.timestamp_start = logEntry.timestamp_start;
                entry.timestamp_end = logEntry.timestamp_end;
                log_entry_adapter.notifyDataSetChanged();
                return;
            }
        }
        // no match yet, insert entry at the top
        int checkedId = radio_group_aggregation.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_detail_this_month) {
            log_entry_adapter.insert(logEntry, 0);
            log_entry_adapter.notifyDataSetChanged();
        }
    }

    public void refresh() {
        if (radio_group_aggregation == null) {
            return;
        }

        try {
            unregisterForContextMenu(getListView());
        } catch (IllegalStateException e) {
            // pass
        }

        registerForContextMenu(getListView());
        refresh_detail();
    }

    public void refresh_detail() {
        if (mContext == null || log_entry_adapter == null) {
            return;
        }

        log_entry_adapter.clear();

        if (TinyTimeTracker.currentTracker != null) {
            int checkedId = radio_group_aggregation.getCheckedRadioButtonId();
            long start = 0L;
            long end = 0L;
            switch(checkedId) {
            case R.id.radio_detail_this_month:
            default:
                start = UnixTimestamp.startOfMonth().getTimestamp();
                end = System.currentTimeMillis();
                break;
            case R.id.radio_detail_last_month:
                start = UnixTimestamp.startOfLastMonth().getTimestamp();
                end = UnixTimestamp.startOfMonth().getTimestamp();
                break;
            }

            LogDataSource datasource = new LogDataSource(mContext);
            List<LogEntry> values = datasource.getAllEntries(TinyTimeTracker.currentTracker.id,
                                                             start, end);
            datasource.close();
            log_entry_adapter.addAll(values);
        }

        log_entry_adapter.notifyDataSetChanged();
    }


    @Subscribe
    public void onEvent(OnWifiUpdateCompleted event) {
        if ( TinyTimeTracker.currentTracker == null ) return;
        if (event.success &&
                TinyTimeTracker.currentTracker.equals(event.tracker)
                && event.logentry != null ) {
            refresh(event.logentry);
        }
    }

    @Subscribe
    public void onEvent(OnTrackerSelected event) {
        Log.i(TAG, "OnTrackerSelected");
        refresh();
    }

    @Subscribe
    public void onEvent(OnTrackerDeleted event) {
        refresh();
    }

    @Subscribe
    public void onEvent(OnLogEntryChanged event) {
        if ( TinyTimeTracker.currentTracker != null &&
                TinyTimeTracker.currentTracker.id == event.entry.tracker_id ) {
            refresh(event.entry);
        }
    }

    @Subscribe
    public void onEvent(OnLogEntryDeleted event) {
        if ( TinyTimeTracker.currentTracker != null ) {
            refresh();
        }
    }

    @Subscribe
    public void onEvent(OnLogEntryAdded event) {
        refresh();
    }
}
