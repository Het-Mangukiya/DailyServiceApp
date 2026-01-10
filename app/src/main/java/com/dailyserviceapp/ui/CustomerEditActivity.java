package com.dailyserviceapp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CustomerEditActivity extends BaseActivity {

    private FirestoreRepository repo;
    private String customerId; // null for ADD, non-null for EDIT
    private Customer existingCustomer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_edit);
        
        // CRITICAL: Check login session first
        if (!isLoggedIn()) {
            showToast("Please login first");
            finish();
            return;
        }

        repo = new FirestoreRepository();
        customerId = getIntent().getStringExtra("customerId");

        TextInputEditText nameInput = findViewById(R.id.nameInput);
        TextInputEditText phoneInput = findViewById(R.id.phoneInput);
        TextInputEditText addressInput = findViewById(R.id.addressInput);
        TextInputEditText serviceInput = findViewById(R.id.serviceInput);
        TextInputEditText rateInput = findViewById(R.id.rateInput);
        MaterialButton deleteButton = findViewById(R.id.deleteButton);

        // If editing, load customer data
        if (customerId != null) {
            setTitle("Edit Customer");
            deleteButton.setVisibility(View.VISIBLE);
            loadCustomerData(nameInput, phoneInput, addressInput, serviceInput, rateInput);
        } else {
            setTitle("Add Customer");
            deleteButton.setVisibility(View.GONE);
        }

        // Delete button click
        deleteButton.setOnClickListener(v -> showDeleteConfirmation());

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

            if (customerId != null) {
                // UPDATE existing customer
                existingCustomer.setName(name);
                existingCustomer.setPhone(phone);
                existingCustomer.setAddress(address);
                existingCustomer.setServiceType(service);
                existingCustomer.setRatePerUnit(rate);
                
                repo.updateCustomer(existingCustomer, new FirestoreRepository.OnSaveCompleteListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(CustomerEditActivity.this, "Customer updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CustomerEditActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // ADD new customer
                Customer customer = new Customer(name, phone, address, service, rate, Timestamp.now());
                customer.setProviderId(getCurrentUserId()); // CRITICAL: Set provider ID
                customer.setStatus("ACTIVE"); // Set initial status
                repo.addCustomer(customer,
                        ref -> {
                            Toast.makeText(this, "Customer added", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void loadCustomerData(TextInputEditText nameInput, TextInputEditText phoneInput, 
                                   TextInputEditText addressInput, TextInputEditText serviceInput, 
                                   TextInputEditText rateInput) {
        FirebaseFirestore.getInstance()
                .collection("customers")
                .document(customerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        existingCustomer = doc.toObject(Customer.class);
                        if (existingCustomer != null) {
                            existingCustomer.setId(doc.getId());
                            nameInput.setText(existingCustomer.getName());
                            phoneInput.setText(existingCustomer.getPhone());
                            addressInput.setText(existingCustomer.getAddress());
                            serviceInput.setText(existingCustomer.getServiceType());
                            rateInput.setText(String.valueOf(existingCustomer.getRatePerUnit()));
                        }
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Error loading customer", Toast.LENGTH_SHORT).show()
                );
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Customer")
                .setMessage("Are you sure you want to delete this customer? This will also delete all delivery records and billing history.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCustomer())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCustomer() {
        if (customerId == null) return;
        
        repo.deleteCustomer(customerId, new FirestoreRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(CustomerEditActivity.this, "Customer deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CustomerEditActivity.this, "Error deleting customer: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Remove this method - use BaseActivity's getCurrentUserId() instead
    
    private static String value(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }
}
