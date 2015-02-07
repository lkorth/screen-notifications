package com.lukekorth.screennotifications.services;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.lukekorth.screennotifications.helpers.ScreenController;

import org.slf4j.LoggerFactory;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService implements SensorEventListener {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!sbn.isOngoing() && isAppEnabled(sbn)) {
            LoggerFactory.getLogger("NotificationListener").debug("Got a non-ongoing notification for an enabled app. " + sbn.getPackageName());
            if (isProximitySensorEnabled()) {
                if (!registerProximitySensorListener()) {
                    new ScreenController(this, false).handleNotification();
                }
            } else {
                new ScreenController(this, false).handleNotification();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            unregisterProximitySensorListener();

            boolean close = event.values[0] < event.sensor.getMaximumRange();
            new ScreenController(this, close).handleNotification();
        }
    }

    private boolean isAppEnabled(StatusBarNotification sbn) {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(sbn.getPackageName(), false);
    }

    private boolean isProximitySensorEnabled() {
        return !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("proxSensor", true);
    }

    private boolean registerProximitySensorListener() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (proximitySensor == null) {
            return false;
        } else {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            return true;
        }
    }

    private void unregisterProximitySensorListener() {
        ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
