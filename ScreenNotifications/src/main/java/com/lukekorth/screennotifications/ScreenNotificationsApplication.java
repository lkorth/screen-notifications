package com.lukekorth.screennotifications;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.mailable_log.MailableLog;
import com.lukekorth.screennotifications.helpers.DatabaseMigrations;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class ScreenNotificationsApplication extends Application {

    private static final String VERSION = "version";

    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .schemaVersion(1)
                .migration(new DatabaseMigrations())
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);

        migrate();

        MailableLog.init(this, BuildConfig.DEBUG);
    }

    private void migrate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int version = prefs.getInt(VERSION, 0);
        if (BuildConfig.VERSION_CODE > version) {
            String now = new Date().toString();

            prefs.edit()
                    .putString("upgrade_date", now)
                    .putInt(VERSION, BuildConfig.VERSION_CODE)
                    .apply();

            MailableLog.clearLog(this);
        }
    }
}
