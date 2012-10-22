package com.lukekorth.screennotifications;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

public class ScreenNotificationsActivity extends PreferenceActivity {

    private boolean active;
    private Preference service;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.main);

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        service = (Preference) findPreference("service");
        service.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if(active)
                    showDialog("You will be now taken to the Accessibility settings. If you wish to stop Screen Notificaitons, simply turn it off.");
                else
                    showDialog("Clicking ok will take you to the Accessibility settings. For this app to work you will need to turn on Screen Notifications. Android will warn you that the app may capture your private data, this app in no way captures any data and has no access to the internet.");

                return true;
            }
        });

        OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(handleTime(newValue.toString()));
                return true;
            }
        };

        Preference start = (Preference) findPreference("startTime");
        Preference stop = (Preference) findPreference("stopTime");
        start.setSummary(handleTime(mPrefs.getString("startTime", "22:00")));
        stop.setSummary(handleTime(mPrefs.getString("stopTime", "08:00")));
        start.setOnPreferenceChangeListener(listener);
        stop.setOnPreferenceChangeListener(listener);
    }

    public void onResume() {
        super.onResume();
        active = isMyServiceRunning();
        if(active) {
            service.setTitle("Screen Notifications active");
            service.setSummary("Click here to deactivate");
        }
        else {
            service.setTitle("Screen Notifications inactive");
            service.setSummary("Click here to activate");
        }
    }

    private String handleTime(String time) {
        String[] timeParts=time.split(":");
        int lastHour=Integer.parseInt(timeParts[0]);
        int lastMinute=Integer.parseInt(timeParts[1]);

        boolean is24HourFormat = DateFormat.is24HourFormat(this);

        if(is24HourFormat) {
            return ((lastHour < 10) ? "0" : "")
                    + Integer.toString(lastHour)
                    + ":" + ((lastMinute < 10) ? "0" : "")
                    + Integer.toString(lastMinute);
        } else {
            int myHour = lastHour % 12;
            return ((myHour == 0) ? "12" : ((myHour < 10) ? "0" : "") + Integer.toString(myHour))
                    + ":" + ((lastMinute < 10) ? "0" : "")
                    + Integer.toString(lastMinute)
                    + ((lastHour >= 12) ? " PM" : " AM");
        }
    }

    private void showDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface alertDialog, int id) {
                alertDialog.cancel();
                startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean jb = getResources().getBoolean(R.bool.is_jelly_bean);

        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(jb) {
                if (ScreenNotificationsServiceJB.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
            else {
                if (ScreenNotificationsService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
