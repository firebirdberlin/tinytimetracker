package com.firebirdberlin.tinytimetracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.fragment.app.DialogFragment;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class MonthYearPickerDialog extends DialogFragment {

    private static final int MIN_YEAR = 2010;
    private static final int MAX_YEAR = 2099;
    private DatePickerDialog.OnDateSetListener listener;
    private int currentMonth = 0;
    private int currentYear = MIN_YEAR;

    public void setListener(DatePickerDialog.OnDateSetListener listener) {
        this.listener = listener;
    }

    public void setCurrentMonth(int currentMonth) {
        this.currentMonth = currentMonth;
    }

    public void setCurrentYear(int currentYear) {
        this.currentYear = currentYear;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        Calendar cal = Calendar.getInstance();

        View dialog = inflater.inflate(R.layout.month_year_picker_dialog, null);
        final NumberPicker monthPicker = dialog.findViewById(R.id.picker_month);
        final NumberPicker yearPicker = dialog.findViewById(R.id.picker_year);

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setValue(currentMonth);
        monthPicker.setDisplayedValues(getMonthStrings());


        int year = cal.get(Calendar.YEAR);
        yearPicker.setMinValue(MIN_YEAR);
        yearPicker.setMaxValue(year);
        yearPicker.setValue(currentYear);

        monthPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                int year = yearPicker.getValue();
                if (oldVal == 11 && newVal == 0) {

                    if (year < yearPicker.getMaxValue()) {
                        yearPicker.setValue(year + 1);
                    }
                } else
                if (oldVal == 0 && newVal == 11) {
                    if (year > yearPicker.getMinValue()) {
                        yearPicker.setValue(year - 1);
                    }
                }

            }
        });

        builder.setView(dialog)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int year = yearPicker.getValue();
                        int month = monthPicker.getValue();

                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, month);
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.MILLISECOND, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.HOUR_OF_DAY, 0);

                        Calendar now = Calendar.getInstance();
                        now.set(Calendar.DAY_OF_MONTH, 1);
                        now.set(Calendar.MILLISECOND, 0);
                        now.set(Calendar.SECOND, 0);
                        now.set(Calendar.MINUTE, 0);
                        now.set(Calendar.HOUR_OF_DAY, 0);

                        if (cal.after(now)) {
                            year = now.get(Calendar.YEAR);
                            month = now.get(Calendar.MONTH);
                        }

                        listener.onDateSet(null, year, month, 1);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MonthYearPickerDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    private static String[] getMonthStrings() {
        return getMonthStringsForLocale(Locale.getDefault());
    }

    private static String[] getMonthStringsForLocale(Locale locale) {
        DateFormatSymbols symbols = new DateFormatSymbols(locale);
        return symbols.getMonths();
    }
}
