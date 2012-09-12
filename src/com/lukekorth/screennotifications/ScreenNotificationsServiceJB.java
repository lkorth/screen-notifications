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

public class ScreenNotificationsServiceJB extends AccessibilityService {

	Sensor myProximitySensor;
	boolean close, sensor;

	public void onServiceConnected() {
		close = false;

		SensorManager mySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		myProximitySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		if (myProximitySensor == null) {
			sensor = false;
		} else {
			mySensorManager.registerListener(proximitySensorEventListener,
					myProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
			sensor = true;
		}
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		if(mPrefs.getBoolean((String) event.getPackageName(), false)) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

			if(sensor && mPrefs.getBoolean("proxSensor", false) == false && !close && !pm.isScreenOn()) {
				int time = mPrefs.getInt("time", 10);
				PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Screen Notifications");
				wl.acquire();
				try {
					Thread.sleep(time * 1000);
				}
				catch (Exception e) {}
				wl.release();
			}
			else if((sensor == false || mPrefs.getBoolean("proxSensor", false)) && !pm.isScreenOn()) {
				int time = mPrefs.getInt("time", 10);
				PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Screen Notifications");
				wl.acquire();
				try {
					Thread.sleep(time * 1000);
				}
				catch (Exception e) {}
				wl.release();
			}
		}
	}

	@Override
	public void onInterrupt() {}

	SensorEventListener proximitySensorEventListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
				if(event.values[0] < event.sensor.getMaximumRange())
					close = true;
				else
					close = false;
			}
		}
	};
}
