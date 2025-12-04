package com.firebirdberlin.tinytimetracker.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.firebirdberlin.tinytimetracker.R;

public class SeekBarDialogPreference extends DialogPreference {
    private int mCurrentValue;
    private final int mMinValue;
    private final int mMaxValue;
    private final String mText;
    private final int mDialogLayoutResId;


    public SeekBarDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarDialogPreference, 0, 0);

        try {
            mMinValue = a.getInt(R.styleable.SeekBarDialogPreference_min, 0);
            mMaxValue = a.getInt(R.styleable.SeekBarDialogPreference_android_max, 100);
            mText = a.getString(R.styleable.SeekBarDialogPreference_android_text);
            mDialogLayoutResId = a.getResourceId(R.styleable.SeekBarDialogPreference_dialogLayout, R.layout.dialog_seekbar_preference);

        } finally {
            a.recycle();
        }

        setDialogLayoutResource(mDialogLayoutResId);
    }

    public int getValue() {
        return mCurrentValue;
    }

    public void setValue(int value) {
        mCurrentValue = value;
        persistInt(mCurrentValue);
        notifyChanged();
    }

    public int getMinValue() {
        return mMinValue;
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public String getText() {
        return mText;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, 0);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue == null) {
            defaultValue = 0;
        }
        setValue(getPersistedInt((Integer) defaultValue));
    }

    public static class SeekBarPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat implements SeekBar.OnSeekBarChangeListener {

        private SeekBar mSeekBar;
        private TextView mValueTextView;
        private int mCurrentValue;

        public static SeekBarPreferenceDialogFragmentCompat newInstance(String key) {
            final SeekBarPreferenceDialogFragmentCompat
                    fragment = new SeekBarPreferenceDialogFragmentCompat();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);

            mSeekBar = view.findViewById(android.R.id.edit);
            // Ensure mValueTextView is found correctly using the standard ID for TextView in preferences
            mValueTextView = view.findViewById(android.R.id.text1);
            TextView messageView = view.findViewById(android.R.id.message);

            SeekBarDialogPreference preference = (SeekBarDialogPreference) getPreference();
            if (preference.getDialogMessage() != null) {
                messageView.setText(preference.getDialogMessage());
            }

            mCurrentValue = preference.getValue();
            mSeekBar.setMax(preference.getMaxValue() - preference.getMinValue());
            mSeekBar.setProgress(mCurrentValue - preference.getMinValue());
            mSeekBar.setOnSeekBarChangeListener(this);
            
            // Ensure the text view is updated when the dialog is first bound
            updateValueText(mCurrentValue, preference.getText());
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                SeekBarDialogPreference preference = (SeekBarDialogPreference) getPreference();
                preference.setValue(mCurrentValue);
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            SeekBarDialogPreference preference = (SeekBarDialogPreference) getPreference();
            mCurrentValue = progress + preference.getMinValue();
            // Ensure the text view is updated when the progress changes
            updateValueText(mCurrentValue, preference.getText());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        private void updateValueText(int value, String text) {
            if (mValueTextView != null) { // Check if mValueTextView is initialized
                String displayText = String.valueOf(value);
                if (text != null) {
                    displayText += " " + text;
                }
                mValueTextView.setText(displayText);
            }
        }
    }
}
