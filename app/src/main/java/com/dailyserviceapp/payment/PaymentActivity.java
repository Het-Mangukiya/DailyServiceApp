package com.dailyserviceapp.payment;

import android.os.Bundle;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;

public class PaymentActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        
        TextView placeholderText = findViewById(R.id.placeholderText);
        placeholderText.setText("Payment Module\n\nComing Soon:\n• Record payments\n• Payment history\n• Outstanding amounts\n• Multiple payment methods");
    }
}
