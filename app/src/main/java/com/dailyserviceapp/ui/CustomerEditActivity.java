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
                Toast.makeText(this, R.string.error_rate_must_be_number, Toast.LENGTH_LONG).show();
                return;
            }
            
            // Comprehensive rate validation
            if (rate <= 0) {
                Toast.makeText(this, "Rate must be greater than zero", Toast.LENGTH_LONG).show();
                return;
            }
            
            if (rate > 100000) {
                Toast.makeText(this, "Rate seems unreasonably high. Please check.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Validate max 2 decimal places
            String[] parts = rateStr.split("\\.");
            if (parts.length > 1 && parts[1].length() > 2) {
                Toast.makeText(this, "Rate can have maximum 2 decimal places", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(CustomerEditActivity.this, R.string.success_customer_updated, Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(this, R.string.success_customer_added, Toast.LENGTH_SHORT).show();
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
        // Use repository instead of direct Firestore call (maintains consistency)
        repo.getCustomer(customerId, 
            doc -> {
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
                } else {
                    Toast.makeText(this, "Customer not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            },
            e -> {
                Toast.makeText(this, R.string.error_loading_customer, Toast.LENGTH_SHORT).show();
                finish();
            }
        );
    }

    private void showDeleteConfirmation() {
        // Enhanced warning with explicit consequences
        new MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ Delete Customer?")
                .setMessage("Deleting this customer will permanently remove:\n\n" +
                        "• All delivery records\n" +
                        "• All bills and payment history\n" +
                        "• Complete transaction history\n\n" +
                        "⚠️ This action cannot be undone.\n\n" +
                        "Are you absolutely sure?")
                .setPositiveButton("Delete Permanently", (dialog, which) -> deleteCustomer())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(true)
                .show();
    }

    private void deleteCustomer() {
        if (customerId == null) return;
        
        repo.deleteCustomer(customerId, new FirestoreRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(CustomerEditActivity.this, R.string.success_customer_deleted, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CustomerEditActivity.this, getString(R.string.error_deleting_customer, error), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Remove this method - use BaseActivity's getCurrentUserId() instead
    
    private static String value(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }
}
