package com.lukekorth.screennotifications.helpers;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

import com.lukekorth.screennotifications.services.NotificationListener;
import com.lukekorth.screennotifications.services.ScreenNotificationsService;

public class NotificationServiceHelper {

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (android.os.Build.VERSION.SDK_INT >= 18 &&
                    NotificationListener.class.getName().equals(service.service.getClassName())) {
                return true;
            } else if (ScreenNotificationsService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}
