package com.lukekorth.screennotifications.models;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import io.realm.RealmObject;
import io.realm.annotations.Required;
import io.realm.internal.OutOfMemoryError;

public class App extends RealmObject {

    @Required
    private String packageName;
    private String name;
    private boolean enabled;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public static Drawable getIcon(App app, PackageManager packageManager) {
        try {
            return packageManager.getApplicationInfo(app.getPackageName(), 0).loadIcon(packageManager);
        } catch (PackageManager.NameNotFoundException | NullPointerException | OutOfMemoryError e) {
            return null;
        }
    }
}
