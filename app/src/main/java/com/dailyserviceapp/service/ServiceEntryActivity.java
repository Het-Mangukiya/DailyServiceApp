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
    private TextView offlineIndicator;
    private TextView pendingSyncText;
    private com.google.android.material.card.MaterialCardView pendingSyncCard;
    private Button changeDateButton;
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
    private List<Customer> cachedCustomers;
    
    /**
     * Initializes the activity: verifies user session, sets up the UI and toolbar, binds views and data,
     * registers click handlers, and begins loading customer and service-entry data.
     *
     * <p>If no user session is active, shows a login prompt and navigates to the login screen
     * without completing initialization.</p>
     *
     * @param savedInstanceState state bundle supplied by the system when recreating the activity; may be null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_entry);
        
        // CRITICAL: Check session first
        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, "Service Entry", true);
        
        initializeViews();
        initializeData();
        setupClickListeners();
        loadData();
    }
    
    /**
     * Binds activity view elements to their fields and configures the RecyclerView layout.
     *
     * Initializes references for the selected date display, offline indicator, pending-sync UI, change-date button,
     * service entries RecyclerView, empty-state layout, mark-delivery button, and loading progress view, then
     * assigns a LinearLayoutManager to the RecyclerView.
     */
    private void initializeViews() {
        selectedDateText = findViewById(R.id.selectedDateText);
        offlineIndicator = findViewById(R.id.offlineIndicator);
        pendingSyncCard = findViewById(R.id.pendingSyncCard);
        pendingSyncText = findViewById(R.id.pendingSyncText);
        changeDateButton = findViewById(R.id.changeDateButton);
        serviceEntriesRecycler = findViewById(R.id.recyclerView);
        emptyStateLayout = findViewById(R.id.emptyState);
        btnMarkDelivery = findViewById(R.id.btnMarkDelivery);
        loadingProgress = findViewById(R.id.loadingProgress);
        
        serviceEntriesRecycler.setLayoutManager(new LinearLayoutManager(this));
    }
    
    /**
     * Initializes runtime data and UI bindings required by the activity.
     *
     * Sets up the Firestore repository and offline cache, initializes the selected date and provider ID,
     * creates and attaches the ServiceEntryAdapter to the RecyclerView, and updates the displayed date.
     */
    private void initializeData() {
        repository = new FirestoreRepository();
        offlineCache = new OfflineCache(this);
        selectedDate = new Date();
        providerId = getCurrentUserId();
        
        adapter = new ServiceEntryAdapter();
        serviceEntriesRecycler.setAdapter(adapter);
        
        updateDateDisplay();
    }
    
    /**
     * Attaches click handlers to the activity's date-change and mark-delivery buttons.
     *
     * When the date-change button is tapped, a date picker is shown. When the mark-delivery
     * button is tapped, the batch delivery flow is started.
     */
    private void setupClickListeners() {
        changeDateButton.setOnClickListener(v -> showDatePicker());
        btnMarkDelivery.setOnClickListener(v -> markDeliveries());
    }
    
    /**
     * Presents a date picker restricted to today and earlier, allowing the user to choose the selectedDate.
     *
     * When the user picks a valid date (today or earlier) the activity's selectedDate is updated, the date display refreshed, and data reloaded. If the user attempts to pick a future date, a toast is shown and the selection is ignored.
     */
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
    
    /**
     * Loads customer data and corresponding service entries for the currently selected date,
     * using an online real-time listener when network is available or falling back to the offline cache.
     *
     * If the session is invalid it navigates to login. When online this method shows a loading state,
     * attaches a customers listener, caches customers for offline use, filters out customers on vacation,
     * and fetches service entries for the selected date before submitting results to the adapter.
     * On network absence it delegates to offline loading. Errors produce user-facing toasts and appropriate
     * empty-state UI updates.
     */
    private void loadData() {
        if (providerId == null || providerId.isEmpty()) {
            showToast("Session expired. Please login again.");
            navigateToLogin();
            return;
        }
        
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
                cachedCustomers = customers;
                
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
         * Initiates batch marking of deliveries for the currently selected customers.
         *
         * If no customers are selected, shows a validation toast and returns.
         * If the device is offline, queues the selected deliveries for later sync and returns.
         * If the selected date is before today, presents a confirmation dialog warning that existing entries may be overwritten; on confirmation proceeds to save, otherwise aborts.
         * If the selected date is today or in the future, proceeds to mark and save the deliveries immediately.
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
     * Marks the provided delivery items as delivered and persists each entry using a transactional save.
     *
     * Displays an error toast and returns if the list is null or empty. While saving, the mark button is disabled
     * and shows a saving label; on complete (all entries saved) the button is restored, a success toast is shown,
     * and the activity reloads data. Individual save failures display an error toast and restore the button.
     *
     * @param deliveries list of delivery items to mark; must not be null or empty
     */
    private void performMarkDeliveries(List<ServiceEntryAdapter.DeliveryItem> deliveries) {
        if (deliveries == null || deliveries.isEmpty()) {
            showToast(getString(R.string.error_no_deliveries));
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
            entry.setRate(item.rate);  // Set the rate for earnings calculation
            
            // Use atomic transaction to save entry and update lent amount
            repository.saveServiceEntryWithTransaction(entry, item.customerId, item.amount,
                new FirestoreRepository.OnSaveCompleteListener() {
                /**
                 * Handles a successful save of a single delivery and finalizes UI state when all saves complete.
                 *
                 * Increments the success counter; when the number of successful saves reaches the total expected,
                 * re-enables the mark-delivery button, restores its label, shows a success toast, and reloads data
                 * to reflect the saved entries.
                 */
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

                /**
                 * Handles a save error by notifying the user and restoring the mark-delivery button state.
                 *
                 * @param error the error message to display to the user
                 */
                @Override
                public void onError(String error) {
                    showToast(getString(R.string.error_general, error));
                    btnMarkDelivery.setEnabled(true);
                    btnMarkDelivery.setText(R.string.button_mark_delivery);
                }
            });
        }
    }
    
    /**
     * Toggle the activity's empty-state UI and enable/disable the delivery action accordingly.
     *
     * @param show true to display the empty-state layout and disable the mark-delivery button; false to show the recycler list and enable the button
     */
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
    
    /**
     * Toggles the loading indicator; when shown, hides the entries list and empty-state view.
     *
     * @param show true to display the loading progress and hide content views, false to hide the loading progress
     */
    private void showLoading(boolean show) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            serviceEntriesRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * Cleans up the active customer listener when the activity is paused to prevent memory leaks.
     *
     * If a customersListener is registered, this removes it and clears the reference; then the method
     * delegates to the superclass pause implementation.
     */
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
    
    /**
     * Refreshes activity state on resume and triggers syncing of any queued offline entries when possible.
     *
     * <p>If there is no active customer listener, reloads customer and service-entry data. If the device
     * is online and the offline cache contains pending entries, initiates a sync of those entries.</p>
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning to activity
        if (customersListener == null) {
            loadData();
        }
        
        // Sync pending offline entries if connected
        if (isNetworkAvailable() && offlineCache.hasPendingEntries()) {
            syncPendingEntries();
        }
    }
    
    /**
     * Loads customer data from the offline cache and updates the UI to reflect cached results and pending sync state.
     *
     * <p>Retrieves cached customers, filters out those marked on vacation, and submits the active list to the adapter
     * (or shows the empty state if none). Also displays the offline indicator, toggles loading/empty views,
     * updates the pending-sync indicator, and shows a toast describing offline status and pending entry count.</p>
     */
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
    
    /**
     * Queue the given delivery items in the offline cache so they will be synchronized later.
     *
     * This method converts each delivery item into a PendingServiceEntry and enqueues it in the OfflineCache,
     * updates the mark-delivery button state and label to indicate successful queuing, shows a toast with
     * the number of queued deliveries, and reloads offline data to reflect the updated state.
     *
     * @param deliveries the delivery items to queue for later sync
     */
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
        
        // Reload to show updated state
        loadOfflineData();
    }
    
    /**
     * Synchronizes pending offline service entries to the remote backend.
     *
     * Attempts to save every entry queued in the offline cache using transactional saves.
     * While syncing, a persistent snackbar is shown. When all entries succeed the pending
     * cache is cleared, the pending-sync indicator is updated to zero, a success toast is shown,
     * and data is reloaded. If any save fails, the snackbar is dismissed and an error toast is shown.
     */
    private void syncPendingEntries() {
        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        if (pending.isEmpty()) {
            return;
        }
        
        // Show persistent snackbar during sync
        com.google.android.material.snackbar.Snackbar syncSnackbar = 
            com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                "🔄 Syncing " + pending.size() + " offline entries...",
                com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
            );
        syncSnackbar.show();
        
        int[] syncedCount = {0};
        int totalCount = pending.size();
        
        for (OfflineCache.PendingServiceEntry pendingEntry : pending) {
            ServiceEntry entry = pendingEntry.toServiceEntry();
            
            repository.saveServiceEntryWithTransaction(entry, pendingEntry.customerId, pendingEntry.amount,
                new FirestoreRepository.OnSaveCompleteListener() {
                    @Override
                    public void onSuccess() {
                        syncedCount[0]++;
                        if (syncedCount[0] == totalCount) {
                            offlineCache.clearPendingEntries();
                            syncSnackbar.dismiss();
                            updatePendingSyncIndicator(0);
                            showToast("✓ All " + totalCount + " entries synced successfully");
                            loadData(); // Reload fresh data
                        }
                    }

                    @Override
                    public void onError(String error) {
                        syncSnackbar.dismiss();
                        showToast("⚠️ Sync failed: " + error);
                    }
                }
            );
        }
    }
    
    /**
     * Update the pending-sync UI indicator to reflect how many deliveries are queued for synchronization.
     *
     * If either UI element is not initialized, the method returns without making changes.
     *
     * @param count the number of pending deliveries awaiting sync; when greater than zero the indicator is shown with a localized message, otherwise the indicator is hidden
     */
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
    
    /**
     * Cleans up activity resources before destruction.
     *
     * Removes the active customers listener, if present, and clears its reference to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Final cleanup - remove listener if still exists
        if (customersListener != null) {
            customersListener.remove();
            customersListener = null;
        }
    }
}