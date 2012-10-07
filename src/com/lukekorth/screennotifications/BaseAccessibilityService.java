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

    private boolean close;

    public void onServiceConnected() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if(proximitySensor == null || mPrefs.getBoolean("proxSensor", false))
            close = false;
        else
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

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
                    if(close == false)
                        turnOnScreen(mPrefs, pm);

                    SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                    sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
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
            if(event.values[0] < event.sensor.getMaximumRange()) {
                close = true;
            } else {
                close = false;
            }
        }
    }

}
