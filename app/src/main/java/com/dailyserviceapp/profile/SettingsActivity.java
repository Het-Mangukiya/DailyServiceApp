package com.dailyserviceapp.profile;

import android.os.Bundle;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;

public class SettingsActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        TextView placeholderText = findViewById(R.id.placeholderText);
        placeholderText.setText("Settings\n\nComing Soon");
    }
}
