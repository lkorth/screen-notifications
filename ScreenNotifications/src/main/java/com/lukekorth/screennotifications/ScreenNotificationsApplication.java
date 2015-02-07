package com.lukekorth.screennotifications;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.mailable_log.MailableLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class ScreenNotificationsApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static final String VERSION = "version";

    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        migrate();
        MailableLog.init(this, BuildConfig.DEBUG);
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
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
}
