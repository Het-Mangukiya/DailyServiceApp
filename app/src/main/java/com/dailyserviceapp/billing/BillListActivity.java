package com.dailyserviceapp.billing;

import android.os.Bundle;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;

public class BillListActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_list);
        
        TextView placeholderText = findViewById(R.id.placeholderText);
        placeholderText.setText("Billing Module\n\nComing Soon:\n• View all bills\n• Generate monthly bills\n• PDF invoices\n• Bill details & breakdowns");
    }
}
