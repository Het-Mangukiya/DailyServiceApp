package com.dailyserviceapp.notifications;

import dagger.hilt.android.AndroidEntryPoint;
import android.os.Bundle;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;

@AndroidEntryPoint
public class NotificationListActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);
        
        TextView placeholderText = findViewById(R.id.placeholderText);
        placeholderText.setText("Notifications\n\nComing Soon:\n• Bill notifications\n• Payment reminders\n• Service updates");
    }
}
