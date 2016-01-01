package com.lukekorth.screennotifications;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.lukekorth.screennotifications.helpers.LogReporting;
import com.lukekorth.screennotifications.receivers.ScreenNotificationsDeviceAdminReceiver;
import com.lukekorth.screennotifications.services.NotificationListener;
import com.lukekorth.screennotifications.services.ScreenNotificationsService;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final String TAG = "SettingsFragment";

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

    private SharedPreferences mPrefs;

    private boolean mServiceActive;
    private boolean mSupportsNotificationListenerService = false;
    private CheckBoxPreference mServicePreference;

    private DevicePolicyManager mDPM;
    private ComponentName mDeviceAdmin;
    private CheckBoxPreference mDeviceAdminPreference;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (android.os.Build.VERSION.SDK_INT >= 18) {
            mSupportsNotificationListenerService = true;
        }

        findPreference("contact").setOnPreferenceClickListener(this);
        findPreference("test_notification").setOnPreferenceClickListener(this);
        findPreference("version").setSummary(BuildConfig.VERSION_NAME);

        initializeService();
        initializeDeviceAdmin();
        initializeTime();

        AppRate.with(getActivity())
                .text(R.string.rate)
                .initialLaunchCount(3)
                .retryPolicy(RetryPolicy.EXPONENTIAL)
                .checkAndShow();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }
    boolean mServiceBound = false;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceBound = true;
        }
    };
    public void onResume() {
        super.onResume();

        //TODO:Refactor me, just a fix for non-starting service
        if (!mServiceBound) {
            Intent intent = new Intent(getActivity(), com.lukekorth.screennotifications.services.NotificationListener.class);
            getActivity().startService(intent);
            getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }

        checkForRunningService();
        checkForActiveDeviceAdmin();
    }

    private void initializeService() {
        mServicePreference = (CheckBoxPreference) findPreference("service");
        mServicePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            Log.d(TAG, "isServiceRunning: "+ service.service.getClassName());
            if (mSupportsNotificationListenerService &&
                    NotificationListener.class.getName().equals(service.service.getClassName())) {
                return true;
            } else if (ScreenNotificationsService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("contact")) {
            new LogReporting(getActivity()).collectAndSendLogs();
            return true;
        }else if(preference.getKey().equals("test_notification")) {
            Log.d(TAG, "onPreferenceClick: create Notification");
            final NotificationManager mNotifyMgr = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            final NotificationCompat.Builder  builder = new NotificationCompat.Builder(mContext)
                    .setContentTitle("Test Notification")
                    .setContentText("This is just a test")
                    .setTicker( "Test Notification" )
                    .setSound(alarmSound)
                    .setSmallIcon(R.drawable.ic_launcher);
            final Notification notification = builder.build();
            notification.defaults = Notification.DEFAULT_ALL;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    Log.d(TAG, "run: Now showing the notification");
                    mNotifyMgr.notify(1447, notification);
                }
            }, 5000);
            return true;
        }
        return false;
    }
}
