package com.lukekorth.screennotifications.services;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;

public class ScreenNotificationsService extends BaseAccessibilityService {

    public void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo localAccessibilityServiceInfo = new AccessibilityServiceInfo();
        localAccessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        localAccessibilityServiceInfo.feedbackType = 16;
        localAccessibilityServiceInfo.notificationTimeout = 0L;
        setServiceInfo(localAccessibilityServiceInfo);
    }
}
