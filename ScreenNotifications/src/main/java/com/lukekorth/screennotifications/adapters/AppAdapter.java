package com.lukekorth.screennotifications.adapters;

import android.content.Context;
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

import io.realm.Realm;

public class AppAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	private ArrayList<App> mApps;
	private Realm mRealm;

	public AppAdapter(Context context, ArrayList<App> apps) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mApps = apps;
		mRealm = Realm.getDefaultInstance();
	}
	
	@Override
	public int getCount() {
		return mApps.size();
	}

	@Override
	public String getItem(int position) {
		return mApps.get(position).getName();
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
		
		holder.icon.setImageDrawable(app.getIcon());
		holder.name.setText(app.getName());

		holder.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mRealm.beginTransaction();
				app.setEnabled(isChecked);
				mRealm.commitTransaction();
			}
		});
		
		holder.selected.setChecked(app.getEnabled());
		
		return convertView;
	}
	
	static class ViewHolder {
		ImageView icon;
		TextView name;
		SwitchCompat selected;
	}
}
