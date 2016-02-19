package com.lukekorth.screennotifications.helpers;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.lukekorth.screennotifications.receivers.ScreenNotificationsDeviceAdminReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ScreenController {

    private static AtomicLong sLastNotificationTime = new AtomicLong();

    private Context mContext;
    private Logger mLogger;
    private SharedPreferences mPrefs;
    private PowerManager mPowerManager;
    private boolean mCloseToProximitySensor;

    public ScreenController(Context context, boolean closeToProximitySensor) {
        mContext = context;
        mLogger = LoggerFactory.getLogger("ScreenController");
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mCloseToProximitySensor = closeToProximitySensor;
    }

    public void handleNotification(String packageName) {
        sLastNotificationTime.set(System.currentTimeMillis());
        if(shouldTurnOnScreen()) {
            AppHelper.recordRecentNotification(packageName);
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    turnOnScreen();
                }
            });
        }
    }

    private void turnOnScreen() {
        mLogger.debug("Turning on screen");

        if(mPrefs.getBoolean("status-bar", false)) {
            mLogger.debug("Sleeping for 3 seconds before turning on screen");
            SystemClock.sleep(3000);
        }

        int flag;
        if(mPrefs.getBoolean("bright", false)) {
            flag = PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
        } else {
            flag = PowerManager.SCREEN_DIM_WAKE_LOCK;
        }
        PowerManager.WakeLock wakeLock = mPowerManager.newWakeLock(flag | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Screen Notifications");
        wakeLock.acquire();

        if(mPrefs.getBoolean("status-bar", false)) {
            expandStatusBar();
        }

        DevicePolicyManager dpm =
                (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdmin =
                new ComponentName(mContext, ScreenNotificationsDeviceAdminReceiver.class);

        long desiredWakeLength = mPrefs.getInt("wake_length", 10) * 1000;
        long actualWakeLength = desiredWakeLength;
        do {
            mLogger.debug("Sleeping for " + actualWakeLength);
            SystemClock.sleep(actualWakeLength);
            actualWakeLength = sLastNotificationTime.get() + desiredWakeLength - System.currentTimeMillis();
        } while (actualWakeLength > 1000);

        wakeLock.release();

        if (dpm.isAdminActive(deviceAdmin) && isDeviceLocked()) {
            mLogger.debug("Device is an active admin and device is still on lock screen, locking");
            dpm.lockNow();
        }
    }

    @SuppressWarnings("ResourceType")
    private void expandStatusBar() {
        try {
            Object statusBarService = mContext.getSystemService("statusbar");
            Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");

            Method showStatusBar;
            if (Build.VERSION.SDK_INT >= 17) {
                showStatusBar = statusBarManager.getMethod("expandNotificationsPanel");
            } else {
                showStatusBar = statusBarManager.getMethod("expand");
            }

            showStatusBar.invoke(statusBarService);

            mLogger.debug("Expanding status bar");
        } catch (Exception e) {
            mLogger.debug("Failed to expand the status bar: " + e.getMessage());
        }
    }

    private boolean isDeviceLocked() {
        return ((KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE))
                .inKeyguardRestrictedInputMode();
    }

    private boolean shouldTurnOnScreen() {
        boolean turnOnScreen = !isInQuietTime() && !isInCall() && !mPowerManager.isScreenOn();

        if(!mPrefs.getBoolean("proxSensor", false)) {
            turnOnScreen = turnOnScreen && !mCloseToProximitySensor;
        }

        mLogger.debug("Should turn on screen: " + turnOnScreen);
        return turnOnScreen;
    }

    private boolean isInQuietTime() {
        boolean quietTime = false;

        if(mPrefs.getBoolean("quiet", false)) {
            String startTime = mPrefs.getString("startTime", "22:00");
            String stopTime = mPrefs.getString("stopTime", "08:00");

            SimpleDateFormat sdfDate = new SimpleDateFormat("H:mm");
            String currentTimeStamp = sdfDate.format(new Date());
            int currentHour = Integer.parseInt(currentTimeStamp.split("[:]+")[0]);
            int currentMinute = Integer.parseInt(currentTimeStamp.split("[:]+")[1]);

            int startHour = Integer.parseInt(startTime.split("[:]+")[0]);
            int startMinute = Integer.parseInt(startTime.split("[:]+")[1]);

            int stopHour = Integer.parseInt(stopTime.split("[:]+")[0]);
            int stopMinute = Integer.parseInt(stopTime.split("[:]+")[1]);

            if (startHour < stopHour && currentHour > startHour && currentHour < stopHour) {
                quietTime = true;
            } else if (startHour > stopHour && (currentHour > startHour || currentHour < stopHour)) {
                quietTime = true;
            } else if (currentHour == startHour && currentMinute >= startMinute) {
                quietTime = true;
            } else if (currentHour == stopHour && currentMinute < stopMinute) {
                quietTime = true;
            }
        }

        mLogger.debug("Device is in quiet time: " + quietTime);
        return quietTime;
    }

    private boolean isInCall() {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        boolean inCall = (manager.getMode() == AudioManager.MODE_IN_CALL ||
                manager.getMode() == AudioManager.MODE_IN_COMMUNICATION);

        mLogger.debug("Device is in a call: " + inCall);
        return inCall;
    }
}
