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
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Service Entry Activity for recording daily deliveries.
 * Allows providers to mark deliveries and enter quantities for each customer.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Date selector for entering past/future deliveries</li>
 *   <li>Customer list with inline quantity controls</li>
 *   <li>Delivery status checkboxes</li>
 *   <li>Auto-save to Firestore</li>
 *   <li>Empty state handling</li>
 * </ul>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-09
 */
public class ServiceEntryActivity extends BaseActivity {
    
    private TextView selectedDateText;
    private Button changeDateButton;
    private RecyclerView serviceEntriesRecycler;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton addServiceEntryFab;
    
    private FirestoreRepository repository;
    private ServiceEntryAdapter adapter;
    private Date selectedDate;
    private String providerId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_entry);
        
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setupToolbar(toolbar, "Service Entry", true);
        
        initializeViews();
        initializeData();
        setupClickListeners();
        loadData();
    }
    
    private void initializeViews() {
        selectedDateText = findViewById(R.id.selectedDateText);
        changeDateButton = findViewById(R.id.changeDateButton);
        serviceEntriesRecycler = findViewById(R.id.serviceEntriesRecycler);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        addServiceEntryFab = findViewById(R.id.addServiceEntryFab);
        
        serviceEntriesRecycler.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void initializeData() {
        repository = new FirestoreRepository();
        selectedDate = new Date();
        providerId = getCurrentUserId();
        
        adapter = new ServiceEntryAdapter((customer, quantity, delivered) -> {
            saveServiceEntry(customer, quantity, delivered);
        });
        serviceEntriesRecycler.setAdapter(adapter);
        
        updateDateDisplay();
    }
    
    private void setupClickListeners() {
        changeDateButton.setOnClickListener(v -> showDatePicker());
        addServiceEntryFab.setOnClickListener(v -> loadData());
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                selectedDate = selected.getTime();
                updateDateDisplay();
                loadData();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
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
        
        // Load customers
        repository.getCustomersByProvider(providerId, new FirestoreRepository.OnCustomersLoadedListener() {
            @Override
            public void onCustomersLoaded(List<Customer> customers) {
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
    
    private void saveServiceEntry(Customer customer, double quantity, boolean delivered) {
        if (quantity <= 0) {
            // Delete entry if quantity is 0
            return;
        }
        
        Timestamp timestamp = new Timestamp(selectedDate);
        ServiceEntry entry = new ServiceEntry(
            providerId,
            customer.getId(),
            timestamp,
            quantity,
            delivered
        );
        
        repository.saveServiceEntry(entry, new FirestoreRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess() {
                // Entry saved successfully
            }

            @Override
            public void onError(String error) {
                showToast("Error saving: " + error);
            }
        });
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            serviceEntriesRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            serviceEntriesRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
}
