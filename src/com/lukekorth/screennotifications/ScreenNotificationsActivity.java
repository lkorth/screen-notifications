/*
 * Copyright 2012 Luke Korth <korth.luke@gmail.com>
 * 
 * This file is part of Screen Notifications.
 * 
 * Screen Notifications is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Screen Notifications is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Screen Notifications.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lukekorth.screennotifications;

import org.donations.DonationsActivity;

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
        
        ((Preference) findPreference("donate")).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				ScreenNotificationsActivity.this
					.startActivity(new Intent(ScreenNotificationsActivity.this, DonationsActivity.class));
				return true;
			}
        });

        service = (Preference) findPreference("service");
        service.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if(active)
                    showServiceDialog(R.string.accessibility_launch);
                else
                    showServiceDialog(R.string.accessibility_warning);

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
            service.setTitle(R.string.active);
            service.setSummary(R.string.active_summary);
        }
        else {
            service.setTitle(R.string.inactive);
            service.setSummary(R.string.inactive_summary);
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

    private void showServiceDialog(int message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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
