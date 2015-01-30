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
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.lukekorth.screennotifications.billing.IabHelper;
import com.lukekorth.screennotifications.billing.IabResult;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

public class ScreenNotificationsActivity extends PreferenceActivity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

    private SharedPreferences mPrefs;

    private boolean mServiceActive;
    private boolean mSupportsNotificationListenerService = false;
    private CheckBoxPreference mServicePreference;

    private DevicePolicyManager mDPM;
    private ComponentName mDeviceAdmin;
    private CheckBoxPreference mDeviceAdminPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (android.os.Build.VERSION.SDK_INT >= 18) {
            mSupportsNotificationListenerService = true;
        }

        initializeService();
        initializeDeviceAdmin();
        initializeTime();
        initializeDonations();

        AppRate.with(this)
                .text(R.string.rate)
                .initialLaunchCount(3)
                .retryPolicy(RetryPolicy.EXPONENTIAL)
                .checkAndShow();
    }

    public void onResume() {
        super.onResume();

        checkForRunningService();
        checkForActiveDeviceAdmin();
    }

    private void initializeService() {
        mServicePreference = (CheckBoxPreference) findPreference("service");
        mServicePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mServiceActive) {
                    if (mSupportsNotificationListenerService) {
                        showServiceDialog(R.string.notification_listener_launch);
                    } else {
                        showServiceDialog(R.string.accessibility_launch);
                    }
                } else {
                    if (mSupportsNotificationListenerService) {
                        showServiceDialog(R.string.notification_listener_warning);
                    } else {
                        showServiceDialog(R.string.accessibility_warning);
                    }
                }

                // don't update checkbox until we're really active
                return false;
            }
        });
    }

    private void checkForRunningService() {
        mServiceActive = isServiceRunning();
        if(mServiceActive) {
            mServicePreference.setChecked(true);
            enableOptions(true);
        }
        else {
            mServicePreference.setChecked(false);
            enableOptions(false);
        }
    }

    private void initializeDeviceAdmin() {
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDeviceAdmin = new ComponentName(this, CustomDeviceAdminReceiver.class);
        mDeviceAdminPreference = (CheckBoxPreference) findPreference("device_admin");

        mDeviceAdminPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    // Launch the activity to have the user enable our admin.
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.device_admin_explanation);
                    startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);

                    // don't update checkbox until we're really active
                    return false;
                } else {
                    mDPM.removeActiveAdmin(mDeviceAdmin);
                    enableScreenTimeoutOption(false);

                    return true;
                }
            }
        });
    }

    private void checkForActiveDeviceAdmin() {
        if(mDPM.isAdminActive(mDeviceAdmin)) {
            mDeviceAdminPreference.setChecked(true);
            enableScreenTimeoutOption(true);
        } else {
            mDeviceAdminPreference.setChecked(false);
            enableScreenTimeoutOption(false);
        }
    }

    private void enableScreenTimeoutOption(boolean enable) {
        Preference wakeLength = findPreference("wake_length");
        wakeLength.setEnabled(enable);

        if (enable) {
            setWakeLengthSummary();
        } else {
            wakeLength.setSummary(R.string.disabled_wake_length);
        }
    }

    private void setWakeLengthSummary() {
        findPreference("wake_length").setSummary(getString(R.string.wake_length_summary) + " " +
                mPrefs.getInt("wake_length", 10) + " " + getString(R.string.wake_length_summary_2));
    }

    private void initializeTime() {
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

        findPreference("wake_length").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LayoutInflater inflater = (LayoutInflater)
                        getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View numberPickerView = inflater.inflate(R.layout.number_picker_dialog, null);

                final NumberPicker numberPicker = (NumberPicker) numberPickerView.findViewById(R.id.number_picker);
                numberPicker.setMinValue(1);
                numberPicker.setMaxValue(900);
                numberPicker.setValue(mPrefs.getInt("wake_length", 10));

                new AlertDialog.Builder(ScreenNotificationsActivity.this)
                        .setTitle(R.string.wake_length)
                        .setView(numberPickerView)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mPrefs.edit().putInt("wake_length", numberPicker.getValue()).commit();
                                setWakeLengthSummary();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                }
                        )
                        .show();

                return true;
            }
        });
    }

    private void enableOptions(boolean enable) {
        findPreference("app").setEnabled(enable);
        findPreference("wake_length").setEnabled(enable);
        findPreference("bright").setEnabled(enable);
        findPreference("proxSensor").setEnabled(enable);
        findPreference("quiet").setEnabled(enable);
        findPreference("startTime").setEnabled(enable);
        findPreference("stopTime").setEnabled(enable);
        findPreference("status-bar").setEnabled(enable);
    }

    private void initializeDonations() {
        findPreference("donate").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(ScreenNotificationsActivity.this)
                        .setTitle(R.string.select_an_amount)
                        .setItems(getResources().getStringArray(R.array.amounts), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final String purchaseItem = getResources().getStringArray(R.array.billing_items)[which];

                                final IabHelper iabHelper = new IabHelper(ScreenNotificationsActivity.this, getString(R.string.billing_public_key));
                                iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                                    @Override
                                    public void onIabSetupFinished(IabResult result) {
                                        if (result.isSuccess()) {
                                            iabHelper.launchPurchaseFlow(ScreenNotificationsActivity.this,
                                                    purchaseItem, 1, null, "donate");
                                        } else {
                                            new AlertDialog.Builder(ScreenNotificationsActivity.this)
                                                    .setTitle(R.string.there_was_a_problem)
                                                    .setMessage(R.string.failed_billing)
                                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    })
                                                    .show();
                                        }
                                    }
                                });
                            }
                        })
                        .create()
                        .show();
                return true;
            }
        });
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

                        if (mSupportsNotificationListenerService) {
                            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                        } else {
                            startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                        }
                    }
                })
                .show();
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean isJellyBean = getResources().getBoolean(R.bool.is_jelly_bean);

        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (mSupportsNotificationListenerService &&
                    NotificationListener.class.getName().equals(service.service.getClassName())) {
                return true;
            } else if (isJellyBean &&
                    ScreenNotificationsServiceJB.class.getName().equals(service.service.getClassName())) {
                return true;
            } else if (ScreenNotificationsService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    public static class CustomDeviceAdminReceiver extends DeviceAdminReceiver {

        @Override
        public void onEnabled(Context context, Intent intent) {}

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
            ComponentName deviceAdmin = new ComponentName(context, CustomDeviceAdminReceiver.class);
            ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).removeActiveAdmin(deviceAdmin);

            return null;
        }

        @Override
        public void onDisabled(Context context, Intent intent) {}

        @Override
        public void onPasswordChanged(Context context, Intent intent) {}

        @Override
        public void onPasswordFailed(Context context, Intent intent) {}

        @Override
        public void onPasswordSucceeded(Context context, Intent intent) {}

        @Override
        public void onPasswordExpiring(Context context, Intent intent) {}
    }
}
