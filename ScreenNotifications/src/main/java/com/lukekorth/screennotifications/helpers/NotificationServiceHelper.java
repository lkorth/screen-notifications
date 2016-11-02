package com.lukekorth.screennotifications.helpers;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import com.lukekorth.screennotifications.services.NotificationListener;

public class NotificationServiceHelper {

    public static boolean isServiceEnabled(Context context) {
        ComponentName componentName = new ComponentName(context, NotificationListener.class);
        String notificationListeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return (notificationListeners != null && notificationListeners.contains(componentName.flattenToString()));
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NotificationListener.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}
