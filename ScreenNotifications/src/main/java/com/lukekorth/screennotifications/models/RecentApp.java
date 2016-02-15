package com.lukekorth.screennotifications.models;

import io.realm.RealmObject;
import io.realm.annotations.Required;

public class RecentApp extends RealmObject {

    @Required
    private String packageName;
    private long timestamp;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
