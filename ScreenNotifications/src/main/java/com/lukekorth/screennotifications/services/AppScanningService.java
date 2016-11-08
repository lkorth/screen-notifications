package com.lukekorth.screennotifications.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.lukekorth.screennotifications.models.App;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class AppScanningService extends IntentService {

    public AppScanningService() {
        super("AppScanningService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<App> previousApps = realm.where(App.class)
                .findAll();
        ArrayList<String> previousAppPackages = new ArrayList<>();
        for (App app : previousApps) {
            previousAppPackages.add(app.getPackageName());
        }

        realm.beginTransaction();

        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> applications = packageManager
                .getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo applicationInfo : applications) {
            if (!applicationInfo.enabled) {
                continue;
            }

            App app = realm.where(App.class)
                    .equalTo("packageName", applicationInfo.packageName)
                    .findFirst();
            if (app == null) {
                app = realm.createObject(App.class);
            }

            app.setPackageName(applicationInfo.packageName);
            app.setName((String) applicationInfo.loadLabel(packageManager));

            previousAppPackages.remove(applicationInfo.packageName);
        }

        for (String uninstalledAppPackage : previousAppPackages) {
            App uninstalledApp = realm.where(App.class)
                    .equalTo("packageName", uninstalledAppPackage)
                    .findFirst();

            if (uninstalledApp != null) {
                uninstalledApp.deleteFromRealm();
            }
        }

        realm.commitTransaction();
        realm.close();
    }
}
