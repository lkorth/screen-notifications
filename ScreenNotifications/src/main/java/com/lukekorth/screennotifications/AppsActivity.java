package com.lukekorth.screennotifications;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;

import com.lukekorth.screennotifications.adapters.AppAdapter;
import com.lukekorth.screennotifications.helpers.AppHelper;
import com.lukekorth.screennotifications.models.App;

import org.slf4j.LoggerFactory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class AppsActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.apps);

		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		init();
	}

	public void init() {
		PackageManager packageManager = getPackageManager();
		HashSet<String> packages = AppHelper.getNotifyingApps(this);
		ArrayList<App> apps = new ArrayList<>();

		ApplicationInfo applicationInfo;
		App app;
		for (String packageName : packages) {
			app = new App();
			app.packageName = packageName;
			try {
				applicationInfo = packageManager.getApplicationInfo(packageName, 0);
				app.name = (String) applicationInfo.loadLabel(packageManager);
				app.icon = applicationInfo.loadIcon(packageManager);
				apps.add(app);
			} catch (OutOfMemoryError e) {
				LoggerFactory.getLogger("AppsActivity").warn("OutOfMemoryError: " + e);
				app.icon = getResources().getDrawable(R.drawable.sym_def_app_icon);
				apps.add(app);
			} catch (PackageManager.NameNotFoundException ignored) {}
		}

		final Collator collator = Collator.getInstance();
		Collections.sort(apps, new Comparator<App>() {
			@Override
			public int compare(App lhs, App rhs) {
				return collator.compare(lhs.name, rhs.name);
			}
		});
		ListView listView = (ListView) findViewById(R.id.appsList);
		listView.setAdapter(new AppAdapter(this, apps));
		listView.setEmptyView(findViewById(R.id.no_apps));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}

		return false;
	}
}

