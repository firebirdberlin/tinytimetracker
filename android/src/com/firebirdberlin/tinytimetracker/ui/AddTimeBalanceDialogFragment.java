package com.firebirdberlin.tinytimetracker.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

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

                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int minutes = 0;
                        String hourText = hourTextEdit.getText().toString();
                        if (!hourText.isEmpty()) {
                            minutes += Integer.parseInt(hourText) * 60;
                        }
                        String minuteText = minuteTextEdit.getText().toString();
                        if (!minuteText.isEmpty()) {
                            minutes += Integer.parseInt(minuteText);
                        }
                        if (spinner.getSelectedItemPosition() == 0) {
                            minutes *= -1;
                        }

                        mListener.onTimeBalanceEntryAdded(minutes);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddTimeBalanceDialogFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

    public interface AddTimeBalanceDialogListener {
        void onTimeBalanceEntryAdded(int minutes);
    }
}