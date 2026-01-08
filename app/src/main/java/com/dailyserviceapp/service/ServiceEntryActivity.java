package com.dailyserviceapp.service;

import android.os.Bundle;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;

public class ServiceEntryActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_entry);
        
        TextView placeholderText = findViewById(R.id.placeholderText);
        placeholderText.setText("Service Entry Module\n\nComing Soon:\n• Mark daily deliveries\n• Calendar view\n• Bulk entry\n• Edit past entries");
    }
}
