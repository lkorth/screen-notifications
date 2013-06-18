/*
 * Copyright 2012 Luke Korth <korth.luke@gmail.com>
 * 
 * This file is part of Screen Notifications.
 * 
 * Screen Notifications is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Screen Notifications is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Screen Notifications.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lukekorth.screennotifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.lukekorth.ez_loaders.EzLoader;
import com.lukekorth.ez_loaders.EzLoaderInterface;

public class AppsActivity extends FragmentActivity implements EzLoaderInterface<Data> {
	
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
        boolean check;

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
	public Loader<Data> onCreateLoader(int arg0, Bundle arg1) {
		return new EzLoader<Data>(this, "android.intent.action.PACKAGE_ADDED", this);
	}
	
	@Override
	public Data loadInBackground(int id) {
		final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(packages, new ApplicationInfo.DisplayNameComparator(pm));
        
        Data data = new Data();
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
        		data.apps[i].icon = this.getResources().getDrawable(R.drawable.sym_def_app_icon);
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
	public void onLoadFinished(Loader<Data> arg0, Data data) {
		mAdapter = new AppAdapter(this, data);
		((ListView) findViewById(R.id.appsList)).setAdapter(mAdapter);
		
		if(mLoadingDialog.isShowing())
			mLoadingDialog.cancel();         
	}
	
	@Override
	public void onLoaderReset(Loader<Data> arg0) {		
	}

	@Override
	public void onReleaseResources(Data t) {		
	}

}

class Data {
	
	ArrayList<Section> sections;
	App[] apps;
	
}

class App {
	
	String name;
	String packageName;
	Drawable icon;
	
}

class Section {
	
	int startingIndex;
	String section;
	
	public Section(int startingIndex, String section) {
		this.startingIndex = startingIndex;
		this.section = section;
	}
	
	public String toString() {
		return section;
	}
}
