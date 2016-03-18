package com.lukekorth.screennotifications.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lukekorth.screennotifications.R;
import com.lukekorth.screennotifications.helpers.AppHelper;
import com.lukekorth.screennotifications.models.RecentApp;

import io.realm.RealmResults;

public class RecentAppsAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private RealmResults<RecentApp> mApps;

    public RecentAppsAdapter(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mApps = AppHelper.getRecentNotifyingApps();
    }

    @Override
    public int getCount() {
        if (mApps.size() > 50) {
            return 50;
        } else {
            return mApps.size();
        }
    }

    @Override
    public String getItem(int position) {
        return mApps.get(position).getPackageName();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.recent_app, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.notificationTime = (TextView) convertView.findViewById(R.id.notification_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final RecentApp app = mApps.get(position);
        RecentApp.fetchInformation(app, mContext);
        if (app.isInstalled()) {
            holder.icon.setImageDrawable(app.getIcon());
            holder.name.setText(app.getName());
            holder.notificationTime.setText(DateUtils.getRelativeTimeSpanString(app.getTimestamp(), System.currentTimeMillis(), 0));
        } else {
            holder.icon.setImageResource(R.drawable.ic_launcher);
            holder.name.setText(R.string.uninstalled_app);
            holder.notificationTime.setText(DateUtils.getRelativeTimeSpanString(app.getTimestamp(), System.currentTimeMillis(), 0));
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
        TextView notificationTime;
    }
}
