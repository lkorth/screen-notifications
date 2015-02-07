package com.lukekorth.screennotifications.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;

import com.lukekorth.screennotifications.helpers.ScreenController;

import org.slf4j.LoggerFactory;

public class BaseAccessibilityService extends AccessibilityService implements SensorEventListener {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
                isAppEnabled(event)) {
            LoggerFactory.getLogger("BaseAccessibilityService")
                    .debug("Received a notification accessibility event for an enabled app. " + event.getPackageName());
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

    private boolean isAppEnabled(AccessibilityEvent event) {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(event.getPackageName().toString(), false);
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
    public void onInterrupt() {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

}
