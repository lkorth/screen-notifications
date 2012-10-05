package com.lukekorth.screennotifications;

import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import java.util.Collections;
import java.util.List;

public class AppsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppLoader task = new AppLoader();
        task.execute();
    }

    private class AppLoader extends AsyncTask<Void, Void, Void>{

        ProgressDialog loadingDialog;
        PreferenceScreen root;

        protected void onPreExecute() {
            loadingDialog = ProgressDialog.show(AppsActivity.this, "", "Loading. Please wait...", true);
        }

        @Override
        protected Void doInBackground(Void... nothing) {
            // Root
            root = getPreferenceManager().createPreferenceScreen(AppsActivity.this);

            // Inline preferences
            PreferenceCategory inlinePrefCat = new PreferenceCategory(AppsActivity.this);
            inlinePrefCat.setTitle("Apps");
            root.addPreference(inlinePrefCat);

            final PackageManager pm = getPackageManager();
            //get a list of installed apps
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            Collections.sort(packages, new ApplicationInfo.DisplayNameComparator(pm));

            for (ApplicationInfo packageInfo : packages) {
                // Checkbox preference
                CheckBoxPreference checkboxPref = new CheckBoxPreference(AppsActivity.this);
                checkboxPref.setKey(packageInfo.packageName);
                checkboxPref.setTitle(packageInfo.loadLabel(pm));
                inlinePrefCat.addPreference(checkboxPref);
            }

            return null;
        }

        protected void onPostExecute(Void nothing) {
            setPreferenceScreen(root);
            loadingDialog.cancel();
        }

    }

}
