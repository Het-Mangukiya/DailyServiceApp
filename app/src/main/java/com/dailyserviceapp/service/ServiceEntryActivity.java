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
import com.dailyserviceapp.core.offline.OfflineCache;
import com.dailyserviceapp.core.sync.SyncWorkScheduler;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.dailyserviceapp.databinding.ActivityServiceEntryBinding;
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

    private ActivityServiceEntryBinding binding;
    
    private TextView selectedDateText;
    private TextView offlineIndicator;
    private TextView pendingSyncText;
    private com.google.android.material.card.MaterialCardView pendingSyncCard;
    private Button changeDateButton;
    private com.google.android.material.button.MaterialButton btnSelectAll;
    private com.google.android.material.button.MaterialButton btnClearAll;
    private RecyclerView serviceEntriesRecycler;
    private LinearLayout emptyStateLayout;
    private MaterialButton btnMarkDelivery;
    private android.widget.ProgressBar loadingProgress;
    
    private FirestoreRepository repository;
    private OfflineCache offlineCache;
    private ServiceEntryAdapter adapter;
    private Date selectedDate;
    private String providerId;
    private ListenerRegistration customersListener;
    private boolean isMarkingDelivery;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityServiceEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // CRITICAL: Check session first
        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }
        
        MaterialToolbar toolbar = binding.toolbar;
        setupToolbar(toolbar, "Service Entry", true);
        
        initializeViews();
        initializeData();
        setupClickListeners();
        loadData();
    }
    
    private void initializeViews() {
        selectedDateText = binding.selectedDateText;
        offlineIndicator = binding.offlineIndicator;
        pendingSyncCard = binding.pendingSyncCard;
        pendingSyncText = binding.pendingSyncText;
        changeDateButton = binding.changeDateButton;
        btnSelectAll = binding.btnSelectAll;
        btnClearAll = binding.btnClearAll;
        serviceEntriesRecycler = binding.recyclerView;
        emptyStateLayout = binding.emptyState;
        btnMarkDelivery = binding.btnMarkDelivery;
        loadingProgress = binding.loadingProgress;
        
        serviceEntriesRecycler.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void initializeData() {
        repository = new FirestoreRepository();
        offlineCache = new OfflineCache(this);
        SyncWorkScheduler.ensurePeriodicSync(this);
        selectedDate = new Date();
        providerId = getCurrentUserId();
        
        adapter = new ServiceEntryAdapter();
        serviceEntriesRecycler.setAdapter(adapter);
        
        updateDateDisplay();
    }
    
    private void setupClickListeners() {
        changeDateButton.setOnClickListener(v -> showDatePicker());
        btnMarkDelivery.setOnClickListener(v -> markDeliveries());
        
        btnSelectAll.setOnClickListener(v -> {
            int selectedCount = adapter.selectAllAvailable();
            if (selectedCount == 0) {
                showToast("No customers available to select");
            } else {
                showToast("Selected " + selectedCount + " customers");
            }
        });
        
        btnClearAll.setOnClickListener(v -> {
            adapter.clearSelection();
            showToast("Selection cleared");
        });
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
            showToast("Session expired. Please login again.");
            navigateToLogin();
            return;
        }

        updatePendingSyncIndicator(offlineCache.getPendingEntries().size());

        if (!isNetworkAvailable()) {
            // Offline mode - load from cache
            loadOfflineData();
            return;
        }
        
        // Show loading state
        showLoading(true);
        
        // Remove old listener if exists
        if (customersListener != null) {
            customersListener.remove();
        }
        
        // Listen to real-time customer updates
        customersListener = repository.listenToCustomers(providerId, new FirestoreRepository.OnCustomersLoadedListener() {
            @Override
            public void onCustomersLoaded(List<Customer> customers) {
                // Cache customers for offline access
                offlineCache.cacheCustomers(customers);
                
                // Hide offline indicator when online
                if (offlineIndicator != null) {
                    offlineIndicator.setVisibility(View.GONE);
                }
                
                if (customers == null || customers.isEmpty()) {
                    showEmptyState(true);
                    return;
                }
                
                // Filter out customers on vacation
                List<Customer> activeCustomers = new java.util.ArrayList<>();
                for (Customer customer : customers) {
                    if (!customer.isOnVacation()) {
                        activeCustomers.add(customer);
                    }
                }
                
                if (activeCustomers.isEmpty()) {
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
                            showLoading(false);
                            adapter.submitData(activeCustomers, entries);
                        }

                        @Override
                        public void onError(String error) {
                            showLoading(false);
                            showToast("Error loading entries: " + error);
                            adapter.submitData(customers, null);
                        }
                    }
                );
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showToast("Error loading customers: " + error);
                showEmptyState(true);
            }
        });
    }
    
    /**
     * Mark deliveries for all selected customers (batch operation)
     */
    private void markDeliveries() {
        List<ServiceEntryAdapter.DeliveryItem> deliveries = adapter.getSelectedDeliveries();
        
        if (deliveries.isEmpty()) {
            showToast(getString(R.string.validation_no_customers_selected));
            return;
        }
        
        // Check if offline
        if (!isNetworkAvailable()) {
            // Queue for later sync
            queueOfflineDeliveries(deliveries);
            return;
        }
        
        // Warn if modifying past entries (beyond today)
        Calendar today = Calendar.getInstance();
        Calendar selected = Calendar.getInstance();
        selected.setTime(selectedDate);
        
        boolean isPastDate = selected.get(Calendar.YEAR) < today.get(Calendar.YEAR) ||
                            (selected.get(Calendar.YEAR) == today.get(Calendar.YEAR) && 
                             selected.get(Calendar.DAY_OF_YEAR) < today.get(Calendar.DAY_OF_YEAR));
        
        if (isPastDate) {
            // Show confirmation dialog for past dates
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Past Date Entry")
                .setMessage("You are marking deliveries for a past date. This may overwrite existing entries. Continue?")
                .setPositiveButton("Continue", (dialog, which) -> performMarkDeliveries(deliveries))
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            performMarkDeliveries(deliveries);
        }
    }
    
    /**
     * Performs the delivery marking operation with transaction safety.
     * Validates input and handles errors gracefully.
     *
     * @param deliveries List of delivery items to mark, must not be null or empty
     */
    private void performMarkDeliveries(List<ServiceEntryAdapter.DeliveryItem> deliveries) {
        if (deliveries == null || deliveries.isEmpty()) {
            showToast(getString(R.string.error_no_deliveries));
            return;
        }

        if (isMarkingDelivery) {
            showToast("Please wait, delivery save is in progress");
            return;
        }

        List<FirestoreRepository.DeliveryWriteRequest> writeRequests = new java.util.ArrayList<>();
        for (ServiceEntryAdapter.DeliveryItem item : deliveries) {
            if (item == null) continue;
            writeRequests.add(new FirestoreRepository.DeliveryWriteRequest(
                item.customerId,
                item.quantity,
                item.rate,
                item.amount
            ));
        }

        if (writeRequests.isEmpty()) {
            showToast(getString(R.string.error_no_deliveries));
            return;
        }

        isMarkingDelivery = true;
        btnMarkDelivery.setEnabled(false);
        btnMarkDelivery.setText(R.string.button_saving);

        Timestamp timestamp = new Timestamp(selectedDate);
        repository.saveServiceEntriesBatchWithTransaction(
            providerId,
            timestamp,
            writeRequests,
            new FirestoreRepository.OnBatchSaveResultListener() {
                @Override
                public void onSuccess(int savedCount, int skippedCount) {
                    isMarkingDelivery = false;
                    btnMarkDelivery.setEnabled(true);
                    btnMarkDelivery.setText(R.string.button_mark_delivery);

                    if (savedCount == 0 && skippedCount > 0) {
                        showToast("Selected customers are already marked for this date");
                        loadData();
                        return;
                    }

                    if (skippedCount > 0) {
                        showToast("Saved " + savedCount + " deliveries, skipped " + skippedCount + " duplicates");
                    } else {
                        showToast(getString(R.string.success_deliveries_marked));
                    }
                    loadData();
                }

                @Override
                public void onError(String error) {
                    isMarkingDelivery = false;
                    btnMarkDelivery.setEnabled(true);
                    btnMarkDelivery.setText(R.string.button_mark_delivery);
                    showToast(getString(R.string.error_general, error));
                }
            }
        );
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
    
    private void showLoading(boolean show) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            serviceEntriesRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Remove listener when activity is paused (better than onDestroy)
        // Prevents memory leaks if activity is backgrounded
        if (customersListener != null) {
            customersListener.remove();
            customersListener = null;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning to activity
        if (customersListener == null) {
            loadData();
        }

        schedulePendingSyncIfNeeded();
    }
    
    private void loadOfflineData() {
        showLoading(true);
        
        // Show offline indicator
        if (offlineIndicator != null) {
            offlineIndicator.setVisibility(View.VISIBLE);
        }
        
        List<Customer> customers = offlineCache.getCachedCustomers();
        if (customers.isEmpty()) {
            showLoading(false);
            showEmptyState(true);
            showToast("📴 Offline - No cached data available");
            return;
        }
        
        // Filter out vacation customers
        List<Customer> activeCustomers = new java.util.ArrayList<>();
        for (Customer customer : customers) {
            if (!customer.isOnVacation()) {
                activeCustomers.add(customer);
            }
        }
        
        showLoading(false);
        if (activeCustomers.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            adapter.submitData(activeCustomers, null);
        }
        
        // Update pending sync indicator
        int pendingCount = offlineCache.getPendingEntries().size();
        updatePendingSyncIndicator(pendingCount);
        
        if (pendingCount > 0) {
            showToast("📴 Offline mode - " + pendingCount + " entries pending sync");
        } else {
            showToast("📴 Offline mode - Using cached data");
        }
    }
    
    private void queueOfflineDeliveries(List<ServiceEntryAdapter.DeliveryItem> deliveries) {
        long timestamp = selectedDate.getTime();
        
        for (ServiceEntryAdapter.DeliveryItem item : deliveries) {
            OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
                providerId, item.customerId, timestamp, item.quantity, item.amount, true
            );
            offlineCache.queuePendingEntry(entry);
        }
        
        btnMarkDelivery.setEnabled(true);
        btnMarkDelivery.setText("✓ Queued for Sync");
        btnMarkDelivery.postDelayed(() -> btnMarkDelivery.setText("Mark Delivery"), 2000);
        
        showToast("✓ " + deliveries.size() + " deliveries queued for sync");

        // Schedule reliable background sync for when network is available.
        SyncWorkScheduler.enqueueImmediateSync(this);

        // Reload to show updated state
        loadOfflineData();
    }

    private void schedulePendingSyncIfNeeded() {
        int pendingCount = offlineCache.getPendingEntries().size();
        updatePendingSyncIndicator(pendingCount);
        if (pendingCount == 0) {
            return;
        }
        SyncWorkScheduler.enqueueImmediateSync(this);
    }
    
    private void updatePendingSyncIndicator(int count) {
        if (pendingSyncCard == null || pendingSyncText == null) {
            return;
        }
        
        if (count > 0) {
            pendingSyncCard.setVisibility(View.VISIBLE);
            String text = count == 1 ? 
                "📤 1 delivery pending sync" : 
                "📤 " + count + " deliveries pending sync";
            pendingSyncText.setText(text);
        } else {
            pendingSyncCard.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Final cleanup - remove listener if still exists
        if (customersListener != null) {
            customersListener.remove();
            customersListener = null;
        }
        binding = null;
    }
}
