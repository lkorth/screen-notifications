package com.lukekorth.screennotifications.activities;

import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.lukekorth.ez_loaders.EzLoader;
import com.lukekorth.ez_loaders.EzLoaderInterface;
import com.lukekorth.screennotifications.R;
import com.lukekorth.screennotifications.adapters.AppAdapter;
import com.lukekorth.screennotifications.models.App;
import com.lukekorth.screennotifications.models.DisplayableApps;
import com.lukekorth.screennotifications.models.Section;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppsActivity extends FragmentActivity implements EzLoaderInterface<DisplayableApps> {

	private ProgressDialog mLoadingDialog;
	private AppAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.apps);

		mLoadingDialog = ProgressDialog.show(AppsActivity.this, "", getString(R.string.loading), true);
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.apps_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();

		if(mAdapter != null) {
			switch (itemId) {
				case R.id.uncheck_all_apps:
					mAdapter.uncheckAll();
					break;
				case R.id.inverse_apps:
					mAdapter.invertSelection();
					break;
			}
		}

		return true;
	}

	@Override
	public Loader<DisplayableApps> onCreateLoader(int arg0, Bundle arg1) {
		return new EzLoader<DisplayableApps>(this, "android.intent.action.PACKAGE_ADDED", this);
	}

	@Override
	public DisplayableApps loadInBackground(int id) {
		final PackageManager pm = getPackageManager();
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		Collections.sort(packages, new ApplicationInfo.DisplayNameComparator(pm));

		DisplayableApps data = new DisplayableApps();
		data.sections = new ArrayList<Section>();
		data.apps = new App[packages.size()];

		String lastSection = "";
		String currentSection;
		for(int i = 0; i < packages.size(); i++) {
			ApplicationInfo appInfo = packages.get(i);

			data.apps[i] = new App();
			data.apps[i].name = (String) appInfo.loadLabel(pm);
			data.apps[i].packageName = appInfo.packageName;

			try {
				data.apps[i].icon = appInfo.loadIcon(pm);
			} catch (OutOfMemoryError e) {
				data.apps[i].icon = getResources().getDrawable(R.drawable.sym_def_app_icon);
			}

			if(data.apps[i].name != null && data.apps[i].name.length() > 0) {
				currentSection = data.apps[i].name.substring(0, 1).toUpperCase();
				if(!lastSection.equals(currentSection)) {
					data.sections.add(new Section(i, currentSection));
					lastSection = currentSection;
				}
			}
		}

		return data;
	}

	@Override
	public void onLoadFinished(Loader<DisplayableApps> arg0, DisplayableApps data) {
		mAdapter = new AppAdapter(this, data);
		((ListView) findViewById(R.id.appsList)).setAdapter(mAdapter);

		if(mLoadingDialog.isShowing())
			mLoadingDialog.cancel();
	}

	@Override
	public void onLoaderReset(Loader<DisplayableApps> arg0) {
	}

	@Override
	public void onReleaseResources(DisplayableApps t) {
	}
}

