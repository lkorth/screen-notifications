package com.lukekorth.screennotifications;

import java.util.Collections;
import java.util.List;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class AppsActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setPreferenceScreen(createPreferenceHierarchy());
	}

	private PreferenceScreen createPreferenceHierarchy() {
		// Root
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

		// Inline preferences
		PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
		inlinePrefCat.setTitle("Apps");
		root.addPreference(inlinePrefCat);

		final PackageManager pm = getPackageManager();
		//get a list of installed apps.
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		Collections.sort(packages, new ApplicationInfo.DisplayNameComparator(pm));

		for (ApplicationInfo packageInfo : packages) {
			// Checkbox preference
			CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
			checkboxPref.setKey(packageInfo.packageName);
			checkboxPref.setTitle(packageInfo.loadLabel(pm));
			inlinePrefCat.addPreference(checkboxPref);
		}

		return root;
	}


}
