package com.lukekorth.screennotifications.models;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.lukekorth.screennotifications.R;

import org.slf4j.LoggerFactory;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Required;

public class RecentApp extends RealmObject {

    @Required
    private String packageName;
    private long timestamp;

    @Ignore
    private boolean informationFetched;
    @Ignore
    private boolean installed;
    @Ignore
    private String name;
    @Ignore
    private Drawable icon;

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

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isInformationFetched() {
        return informationFetched;
    }

    public void setInformationFetched(boolean informationFetched) {
        this.informationFetched = informationFetched;
    }

    public static void fetchInformation(RecentApp app, Context context) {
        if (app.isInformationFetched()) {
            return;
        }

        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(app.getPackageName(), 0);
            app.setInstalled(true);
            app.setName((String) applicationInfo.loadLabel(packageManager));
            app.setIcon(applicationInfo.loadIcon(packageManager));
        } catch (OutOfMemoryError e) {
            LoggerFactory.getLogger("App").warn("OutOfMemoryError: " + e);
            app.setIcon(context.getResources().getDrawable(R.drawable.sym_def_app_icon));
        } catch (PackageManager.NameNotFoundException e) {
            app.setInstalled(false);
        }

        app.setInformationFetched(true);
    }
}
