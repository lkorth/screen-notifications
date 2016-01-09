package com.lukekorth.screennotifications.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
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

import com.lukekorth.screennotifications.R;
import com.lukekorth.screennotifications.models.App;
import com.lukekorth.screennotifications.models.DisplayableApps;
import com.lukekorth.screennotifications.models.Section;

import java.util.ArrayList;

public class AppAdapter extends BaseAdapter implements SectionIndexer {
	
	private LayoutInflater mInflater;
	private SharedPreferences mPrefs;
	private App[] apps;
	private ArrayList<Section> sections;
	
	public AppAdapter(Context context, DisplayableApps data) {
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
		
		editor.apply();
		notifyDataSetChanged();
	}
	
	public void invertSelection() {
		Editor editor = mPrefs.edit();
		
		for(App app : apps) {
			editor.putBoolean(app.packageName, !mPrefs.getBoolean(app.packageName, true));
		}
		
		editor.apply();
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
        if (!TextUtils.isEmpty(apps[position].name))
		    return sections.indexOf(new Section(0, apps[position].name.substring(0, 1).toUpperCase()));

        if (position == 0)
            return 0;
        else
            return sections.size();
	}

	@Override
	public Object[] getSections() {		
		return sections.toArray();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		
		if(convertView == null) {
			convertView = mInflater.inflate(R.layout.app, null);
			holder = new ViewHolder();
			holder.container = convertView;
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
		holder.container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = holder.selected.isChecked();
				holder.selected.setChecked(!checked);
				mPrefs.edit().putBoolean(packageName, !checked).apply();
			}
		});
		holder.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPrefs.edit().putBoolean(packageName, isChecked).apply();
			}
		});
		
		holder.selected.setChecked(mPrefs.getBoolean(apps[position].packageName, false));
		
		return convertView;
	}
	
	static class ViewHolder {
		View container;
		ImageView icon;
		TextView name;
		CheckBox selected;
	}

}
