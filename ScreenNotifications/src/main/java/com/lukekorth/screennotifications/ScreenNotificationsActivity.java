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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.lukekorth.screennotifications.util.IabHelper;
import com.lukekorth.screennotifications.util.IabResult;

public class ScreenNotificationsActivity extends PreferenceActivity {

    private SharedPreferences mPrefs;
    private boolean active;
    private Preference service;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        findPreference("donate").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(ScreenNotificationsActivity.this)
                        .setTitle("Select a donation amount")
                        .setItems(getResources().getStringArray(R.array.amounts), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final String purchaseItem = getResources().getStringArray(R.array.billing_items)[which];

                                final IabHelper iabHelper = new IabHelper(ScreenNotificationsActivity.this, getString(R.string.billing_public_key));
                                iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                                    @Override
                                    public void onIabSetupFinished(IabResult result) {
                                        iabHelper.launchPurchaseFlow(ScreenNotificationsActivity.this,
                                                purchaseItem, 1, null, "donate");
                                    }
                                });
                            }
                        })
                        .create()
                        .show();

                return true;
            }
        });

        service = findPreference("service");
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

        Preference start = findPreference("startTime");
        Preference stop = findPreference("stopTime");
        start.setSummary(handleTime(mPrefs.getString("startTime", "22:00")));
        stop.setSummary(handleTime(mPrefs.getString("stopTime", "08:00")));
        start.setOnPreferenceChangeListener(listener);
        stop.setOnPreferenceChangeListener(listener);

        Preference time = findPreference("time");
        time.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LayoutInflater inflater = (LayoutInflater)
                        getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View numberPickerView = inflater.inflate(R.layout.number_picker_dialog, null);

                final NumberPicker numberPicker = (NumberPicker) numberPickerView.findViewById(R.id.number_picker);
                numberPicker.setValue(mPrefs.getInt("time", 10));
                numberPicker.setMinValue(1);
                numberPicker.setMaxValue(900);

                new AlertDialog.Builder(ScreenNotificationsActivity.this)
                        .setTitle(R.string.wake_length)
                        .setView(numberPickerView)
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                        mPrefs.edit().putInt("time", numberPicker.getValue()).commit();
                                    }
                                })
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                })
                        .show();

                return true;
            }
        });
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
        String[] timeParts = time.split(":");
        int lastHour = Integer.parseInt(timeParts[0]);
        int lastMinute = Integer.parseInt(timeParts[1]);

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
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface alertDialog, int id) {
                        alertDialog.cancel();
                        startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                })
                .show();
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean isJellyBean = getResources().getBoolean(R.bool.is_jelly_bean);

        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(isJellyBean) {
                if (ScreenNotificationsServiceJB.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            } else {
                if (ScreenNotificationsService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }

        return false;
    }
}
