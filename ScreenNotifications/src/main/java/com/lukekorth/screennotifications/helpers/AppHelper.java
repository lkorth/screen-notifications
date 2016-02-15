package com.lukekorth.screennotifications.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.screennotifications.models.RecentApp;

import java.util.HashSet;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

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

    public static void recordRecentNotification(String packageName) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        RecentApp recentApp = realm.createObject(RecentApp.class);
        recentApp.setPackageName(packageName);
        recentApp.setTimestamp(System.currentTimeMillis());

        realm.commitTransaction();
        realm.close();
    }

    public static RealmResults<RecentApp> getRecentNotifyingApps() {
        return Realm.getDefaultInstance().where(RecentApp.class)
                .findAllSorted("timestamp", Sort.DESCENDING);
    }

    public static HashSet<String> getNotifyingApps(Context context) {
        return new HashSet<>(getPreferences(context).getStringSet(NOTIFYING_APPS, new HashSet<String>()));
    }

    private static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
