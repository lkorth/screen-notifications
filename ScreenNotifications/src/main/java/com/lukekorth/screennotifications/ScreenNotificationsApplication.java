package com.lukekorth.screennotifications;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.lukekorth.mailable_log.MailableLog;
import com.lukekorth.screennotifications.models.App;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class ScreenNotificationsApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static final String NOTIFYING_APPS = "notifying_apps";
    private static final String VERSION = "version";

    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this).build());
        migrate();
        MailableLog.init(this, BuildConfig.DEBUG);
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    private void migrate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int version = prefs.getInt(VERSION, 0);
        if (BuildConfig.VERSION_CODE > version) {
            if (version < 20) {
                migrateToNewAppList(prefs);
            }

            if (version < 22) {
                migrateToNewAppDatabase(prefs);
            }

            String now = new Date().toString();

            prefs.edit()
                    .putString("upgrade_date", now)
                    .putInt(VERSION, BuildConfig.VERSION_CODE)
                    .apply();

            MailableLog.clearLog(this);
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Logger logger = LoggerFactory.getLogger("Exception");

        logger.error("thread.toString(): " + thread.toString());
        logger.error("Exception: " + ex.toString());
        logger.error("Exception stacktrace:");
        for (StackTraceElement trace : ex.getStackTrace()) {
            logger.error(trace.toString());
        }

        logger.error("");

        logger.error("cause.toString(): " + ex.getCause().toString());
        logger.error("Cause: " + ex.getCause().toString());
        logger.error("Cause stacktrace:");
        for (StackTraceElement trace : ex.getCause().getStackTrace()) {
            logger.error(trace.toString());
        }

        mDefaultExceptionHandler.uncaughtException(thread, ex);
    }

    private void migrateToNewAppList(SharedPreferences prefs) {
        Map<String, ?> preferences = prefs.getAll();
        Set<String> apps = new HashSet<>();
        for (String possibleApp : preferences.keySet()) {
            Object value = preferences.get(possibleApp);
            if (value instanceof Boolean && (boolean) value) {
                if (isAppInstalled(possibleApp)) {
                    apps.add(possibleApp);
                }
            }
        }

        prefs.edit().putStringSet(NOTIFYING_APPS, apps).apply();
    }

    private void migrateToNewAppDatabase(SharedPreferences prefs) {
        Set<String> apps = prefs.getStringSet(NOTIFYING_APPS, null);
        if (apps != null) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();

            for (String packageName : apps) {
                App app = realm.createObject(App.class);
                app.setPackageName(packageName);
                app.setEnabled(prefs.getBoolean(packageName, true));
            }

            realm.commitTransaction();
            realm.close();
        }
    }

    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
