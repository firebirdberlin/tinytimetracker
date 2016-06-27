package com.firebirdberlin.tinytimetracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import java.util.Calendar;

public class EditLogEntryDialogFragment extends DialogFragment {

    public Context mContext = null;
    public LogEntry prevEntry = null;
    public LogEntry entry = null;
    private Calendar cal_start = null;
    private Calendar cal_end = null;
    private NumberPicker pickerHourStart = null;
    private NumberPicker pickerHourEnd = null;
    private NumberPicker pickerMinuteStart = null;
    private NumberPicker pickerMinuteEnd = null;
    private TextView textViewStartDate = null;
    private TextView textViewEndDate = null;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setRetainInstance(true);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_edit_log_entry, null);
        LinearLayout main_layout = ( LinearLayout ) v.findViewById ( R.id.main_layout );
        pickerHourStart = ( NumberPicker ) v.findViewById ( R.id.hour_start );
        pickerHourEnd = ( NumberPicker ) v.findViewById ( R.id.hour_end );
        pickerMinuteStart = ( NumberPicker ) v.findViewById ( R.id.minutes_start );
        pickerMinuteEnd = ( NumberPicker ) v.findViewById ( R.id.minutes_end );
        textViewStartDate = ( TextView ) v.findViewById ( R.id.date_start );
        textViewEndDate = ( TextView ) v.findViewById ( R.id.date_end );

        pickerHourStart.setMaxValue(23);
        pickerHourEnd.setMaxValue(23);
        pickerMinuteStart.setMaxValue(59);
        pickerMinuteEnd.setMaxValue(59);

        UnixTimestamp start = entry.timestamp_start;
        UnixTimestamp end = entry.timestamp_end;

        cal_start = entry.timestamp_start.toCalendar();
        cal_end = entry.timestamp_end.toCalendar();

        textViewStartDate.setText(start.toDateString());
        textViewEndDate.setText(end.toDateString());

        pickerHourStart.setValue(start.getHourOfDay());
        pickerMinuteStart.setValue(start.getMinute());

        pickerHourEnd.setValue(end.getHourOfDay());
        pickerMinuteEnd.setValue(end.getMinute());

        pickerMinuteStart.setOnValueChangedListener(minutesStartValueChangeListener);
        pickerMinuteEnd.setOnValueChangedListener(minutesEndValueChangeListener);

        pickerHourStart.setOnValueChangedListener(hourStartValueChangeListener);
        pickerHourEnd.setOnValueChangedListener(hourEndValueChangeListener);

        int orientation = getActivity().getResources().getConfiguration().orientation;
        if ( orientation == Configuration.ORIENTATION_LANDSCAPE ) {
            main_layout.setOrientation(LinearLayout.HORIZONTAL);
        }

        builder.setView(v)
               .setPositiveButton(android.R.string.ok, positiveButtonClickListener)
               .setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
      if (getDialog() != null && getRetainInstance())
        getDialog().setDismissMessage(null);
      super.onDestroyView();
    }

    OnValueChangeListener minutesStartValueChangeListener =
        new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                auto_roll_hour_picker(pickerHourStart, picker, oldVal, newVal);
                updateStartTimeFromPickers();
            }
        };

    OnValueChangeListener minutesEndValueChangeListener =
        new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                auto_roll_hour_picker(pickerHourEnd, picker, oldVal, newVal);
                updateEndTimeFromPickers();
            }
        };

    OnValueChangeListener hourStartValueChangeListener =
        new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                int incr = getIncrement(picker, oldVal, newVal);
                if ( incr != 0 ) {
                    cal_start.add(Calendar.DAY_OF_YEAR, incr);
                    UnixTimestamp ts = new UnixTimestamp(cal_start.getTimeInMillis());
                    textViewStartDate.setText(ts.toDateString());
                }

                updateStartTimeFromPickers();
            }
        };

    OnValueChangeListener hourEndValueChangeListener =
        new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                int incr = getIncrement(picker, oldVal, newVal);
                if ( incr != 0 ) {
                    cal_end.add(Calendar.DAY_OF_YEAR, incr);
                    UnixTimestamp ts = new UnixTimestamp(cal_end.getTimeInMillis());
                    textViewEndDate.setText(ts.toDateString());
                }

                updateEndTimeFromPickers();
            }
        };

    private void auto_roll_hour_picker(NumberPicker hourPicker, NumberPicker picker, int oldVal,
                                       int newVal) {
        int incr = getIncrement(picker, oldVal, newVal);
        if ( incr != 0 ) {
            hourPicker.setValue( hourPicker.getValue() + incr % hourPicker.getMaxValue() );
            hourPicker.invalidate();
        }
    }

    private int getIncrement(NumberPicker picker, int oldVal, int newVal) {
        if ( newVal == picker.getMinValue() && oldVal == picker.getMaxValue()) {
            return 1;
        } else if ( oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
            return -1;
        }
        return 0;
    }

    private void updateStartTimeFromPickers() {
        cal_start.set(Calendar.MINUTE, pickerMinuteStart.getValue());
        cal_start.set(Calendar.HOUR_OF_DAY, pickerHourStart.getValue());
    }

    private void updateEndTimeFromPickers() {
        cal_end.set(Calendar.MINUTE, pickerMinuteEnd.getValue());
        cal_end.set(Calendar.HOUR_OF_DAY, pickerHourEnd.getValue());
    }

    DialogInterface.OnClickListener positiveButtonClickListener =
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                entry.setTimestampStart(cal_start.getTimeInMillis());
                entry.setTimestampEnd(cal_end.getTimeInMillis());
                LogDataSource datasource = new LogDataSource(mContext);
                datasource.save( entry );
            }
        };
}
