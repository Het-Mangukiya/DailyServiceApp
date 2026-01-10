package com.dailyserviceapp.service;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Simplified Service Entry Activity for recording daily deliveries.
 * All customers are pre-selected with default quantities.
 * Single "Mark Delivery" button for batch operations.
 * 
 * @author DailyDrop Team
 * @version 2.0
 * @since 2026-01-10
 */
public class ServiceEntryActivity extends BaseActivity {
    
    private TextView selectedDateText;
    private Button changeDateButton;
    private RecyclerView serviceEntriesRecycler;
    private LinearLayout emptyStateLayout;
    private MaterialButton btnMarkDelivery;
    
    private FirestoreRepository repository;
    private ServiceEntryAdapter adapter;
    private Date selectedDate;
    private String providerId;
    private ListenerRegistration customersListener;
    private List<Customer> cachedCustomers;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_entry);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, "Service Entry", true);
        
        initializeViews();
        initializeData();
        setupClickListeners();
        loadData();
    }
    
    private void initializeViews() {
        selectedDateText = findViewById(R.id.selectedDateText);
        changeDateButton = findViewById(R.id.changeDateButton);
        serviceEntriesRecycler = findViewById(R.id.recyclerView);
        emptyStateLayout = findViewById(R.id.emptyState);
        btnMarkDelivery = findViewById(R.id.btnMarkDelivery);
        
        serviceEntriesRecycler.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void initializeData() {
        repository = new FirestoreRepository();
        selectedDate = new Date();
        providerId = getCurrentUserId();
        
        adapter = new ServiceEntryAdapter();
        serviceEntriesRecycler.setAdapter(adapter);
        
        updateDateDisplay();
    }
    
    private void setupClickListeners() {
        changeDateButton.setOnClickListener(v -> showDatePicker());
        btnMarkDelivery.setOnClickListener(v -> markDeliveries());
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        
        // Only allow today and past dates
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                
                // Validate: no future dates
                if (selected.after(Calendar.getInstance())) {
                    showToast("Cannot mark deliveries for future dates");
                    return;
                }
                
                selectedDate = selected.getTime();
                updateDateDisplay();
                loadData();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    private void updateDateDisplay() {
        String dateStr = DateUtils.formatShortDate(selectedDate);
        Calendar today = Calendar.getInstance();
        Calendar selected = Calendar.getInstance();
        selected.setTime(selectedDate);
        
        if (today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)) {
            selectedDateText.setText("Today - " + dateStr);
        } else {
            selectedDateText.setText(dateStr);
        }
    }
    
    private void loadData() {
        if (providerId == null || providerId.isEmpty()) {
            showToast("Please login again");
            showEmptyState(true);
            return;
        }
        
        if (!isNetworkAvailable()) {
            showToast("No internet connection");
            return;
        }
        
        // Remove old listener if exists
        if (customersListener != null) {
            customersListener.remove();
        }
        
        // Listen to real-time customer updates
        customersListener = repository.listenToCustomers(providerId, new FirestoreRepository.OnCustomersLoadedListener() {
            @Override
            public void onCustomersLoaded(List<Customer> customers) {
                cachedCustomers = customers;
                
                if (customers == null || customers.isEmpty()) {
                    showEmptyState(true);
                    return;
                }
                
                showEmptyState(false);
                
                // Load service entries for selected date
                Date startOfDay = DateUtils.getStartOfDay(selectedDate);
                Date endOfDay = DateUtils.getEndOfDay(selectedDate);
                
                repository.getServiceEntriesByProviderAndDate(
                    providerId,
                    new Timestamp(startOfDay),
                    new Timestamp(endOfDay),
                    new FirestoreRepository.OnServiceEntriesLoadedListener() {
                        @Override
                        public void onServiceEntriesLoaded(List<ServiceEntry> entries) {
                            adapter.submitData(customers, entries);
                        }

                        @Override
                        public void onError(String error) {
                            showToast("Error loading entries: " + error);
                            adapter.submitData(customers, null);
                        }
                    }
                );
            }

            @Override
            public void onError(String error) {
                showToast("Error loading customers: " + error);
                showEmptyState(true);
            }
        });
    }
    
    /**
     * Mark deliveries for all selected customers (batch operation)
     */
    private void markDeliveries() {
        if (!isNetworkAvailable()) {
            showToast(getString(R.string.validation_no_internet));
            return;
        }
        
        List<ServiceEntryAdapter.DeliveryItem> deliveries = adapter.getSelectedDeliveries();
        
        if (deliveries.isEmpty()) {
            showToast(getString(R.string.validation_no_customers_selected));
            return;
        }
        
        btnMarkDelivery.setEnabled(false);
        btnMarkDelivery.setText(R.string.button_saving);
        
        Timestamp timestamp = new Timestamp(selectedDate);
        int[] successCount = {0};
        int totalCount = deliveries.size();
        
        for (ServiceEntryAdapter.DeliveryItem item : deliveries) {
            ServiceEntry entry = new ServiceEntry(
                providerId,
                item.customerId,
                timestamp,
                item.quantity,
                true  // All selected items are marked as delivered
            );
            
            // Use atomic transaction to save entry and update lent amount
            repository.saveServiceEntryWithTransaction(entry, item.customerId, item.amount,
                new FirestoreRepository.OnSaveCompleteListener() {
                @Override
                public void onSuccess() {
                    successCount[0]++;
                    if (successCount[0] == totalCount) {
                        btnMarkDelivery.setEnabled(true);
                        btnMarkDelivery.setText(R.string.button_mark_delivery);
                        showToast(getString(R.string.success_deliveries_marked));
                        loadData(); // Refresh to show saved state
                    }
                }

                @Override
                public void onError(String error) {
                    showToast(getString(R.string.error_general, error));
                    btnMarkDelivery.setEnabled(true);
                    btnMarkDelivery.setText(R.string.button_mark_delivery);
                }
            });
        }
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            serviceEntriesRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            btnMarkDelivery.setEnabled(false);
        } else {
            serviceEntriesRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            btnMarkDelivery.setEnabled(true);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove real-time listener to prevent memory leaks
        if (customersListener != null) {
            customersListener.remove();
        }
    }
}
