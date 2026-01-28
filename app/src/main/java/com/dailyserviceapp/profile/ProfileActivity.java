package com.dailyserviceapp.profile;

import android.os.Bundle;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;

public class ProfileActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // CRITICAL: Check session first
        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }
        
        TextView placeholderText = findViewById(R.id.placeholderText);
        placeholderText.setText("Profile Module\n\nComing Soon:\n• View/edit profile\n• Change password\n• Settings\n• App preferences");
    }
}
