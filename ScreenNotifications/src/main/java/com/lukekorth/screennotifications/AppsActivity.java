package com.lukekorth.screennotifications;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;

import com.lukekorth.screennotifications.adapters.AppAdapter;
import com.lukekorth.screennotifications.helpers.AppHelper;
import com.lukekorth.screennotifications.models.App;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import io.realm.RealmResults;

public class AppsActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.apps);

		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

        ArrayList<App> apps = new ArrayList<>();
		RealmResults<App> realmApps = AppHelper.getNotifyingApps();
		for (App app : realmApps) {
            App.fetchInformation(app, this);

			if (app.isInstalled()) {
                apps.add(app);
			}
		}

		final Collator collator = Collator.getInstance();
		Collections.sort(apps, new Comparator<App>() {
			@Override
			public int compare(App lhs, App rhs) {
				return collator.compare(lhs.getName(), rhs.getName());
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

