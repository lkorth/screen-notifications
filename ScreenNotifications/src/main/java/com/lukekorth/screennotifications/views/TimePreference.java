package com.lukekorth.screennotifications.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.lukekorth.screennotifications.R;

public class TimePreference extends DialogPreference {

    private int mLastHour = 0;
    private int mLastMinute = 0;
    private boolean mIs24HourFormat;
    private TimePicker mPicker = null;
    private TextView mTimeDisplay;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TimePreference(Context context) {
        super(context);
        init(context);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mIs24HourFormat = DateFormat.is24HourFormat(context);
        setPositiveButtonText(R.string.set);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    public String toString() {
        if (mIs24HourFormat) {
            return ((mLastHour < 10) ? "0" : "") + Integer.toString(mLastHour)
                    + ":" + ((mLastMinute < 10) ? "0" : "") + Integer.toString(mLastMinute);
        } else {
            int myHour = mLastHour % 12;
            return ((myHour == 0) ? "12" : ((myHour < 10) ? "0" : "") + Integer.toString(myHour))
                    + ":" + ((mLastMinute < 10) ? "0" : "") + Integer.toString(mLastMinute)
                    + ((mLastHour >= 12) ? " PM" : " AM");
        }
    }

    @Override
    protected View onCreateDialogView() {
        mPicker = new TimePicker(getContext());
        return mPicker;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mPicker.setIs24HourView(mIs24HourFormat);
        mPicker.setCurrentHour(mLastHour);
        mPicker.setCurrentMinute(mLastMinute);
    }

    @Override
    public void onBindView(View view) {
        View widgetLayout;
        int childCounter = 0;
        do {
            widgetLayout = ((ViewGroup) view).getChildAt(childCounter);
            childCounter++;
        } while (widgetLayout.getId() != android.R.id.widget_frame);

        ((ViewGroup) widgetLayout).removeAllViews();
        mTimeDisplay = new TextView(widgetLayout.getContext());
        mTimeDisplay.setText(toString());
        ((ViewGroup) widgetLayout).addView(mTimeDisplay);

        super.onBindView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            mPicker.clearFocus();
            mLastHour = mPicker.getCurrentHour();
            mLastMinute = mPicker.getCurrentMinute();

            String time = String.valueOf(mLastHour) + ":" + String.valueOf(mLastMinute);

            if (callChangeListener(time)) {
                persistString(time);
                mTimeDisplay.setText(toString());
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time;
        if (restoreValue) {
            if (defaultValue == null) {
                time = getPersistedString("00:00");
            } else {
                time = getPersistedString(defaultValue.toString());
            }
        } else {
            if (defaultValue == null) {
                time = "00:00";
            } else {
                time = defaultValue.toString();
            }
            if (shouldPersist()) {
                persistString(time);
            }
        }

        String[] timeParts = time.split(":");
        mLastHour = Integer.parseInt(timeParts[0]);
        mLastMinute = Integer.parseInt(timeParts[1]);
    }
}