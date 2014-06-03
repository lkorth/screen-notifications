/*
 * Copyright 2012 Luke Korth <korth.luke@gmail.com>
 * 
 * This file is part of Screen Notifications.
 * 
 * Screen Notifications is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Screen Notifications is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Screen Notifications.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lukekorth.screennotifications;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;

public class BaseAccessibilityService extends AccessibilityService implements SensorEventListener {

    private boolean mListening;
    private boolean mClose;

    public void onServiceConnected() {
        handleListenerRegistration();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        new ScreenController(this, mClose).processNotification(event);
        handleListenerRegistration();
    }

    private void handleListenerRegistration() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if(proximitySensor == null ||
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("proxSensor", true)) {
            if (mListening) {
                sensorManager.unregisterListener(this);
            }
            mListening = false;
            mClose = false;
        } else {
            if (!mListening) {
                sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            mListening = true;
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if(event.values[0] < event.sensor.getMaximumRange()) {
                mClose = true;
            } else {
                mClose = false;
            }
        }
    }
}
