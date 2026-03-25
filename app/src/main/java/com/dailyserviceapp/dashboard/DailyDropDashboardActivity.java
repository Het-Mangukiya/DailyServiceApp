package com.dailyserviceapp.dashboard;

import android.content.Intent;
import android.os.Bundle;

import com.dailyserviceapp.core.base.BaseActivity;

/**
 * Temporary compatibility entry point.
 * Keeps old routes safe by forwarding any new-dashboard intents back to the legacy provider UI.
 */
public class DailyDropDashboardActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, ProviderDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
