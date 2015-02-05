package com.lukekorth.screennotifications.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class ScreenNotificationsDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {}

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        ComponentName deviceAdmin = new ComponentName(context, ScreenNotificationsDeviceAdminReceiver.class);
        ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).removeActiveAdmin(deviceAdmin);

        return null;
    }

    @Override
    public void onDisabled(Context context, Intent intent) {}

    @Override
    public void onPasswordChanged(Context context, Intent intent) {}

    @Override
    public void onPasswordFailed(Context context, Intent intent) {}

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {}

    @Override
    public void onPasswordExpiring(Context context, Intent intent) {}
}
