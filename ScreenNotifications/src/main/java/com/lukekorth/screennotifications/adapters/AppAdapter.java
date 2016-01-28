package com.lukekorth.screennotifications.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.lukekorth.screennotifications.R;
import com.lukekorth.screennotifications.models.App;

import java.util.ArrayList;

public class AppAdapter extends BaseAdapter {
	
	private LayoutInflater mInflater;
	private SharedPreferences mPrefs;
	private ArrayList<App> mApps;

	public AppAdapter(Context context, ArrayList<App> apps) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mApps = apps;
	}
	
	@Override
	public int getCount() {
		return mApps.size();
	}

	@Override
	public String getItem(int position) {
		return mApps.get(position).name;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		if(convertView == null) {
			convertView = mInflater.inflate(R.layout.app, null);
			holder = new ViewHolder();
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.selected = (SwitchCompat) convertView.findViewById(R.id.selected);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final App app = mApps.get(position);
		
		holder.icon.setImageDrawable(app.icon);
		holder.name.setText(app.name);

		holder.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPrefs.edit().putBoolean(app.packageName, isChecked).apply();
			}
		});
		
		holder.selected.setChecked(mPrefs.getBoolean(app.packageName, true));
		
		return convertView;
	}
	
	static class ViewHolder {
		ImageView icon;
		TextView name;
		SwitchCompat selected;
	}
}
