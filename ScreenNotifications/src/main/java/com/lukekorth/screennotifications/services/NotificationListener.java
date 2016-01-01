package com.lukekorth.screennotifications.services;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.Display;

import com.lukekorth.screennotifications.helpers.ScreenController;

import org.slf4j.LoggerFactory;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService implements SensorEventListener {

    private static final String TAG = "NotificationListener";

    private double mAccel = SensorManager.GRAVITY_EARTH;

    private float accelerometerThreshold = 2.0f;
    private int maximumPoolingTime = 5;

    private SharedPreferences mPrefs;
    private PowerManager mPowerManager;
    private DisplayManager dm;

    private long startTime;
    private long endTime;

    private boolean inPocket=false;
    private boolean deviceHasMoved=false;

    private boolean isAlreadyPooling=false;

    private boolean notificationTriggersVibration = false;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: notificationListener");
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        dm = (DisplayManager) this.getSystemService(Context.DISPLAY_SERVICE);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if(!isScreenOn()) {
            //Used here, so that changes in the shared preferences are immediately reflected into the service
            accelerometerThreshold = Float.parseFloat(mPrefs.getString("accelerometerThreshold", "2.0f"));
            maximumPoolingTime = Integer.parseInt(mPrefs.getString("maximumPoolingTime", "5"));

            Log.d(TAG, "onNotificationPosted: Got a notification for an app. " + sbn.getPackageName());
            if (!sbn.isOngoing() && isAppEnabled(sbn)) {
                //used to check if phone vibrates because of the notification and then change the sensor listening later
                notificationTriggersVibration = (sbn.getNotification().defaults != 0 || sbn.getNotification().vibrate != null);

                if (isAlreadyPooling) {
                    startTime = SystemClock.elapsedRealtime();
                    endTime = SystemClock.elapsedRealtime();
                } else {
                    LoggerFactory.getLogger("NotificationListener").debug("Got a non-ongoing notification for an enabled app. " + sbn.getPackageName());

                    if (isProximitySensorEnabled()) {
                        if (!registerSensorListeners()) {
                            Log.d(TAG, "registerSensorListeners was false");
                            new ScreenController(this, true).handleNotification();
                        }
                    } else {
                        Log.d(TAG, "ProximitySensor NOT Enabled");
                        new ScreenController(this, true).handleNotification();
                    }
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        endTime = SystemClock.elapsedRealtime();
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            inPocket = event.values[0] < event.sensor.getMaximumRange();
        }

        //Currently the sensor is only used after some time if the phone vibrates, because otherwise the vibration of a notification might give us false data. Maybe use low-pass filter or other sensors to distinguish between vibration caused movement or real movement of the device
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && (!notificationTriggersVibration || (endTime - startTime)/1000f > 1.5)) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            mAccel = Math.sqrt(x*x + y*y + z*z);
            Log.d(TAG, "mAccel:"+mAccel);

            deviceHasMoved = (mAccel > accelerometerThreshold);
        }

        Log.d(TAG, "onSensorChanged: shouldSensorsWakeUpDevice:" + (!inPocket&&deviceHasMoved)+ " time:"+(endTime - startTime)/1000);
        if ((!inPocket&&deviceHasMoved) || (endTime - startTime)/1000 > maximumPoolingTime) {
            unregisterSensorListeners();
            new ScreenController(this, (!inPocket&&deviceHasMoved)).handleNotification();
            inPocket=false;
            deviceHasMoved=false;
            isAlreadyPooling=false;
            notificationTriggersVibration=false;
        }
    }

    private boolean isAppEnabled(StatusBarNotification sbn) {
        return mPrefs.getBoolean(sbn.getPackageName(), false);
    }

    private boolean isProximitySensorEnabled() {
        return !mPrefs.getBoolean("proxSensor", true);
    }

    private boolean registerSensorListeners() {
        startTime = SystemClock.elapsedRealtime();
        isAlreadyPooling=true;
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        if (proximitySensor == null) {
            return false;
        } else {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        return true;
    }

    private void unregisterSensorListeners() {
        ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).unregisterListener(this);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        //TODO: Allow option to have sensors active until notification was read. This consumes more battery, but then whenever he moves the device it will wake up, makes only sense for accelerometer mode
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public boolean isScreenOn() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            return mPowerManager.isScreenOn();
        }
    }
}
