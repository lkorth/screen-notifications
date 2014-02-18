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

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
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
            if(notInCall() && shouldTurnOnScreen(mPrefs)) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

                if(!pm.isScreenOn()) {
                    if(mPrefs.getBoolean("proxSensor", false)) {
                        turnOnScreen(mPrefs, pm);

                        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                        sensorManager.unregisterListener(this);
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
    }

    private boolean notInCall() {
    	AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    	
    	if(manager.getMode() == AudioManager.MODE_IN_CALL || manager.getMode() == AudioManager.MODE_IN_COMMUNICATION)
    		return false;
	    else
	    	return true;
    }
    
    private boolean shouldTurnOnScreen(SharedPreferences mPrefs) {
        if(mPrefs.getBoolean("quiet", false)) {
            String startTime = mPrefs.getString("startTime", "22:00");
            String stopTime = mPrefs.getString("stopTime", "08:00");;
            boolean turnOnScreen = true;

            SimpleDateFormat sdfDate = new SimpleDateFormat("H:mm");
            String currentTimeStamp = sdfDate.format(new Date());
            int currentHour = Integer.parseInt(currentTimeStamp.split("[:]+")[0]);
            int currentMinute = Integer.parseInt(currentTimeStamp.split("[:]+")[1]);

            int startHour  = Integer.parseInt(startTime.split("[:]+")[0]);
            int startMinute = Integer.parseInt(startTime.split("[:]+")[1]);

            int stopHour = Integer.parseInt(stopTime.split("[:]+")[0]);
            int stopMinute = Integer.parseInt(stopTime.split("[:]+")[1]);

            if(startHour < stopHour && currentHour > startHour && currentHour < stopHour)
                turnOnScreen = false;
            else if (startHour > stopHour && (currentHour > startHour || currentHour < stopHour))
                turnOnScreen = false;
            else if(currentHour == startHour && currentMinute >= startMinute)
                turnOnScreen = false;
            else if(currentHour == stopHour && currentMinute < stopMinute)
                turnOnScreen = false;

            return turnOnScreen;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void turnOnScreen(SharedPreferences mPrefs, PowerManager pm) {
    	if(mPrefs.getBoolean("status-bar", false)) {
    		try {    	
                Thread.sleep(3000);
            } catch (Exception e) {
            	// ignore
            }
    	}
    	
        int time = mPrefs.getInt("time", 10);

        PowerManager.WakeLock wl;
        if(mPrefs.getBoolean("bright", false))
            wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Screen Notifications");
        else
            wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Screen Notifications");
        wl.acquire();   
        
        if(mPrefs.getBoolean("status-bar", false)) {
	        try {
	        	Object sbservice = getSystemService("statusbar");
	        	Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
	        	Method showsb = statusbarManager.getMethod("expand");
	        	showsb.invoke(sbservice);
	        } catch (Exception e) {
	        	// ignore
	        }
        }
        
        int origTimeout = -1;
        try {
			origTimeout = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
			Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 100);
		} catch (SettingNotFoundException e1) {
			// ignore
		}
        
        try {    	
            Thread.sleep(time * 1000);
        } catch (Exception e) {
        	// ignore
        } finally {
        	wl.release();
        }
        
        try {
        	Thread.sleep(200);
        } catch (Exception e) {
        	// ignore
        }
        
        if(origTimeout != -1) {
        	Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, origTimeout);
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
                close = true;
            } else {
                close = false;
            }
        }
    }

}
