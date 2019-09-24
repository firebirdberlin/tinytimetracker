package com.firebirdberlin.tinytimetracker.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.firebirdberlin.tinytimetracker.R;

public class AddTimeBalanceDialogFragment extends DialogFragment {

    // Use this instance of the interface to deliver action events
    AddTimeBalanceDialogListener mListener;
    Spinner spinner;
    EditText hourTextEdit;
    EditText minuteTextEdit;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (AddTimeBalanceDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AddTimeBalanceDialogListener");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (AddTimeBalanceDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement AddTimeBalanceDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.add_time_balance_dialog, null);
        spinner = view.findViewById(R.id.time_balance_type);
        hourTextEdit = view.findViewById(R.id.hourText);
        minuteTextEdit = view.findViewById(R.id.minuteText);

        builder.setTitle(R.string.dialog_title_time_balancing_entry)
                .setIcon(R.drawable.ic_scales)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(android.R.string.cancel, null);

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        int minutes = 0;
                        boolean minuteValid = true;
                        boolean hourValid = true;

                        String hourText = hourTextEdit.getText().toString();
                        if ( !hourText.isEmpty() ) {
                            try {
                                minutes += Integer.parseInt(hourText) * 60;
                            } catch (NumberFormatException e) {
                                hourValid = false;
                            }
                        }

                        String minuteText = minuteTextEdit.getText().toString();
                        if ( !minuteText.isEmpty() ) {
                            try {
                                int minute = Integer.parseInt(minuteText);

                                if ( minute >= 60 ) {
                                    minuteValid = false;
                                } else {
                                    minutes += minute;
                                }
                            } catch (NumberFormatException e) {
                                minuteValid = false;
                            }
                        }

                        if ( spinner.getSelectedItemPosition() == 0 ) {
                            minutes *= -1;
                        }

                        if ( !minuteValid ) {
                            minuteTextEdit.setTextColor(Color.RED);
                        }

                        if ( !hourValid ) {
                            hourTextEdit.setTextColor(Color.RED);
                        }

                        if ( minuteValid && hourValid) {
                            mListener.onTimeBalanceEntryAdded(minutes);
                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        return dialog;
    }

    public interface AddTimeBalanceDialogListener {
        void onTimeBalanceEntryAdded(int minutes);
    }
}