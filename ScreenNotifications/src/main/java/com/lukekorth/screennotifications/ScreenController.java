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
import android.view.accessibility.AccessibilityEvent;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

public class ScreenController {

    private static long sLastNotificationTime;

    private Context mContext;
    private SharedPreferences mPrefs;
    private PowerManager mPowerManager;
    private boolean mCloseToProximitySensor;

    public ScreenController(Context context, boolean closeToProximitySensor) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mCloseToProximitySensor = closeToProximitySensor;
    }

    public void processNotification(AccessibilityEvent notificationEvent) {
        ScreenController.sLastNotificationTime = System.currentTimeMillis();
        if(notificationEvent != null && notificationEvent.getPackageName() != null &&
           mPrefs.getBoolean(notificationEvent.getPackageName().toString(), false) &&
                shouldTurnOnScreen()) {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    turnOnScreen();
                }
            });
        }
    }

    private void turnOnScreen() {
        if(mPrefs.getBoolean("status-bar", false)) {
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
                new ComponentName(mContext, ScreenNotificationsActivity.CustomDeviceAdminReceiver.class);

        long desiredWakeLength = mPrefs.getInt("wake_length", 10) * 1000;
        long actualWakeLength = desiredWakeLength;
        do {
            SystemClock.sleep(actualWakeLength);
            actualWakeLength = ScreenController.sLastNotificationTime + desiredWakeLength -
                    System.currentTimeMillis();
        } while (actualWakeLength > 1000);

        wakeLock.release();

        if (dpm.isAdminActive(deviceAdmin) && isDeviceLocked()) {
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
        } catch (Exception e) {
            // ignore
        }
    }

    private boolean isDeviceLocked() {
        return ((KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE))
                .inKeyguardRestrictedInputMode();
    }

    private boolean shouldTurnOnScreen() {
        boolean turnOnScreen = !isInQuietTime() && !isInCall() && !mPowerManager.isScreenOn();

        if(!mPrefs.getBoolean("proxSensor", true)) {
            turnOnScreen = turnOnScreen && !mCloseToProximitySensor;
        }

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

        return quietTime;
    }

    private boolean isInCall() {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if(manager.getMode() == AudioManager.MODE_IN_CALL || manager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            return true;
        } else {
            return false;
        }
    }
}
