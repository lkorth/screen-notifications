package com.lukekorth.screennotifications;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class ScreenNotificationsActivity extends PreferenceActivity {
	
	private boolean active;
	private Preference service;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
       
        service = (Preference) findPreference("service");
        service.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        	public boolean onPreferenceClick(Preference preference) {
        		if(active) 
        			showDialog("You will be now taken to the Accessibility settings. If you wish to stop Screen Notificaitons, simply turn it off.");
        		else
        			showDialog("Clicking ok will take you to the Accessibility settings. For this app to work you will need to turn on Screen Notifications. Android will warn you that the app may capture your private data, this app in no way captures any data and has no access to the internet.");
        			
				return true;
			}
        });
	}
	
	public void onResume() {
		super.onResume();
		active = isMyServiceRunning();
        if(active) {
        	service.setTitle("Screen Notifications active");
            service.setSummary("Click here to deactivate");
        }
        else {
        	service.setTitle("Screen Notifications inactive");
            service.setSummary("Click here to activate");
        }
	}
	
	private void showDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface alertDialog, int id) {
				alertDialog.cancel();
				startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (ScreenNotificationsService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
}
