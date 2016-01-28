package com.lukekorth.screennotifications.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;

public class AppHelper {

    public static final String NOTIFYING_APPS = "notifying_apps";

    public static boolean isAppEnabled(Context context, String packageName) {
        return getPreferences(context).getBoolean(packageName, true);
    }

    public static void recordNotificationFromApp(Context context, String packageName) {
        SharedPreferences preferences = getPreferences(context);
        if (!preferences.contains(packageName)) {
            HashSet<String> apps = getNotifyingApps(context);
            apps.add(packageName);

            preferences.edit()
                    .putBoolean(packageName, true)
                    .putStringSet(NOTIFYING_APPS, apps)
                    .apply();
        }
    }

    public static HashSet<String> getNotifyingApps(Context context) {
        return new HashSet<>(getPreferences(context).getStringSet(NOTIFYING_APPS, new HashSet<String>()));
    }

    private static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
