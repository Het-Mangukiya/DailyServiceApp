package com.dailyserviceapp.reports;

import android.os.Bundle;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;

public class ReportsActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        
        // CRITICAL: Check session first
        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }
        
        TextView placeholderText = findViewById(R.id.placeholderText);
        placeholderText.setText("Reports Module\n\nComing Soon:\n• Revenue reports\n• Payment summaries\n• Service statistics\n• Charts & graphs\n• Export to PDF/CSV");
    }
}
