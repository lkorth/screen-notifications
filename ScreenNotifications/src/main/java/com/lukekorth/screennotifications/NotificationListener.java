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

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService implements SensorEventListener {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        if (!sbn.isOngoing() && isAppEnabled(sbn)) {
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
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
