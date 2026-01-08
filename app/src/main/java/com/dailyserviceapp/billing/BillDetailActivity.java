package com.dailyserviceapp.billing;

import android.os.Bundle;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;

public class BillDetailActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_detail);
        
        TextView placeholderText = findViewById(R.id.placeholderText);
        placeholderText.setText("Bill Details\n\nComing Soon");
    }
}
