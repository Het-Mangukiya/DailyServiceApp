package com.dailyserviceapp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dailyserviceapp.R;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

public class CustomerEditActivity extends AppCompatActivity {

    private FirestoreRepository repo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_edit);

        repo = new FirestoreRepository();

        TextInputEditText nameInput = findViewById(R.id.nameInput);
        TextInputEditText phoneInput = findViewById(R.id.phoneInput);
        TextInputEditText addressInput = findViewById(R.id.addressInput);
        TextInputEditText serviceInput = findViewById(R.id.serviceInput);
        TextInputEditText rateInput = findViewById(R.id.rateInput);

        MaterialButton save = findViewById(R.id.saveButton);
        save.setOnClickListener(v -> {
            String name = value(nameInput);
            String phone = value(phoneInput);
            String address = value(addressInput);
            String service = value(serviceInput);
            String rateStr = value(rateInput);

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_LONG).show();
                return;
            }
            if (TextUtils.isEmpty(rateStr)) {
                Toast.makeText(this, "Rate is required", Toast.LENGTH_LONG).show();
                return;
            }

            double rate;
            try {
                rate = Double.parseDouble(rateStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Rate must be a number", Toast.LENGTH_LONG).show();
                return;
            }

            Customer customer = new Customer(name, phone, address, service, rate, Timestamp.now());
            repo.addCustomer(customer,
                    ref -> {
                        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });
    }

    private static String value(TextInputEditText editText) {
        if (editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }
}
