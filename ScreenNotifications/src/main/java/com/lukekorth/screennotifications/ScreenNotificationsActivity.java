package com.lukekorth.screennotifications;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lukekorth.screennotifications.services.AppScanningService;

public class ScreenNotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        startService(new Intent(this, AppScanningService.class));
    }
}
