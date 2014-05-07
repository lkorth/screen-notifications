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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class AppAdapter extends BaseAdapter implements SectionIndexer {
	
	private LayoutInflater mInflater;
	private SharedPreferences mPrefs;
	private App[] apps;
	private ArrayList<Section> sections;
	
	public AppAdapter(Context context, Data data) {		
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		this.apps = data.apps;
		this.sections = data.sections;
	}
	
	public void uncheckAll() {
		Editor editor = mPrefs.edit();
		
		for(App app : apps) {
			editor.putBoolean(app.packageName, false);
		}
		
		editor.commit();
		notifyDataSetChanged();
	}
	
	public void invertSelection() {
		Editor editor = mPrefs.edit();
		
		for(App app : apps) {
			editor.putBoolean(app.packageName, !mPrefs.getBoolean(app.packageName, true));
		}
		
		editor.commit();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return apps.length;
	}

	@Override
	public String getItem(int position) {
		return apps[position].name;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getPositionForSection(int section) {		
		return sections.get(section).startingIndex;
	}

	@Override
	public int getSectionForPosition(int position) {
		return sections.indexOf(new Section(0, apps[position].name.substring(0, 1).toUpperCase()));
	}

	@Override
	public Object[] getSections() {		
		return sections.toArray();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if(convertView == null) {
			convertView = mInflater.inflate(R.layout.app, null);
			holder = new ViewHolder();
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.selected = (CheckBox) convertView.findViewById(R.id.selected);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.icon.setImageDrawable(apps[position].icon);
		holder.name.setText(apps[position].name);

		final String packageName = apps[position].packageName;
		holder.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPrefs.edit().putBoolean(packageName, isChecked).commit();
			}
		});
		
		holder.selected.setChecked(mPrefs.getBoolean(apps[position].packageName, false));
		
		return convertView;
	}
	
	static class ViewHolder {
		ImageView icon;
		TextView name;
		CheckBox selected;
	}

}
