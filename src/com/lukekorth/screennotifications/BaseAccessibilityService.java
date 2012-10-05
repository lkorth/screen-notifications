package com.lukekorth.screennotifications;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;

public class BaseAccessibilityService extends AccessibilityService implements SensorEventListener {

    public void onServiceConnected() {}

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if(mPrefs.getBoolean((String) event.getPackageName(), false)) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if(!pm.isScreenOn()) {
                if(mPrefs.getBoolean("proxSensor", false)) {
                    turnOnScreen(mPrefs, pm);
                }
                else {
                    SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

                    if(proximitySensor == null) {
                        turnOnScreen(mPrefs, pm);
                    }
                    else {
                        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
                    }
                }
            }
        }
    }

    private void turnOnScreen(SharedPreferences mPrefs, PowerManager pm) {
        int time = mPrefs.getInt("time", 10);

        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Screen Notifications");
        wl.acquire();
        try {
            Thread.sleep(time * 1000);
        }
        catch (Exception e) {}
        wl.release();
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if(event.values[0] < event.sensor.getMaximumRange()) { //Could not unregister here and then screen would turn on as soon as user took out of pocket
                ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
            } else {
                ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
                turnOnScreen(PreferenceManager.getDefaultSharedPreferences(this), (PowerManager) getSystemService(Context.POWER_SERVICE));
            }
        }
    }

}
