package com.lukekorth.screennotifications;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.lukekorth.screennotifications.billing.IabHelper;
import com.lukekorth.screennotifications.billing.IabResult;
import com.lukekorth.screennotifications.helpers.LogReporting;
import com.lukekorth.screennotifications.helpers.NotificationServiceHelper;
import com.lukekorth.screennotifications.receivers.ScreenNotificationsDeviceAdminReceiver;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

    private SharedPreferences mPrefs;

    private boolean mServiceActive;
    private CheckBoxPreference mServicePreference;

    private DevicePolicyManager mDPM;
    private ComponentName mDeviceAdmin;
    private CheckBoxPreference mDeviceAdminPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        findPreference("recent_apps").setOnPreferenceClickListener(this);
        findPreference("contact").setOnPreferenceClickListener(this);
        findPreference("version").setSummary(BuildConfig.VERSION_NAME);

        initializeService();
        initializeDeviceAdmin();
        initializeTime();
        setDelaySummary();
        initializeDonations();
    }

    @Override
    public void onStart() {
        super.onStart();

        AppRate.with(getActivity())
                .text(R.string.rate)
                .initialLaunchCount(9)
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
        mServicePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mServiceActive) {
                    showServiceDialog(R.string.notification_listener_launch);
                } else {
                    showServiceDialog(R.string.notification_listener_warning);
                }

                // don't update checkbox until we're really active
                return false;
            }
        });
    }

    private void checkForRunningService() {
        mServiceActive = NotificationServiceHelper.isServiceRunning(getActivity());
        if (mServiceActive) {
            mServicePreference.setChecked(true);
            enableOptions(true);
        } else {
            mServicePreference.setChecked(false);
            enableOptions(false);
        }
    }

    private void initializeDeviceAdmin() {
        mDPM = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDeviceAdmin = new ComponentName(getActivity(), ScreenNotificationsDeviceAdminReceiver.class);
        mDeviceAdminPreference = (CheckBoxPreference) findPreference("device_admin");

        mDeviceAdminPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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

    private void setDelaySummary() {
        findPreference("delay").setSummary(getString(R.string.delay_summary,
                mPrefs.getInt("delay", 0)));
    }

    private void initializeTime() {
        Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
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

        findPreference("wake_length").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LayoutInflater inflater = (LayoutInflater)
                        getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View numberPickerView = inflater.inflate(R.layout.number_picker_dialog, null);

                final NumberPicker numberPicker = (NumberPicker) numberPickerView.findViewById(R.id.number_picker);
                numberPicker.setMinValue(1);
                numberPicker.setMaxValue(900);
                numberPicker.setValue(mPrefs.getInt("wake_length", 10));

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.wake_length)
                        .setView(numberPickerView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mPrefs.edit().putInt("wake_length", numberPicker.getValue()).apply();
                                setWakeLengthSummary();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel,
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

        findPreference("delay").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LayoutInflater inflater = (LayoutInflater)
                        getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View numberPickerView = inflater.inflate(R.layout.number_picker_dialog, null);

                final NumberPicker numberPicker = (NumberPicker) numberPickerView.findViewById(R.id.number_picker);
                numberPicker.setMinValue(0);
                numberPicker.setMaxValue(900);
                numberPicker.setValue(mPrefs.getInt("delay", 0));

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delay_title)
                        .setView(numberPickerView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mPrefs.edit().putInt("delay", numberPicker.getValue()).apply();
                                setDelaySummary();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel,
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

    private void initializeDonations() {
        findPreference("donate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.select_an_amount)
                        .setItems(getResources().getStringArray(R.array.amounts), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final String purchaseItem = getResources().getStringArray(R.array.billing_items)[which];
                                final IabHelper iabHelper = new IabHelper(getActivity(), getString(R.string.billing_public_key));

                                iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                                    @Override
                                    public void onIabSetupFinished(IabResult result) {
                                        if (result.isSuccess()) {
                                            iabHelper.launchPurchaseFlow(getActivity(), purchaseItem, 1, null, "donate");
                                        } else {
                                            new AlertDialog.Builder(getActivity())
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

    private String handleTime(String time) {
        String[] timeParts = time.split(":");
        int lastHour = Integer.parseInt(timeParts[0]);
        int lastMinute = Integer.parseInt(timeParts[1]);

        boolean is24HourFormat = DateFormat.is24HourFormat(getActivity());

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
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface alertDialog, int id) {
                        alertDialog.cancel();
                        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    }
                })
                .show();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("contact")) {
            new LogReporting(getActivity()).collectAndSendLogs();
            return true;
        } else if (preference.getKey().equals("recent_apps")) {
            startActivity(new Intent(getActivity(), RecentAppsActivity.class));
            return true;
        }
        return false;
    }
}
