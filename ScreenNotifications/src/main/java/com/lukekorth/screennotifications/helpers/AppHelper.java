package com.lukekorth.screennotifications.helpers;

import com.lukekorth.screennotifications.models.App;
import com.lukekorth.screennotifications.models.RecentApp;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class AppHelper {

    public static boolean isAppEnabled(String packageName) {
        Realm realm = Realm.getDefaultInstance();

        App existingApp = realm.where(App.class)
                .equalTo("packageName", packageName)
                .findFirst();

        boolean enabled = false;
        if (existingApp != null) {
            enabled = existingApp.getEnabled();
        }

        realm.close();

        return enabled;
    }

    public static void recordNotificationFromApp(String packageName) {
        Realm realm = Realm.getDefaultInstance();

        App existingApp = realm.where(App.class)
                .equalTo("packageName", packageName)
                .findFirst();

        if (existingApp == null) {
            realm.beginTransaction();

            App app = realm.createObject(App.class);
            app.setPackageName(packageName);
            app.setEnabled(true);

            realm.commitTransaction();
        }

        realm.close();
    }

    public static void recordScreenWakeFromApp(String packageName) {
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

    public static RealmResults<App> getNotifyingApps() {
        return Realm.getDefaultInstance().where(App.class)
                .findAll();
    }
}
