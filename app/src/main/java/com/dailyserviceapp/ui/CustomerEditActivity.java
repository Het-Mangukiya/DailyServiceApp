package com.dailyserviceapp.ui;

import dagger.hilt.android.AndroidEntryPoint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.databinding.ActivityCustomerEditBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.util.Locale;

@AndroidEntryPoint
public class CustomerEditActivity extends BaseActivity {

    private ActivityCustomerEditBinding binding;

    private FirestoreRepository repo;
    private String customerId; // null for ADD, non-null for EDIT
    private Customer existingCustomer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        if (!isLoggedIn()) {
            showToast("Please login first");
            finish();
            return;
        }

        repo = new FirestoreRepository();
        customerId = getIntent().getStringExtra("customerId");

        TextInputEditText nameInput = binding.nameInput;
        TextInputEditText phoneInput = binding.phoneInput;
        TextInputEditText addressInput = binding.addressInput;
        TextInputEditText serviceInput = binding.serviceInput;
        TextInputEditText rateInput = binding.rateInput;
        MaterialButton deleteButton = binding.deleteButton;

        if (customerId != null) {
            binding.topAppBar.setTitle("Edit Customer");
            deleteButton.setVisibility(View.VISIBLE);
            loadCustomerData();
        } else {
            binding.topAppBar.setTitle("Add Customer");
            deleteButton.setVisibility(View.GONE);
        }

        deleteButton.setOnClickListener(v -> showDeleteConfirmation());

        MaterialButton save = binding.saveButton;
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
                Toast.makeText(this, "Rate must be a valid number", Toast.LENGTH_LONG).show();
                return;
            }
            
            if (rate <= 0) {
                Toast.makeText(this, "Rate must be greater than zero", Toast.LENGTH_LONG).show();
                return;
            }

            if (customerId != null) {
                if (existingCustomer == null) {
                    Toast.makeText(this, "Customer data is still loading", Toast.LENGTH_SHORT).show();
                    return;
                }
                existingCustomer.setName(name);
                existingCustomer.setPhone(phone);
                existingCustomer.setAddress(address);
                existingCustomer.setServiceType(service);
                existingCustomer.setRatePerUnit(rate);
                
                repo.updateCustomer(existingCustomer, new FirestoreRepository.OnSaveCompleteListener() {
                    @Override
                    public void onSuccess() {
                        if (!isUiActive()) return;
                        Toast.makeText(CustomerEditActivity.this, "Customer updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isUiActive()) return;
                        Toast.makeText(CustomerEditActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Customer customer = new Customer(name, phone, address, service, rate, Timestamp.now());
                customer.setProviderId(getCurrentUserId());
                customer.setStatus("ACTIVE");
                repo.addCustomer(customer,
                        ref -> {
                            if (!isUiActive()) return;
                            Toast.makeText(this, "Customer added successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        e -> {
                            if (!isUiActive()) return;
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                );
            }
        });

        binding.topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void loadCustomerData() {
        repo.getCustomer(customerId, 
            doc -> {
                if (!isUiActive()) return;
                if (doc.exists()) {
                    existingCustomer = doc.toObject(Customer.class);
                    if (existingCustomer != null) {
                        existingCustomer.setId(doc.getId());
                        binding.nameInput.setText(existingCustomer.getName());
                        binding.phoneInput.setText(existingCustomer.getPhone());
                        binding.addressInput.setText(existingCustomer.getAddress());
                        binding.serviceInput.setText(existingCustomer.getServiceType());
                        binding.rateInput.setText(String.format(Locale.US, "%.2f", existingCustomer.getRatePerUnit()));
                    }
                } else {
                    Toast.makeText(this, "Customer not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            },
            e -> {
                if (!isUiActive()) return;
                Toast.makeText(this, "Error loading customer", Toast.LENGTH_SHORT).show();
                finish();
            }
        );
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Customer?")
                .setMessage("Are you sure you want to delete " + (existingCustomer != null ? existingCustomer.getName() : "this customer") + "? This action will also delete all associated history and cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCustomer())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCustomer() {
        if (customerId == null) return;
        
        repo.deleteCustomer(customerId, new FirestoreRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess() {
                if (!isUiActive()) return;
                Toast.makeText(CustomerEditActivity.this, "Customer deleted successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                if (!isUiActive()) return;
                Toast.makeText(CustomerEditActivity.this, "Error deleting customer: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }
    
    private static String value(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
