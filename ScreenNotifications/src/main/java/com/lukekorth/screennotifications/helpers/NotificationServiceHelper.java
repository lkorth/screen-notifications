package com.lukekorth.screennotifications.helpers;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import com.lukekorth.screennotifications.services.NotificationListener;
import com.lukekorth.screennotifications.services.ScreenNotificationsService;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;

public class NotificationServiceHelper {

    public static boolean isServiceEnabled(Context context) {
        if (SDK_INT >= JELLY_BEAN_MR2) {
            ComponentName componentName = new ComponentName(context, NotificationListener.class);
            String notificationListeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            return (notificationListeners != null && notificationListeners.contains(componentName.flattenToString()));
        } else {
            ComponentName componentName = new ComponentName(context, NotificationListener.class);
            String accessibilityServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return (accessibilityServices != null && accessibilityServices.contains(componentName.flattenToString()));
        }
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SDK_INT >= JELLY_BEAN_MR2 && NotificationListener.class.getName().equals(service.service.getClassName())) {
                return true;
            } else if (ScreenNotificationsService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}
