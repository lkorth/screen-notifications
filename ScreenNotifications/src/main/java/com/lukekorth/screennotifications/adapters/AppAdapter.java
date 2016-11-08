package com.lukekorth.screennotifications.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
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

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class AppAdapter extends BaseAdapter implements RealmChangeListener<RealmResults<App>> {

    private Context mContext;
	private LayoutInflater mInflater;
    private Realm mRealm;
	private RealmResults<App> mApps;

	public AppAdapter(Context context) {
        mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mRealm = Realm.getDefaultInstance();

        getApps();
	}

    public void tearDown() {
        mRealm.close();
    }

    private void getApps() {
        mApps = mRealm.where(App.class)
                .findAllSorted("name");
        mApps.addChangeListener(this);

        notifyDataSetChanged();
    }

    @Override
    public void onChange(RealmResults<App> element) {
        getApps();
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
		if (convertView == null) {
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
		holder.icon.setImageDrawable(getIcon(app));
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

    private Drawable getIcon(App app) {
        Drawable icon = App.getIcon(app, mContext.getPackageManager());
        if (icon == null) {
            icon = mContext.getResources().getDrawable(R.drawable.sym_def_app_icon);
        }

        return icon;
    }

    private static class ViewHolder {
		ImageView icon;
		TextView name;
		SwitchCompat selected;
	}
}
