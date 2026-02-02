package com.dailyserviceapp.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dailyserviceapp.R;
import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.billing.BillListActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.offline.OfflineCache;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.payment.PaymentActivity;
import com.dailyserviceapp.profile.ProfileActivity;
import com.dailyserviceapp.reports.ReportsActivity;
import com.dailyserviceapp.service.ServiceEntryActivity;
import com.dailyserviceapp.ui.CustomerAdapter;
import com.dailyserviceapp.ui.CustomerEditActivity;
import com.dailyserviceapp.utils.TestDataGenerator;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Main Dashboard Activity - Landing page with customer list and analytics
 * 
 * Features:
 * - Navigation drawer with menu options
 * - Customer list with search functionality
 * - Analytics cards (total customers, monthly revenue)
 * - Add customer via FAB (manual or QR code)
 */
public class DashboardActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private TextView totalCustomersCount, totalRevenueAmount;
    private TextView txtTodayDelivered, txtTodayAmount;
    private com.google.android.material.chip.Chip syncStatusChip;
    private RecyclerView customerRecyclerView;
    private CustomerAdapter customerAdapter;
    private LinearLayout emptyState;
    private EditText searchEditText;
    private ExtendedFloatingActionButton addCustomerFab;
    private com.google.android.material.button.MaterialButton sortButton;
    
    private FirebaseFirestore firestore;
    private OfflineCache offlineCache;
    private GoogleSignInClient googleSignInClient;
    private String providerId;
    private List<Customer> allCustomers = new ArrayList<>();
    private List<Customer> filteredCustomers = new ArrayList<>();
    private ListenerRegistration customersListener;
    
    // Search debouncing
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY_MS = 300;
    
    // Sorting
    private enum SortOrder { NAME, SERVICE_TYPE, ADDRESS }
    private SortOrder currentSortOrder = SortOrder.NAME;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        providerId = getCurrentUserId();
        firestore = FirebaseFirestore.getInstance();
        offlineCache = new OfflineCache(this);
        
        // Initialize Google Sign-In client for logout
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        initializeViews();
        setupNavigationDrawer();
        setupRecyclerView();
        setupListeners();
        loadData();
    }
    
    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.topAppBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        totalCustomersCount = findViewById(R.id.txtCustomerCount);
        totalRevenueAmount = findViewById(R.id.txtTotalValue);
        txtTodayDelivered = findViewById(R.id.txtTodayDelivered);
        txtTodayAmount = findViewById(R.id.txtTodayAmount);
        syncStatusChip = findViewById(R.id.syncStatusChip);
        customerRecyclerView = findViewById(R.id.customerRecyclerView);
        emptyState = findViewById(R.id.emptyStateLayout);
        searchEditText = findViewById(R.id.edtSearch);
        addCustomerFab = findViewById(R.id.fabAddCustomer);
        // sortButton will be set from menu in onCreateOptionsMenu
        
        setSupportActionBar(toolbar);
        
        // Setup swipe-to-refresh (if available in layout)
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                R.color.md_theme_primary,
                R.color.md_theme_secondary,
                R.color.md_theme_tertiary
            );
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadData();
            });
        }
    }
    
    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        navigationView.setNavigationItemSelectedListener(this);
        
        // Set user info in navigation header
        View headerView = navigationView.getHeaderView(0);
        TextView userName = headerView.findViewById(R.id.userName);
        TextView userEmail = headerView.findViewById(R.id.userEmail);
        TextView userInitial = headerView.findViewById(R.id.userInitial);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            
            userName.setText(name != null ? name : getString(R.string.default_user_name));
            userEmail.setText(email != null ? email : getString(R.string.default_user_email));
            
            if (name != null && !name.isEmpty()) {
                userInitial.setText(name.substring(0, 1).toUpperCase());
            }
        }
    }
    
    private void setupRecyclerView() {
        customerAdapter = new CustomerAdapter(customer -> {
            // Show options dialog: View Details or Toggle Vacation
            showCustomerOptions(customer);
        });
        customerAdapter.submit(filteredCustomers);
        customerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        customerRecyclerView.setAdapter(customerAdapter);
    }
    
    private void setupListeners() {
        addCustomerFab.setOnClickListener(v -> {
            startActivity(new Intent(this, CustomerEditActivity.class));
        });
        
        // Sort button is handled via menu onOptionsItemSelected
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remove pending search callbacks
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // Post new search with 300ms delay
                searchRunnable = () -> filterCustomers(s.toString());
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void loadData() {
        if (providerId == null || providerId.isEmpty()) {
            return;
        }
        
        // Analytics will be loaded after customers finish loading
        loadCustomers();
    }
    
    private void loadCustomers() {
        // Remove old listener if exists
        if (customersListener != null) {
            customersListener.remove();
        }
        
        // Listen to real-time customer updates
        customersListener = firestore.collection("customers")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("status", "ACTIVE")
            .addSnapshotListener((queryDocumentSnapshots, error) -> {
                if (error != null) {
                    showToast("Failed to load customers: " + error.getMessage());
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    return;
                }
                
                if (queryDocumentSnapshots != null) {
                    allCustomers.clear();
                    filteredCustomers.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Customer customer = document.toObject(Customer.class);
                        customer.setId(document.getId());
                        allCustomers.add(customer);
                        filteredCustomers.add(customer);
                    }
                    
                    // Cache for offline access
                    offlineCache.cacheCustomers(allCustomers);
                    
                    // Update adapter using submit method
                    customerAdapter.submit(filteredCustomers);
                    updateEmptyState();
                    
                    // Load analytics after customers are loaded to avoid race condition
                    loadAnalytics();
                    
                    // Stop refresh animation
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
    }
    
    private void loadAnalytics() {
        // Load total customers
        totalCustomersCount.setText(String.valueOf(allCustomers.size()));
        
        // Calculate current month revenue from deliveries
        calculateCurrentMonthRevenue();
        
        // Calculate today's deliveries
        calculateTodaysSummary();
    }
    
    private void calculateTodaysSummary() {
        if (allCustomers.isEmpty()) {
            txtTodayDelivered.setText(getString(R.string.delivered_format, 0, 0));
            txtTodayAmount.setText(CurrencyUtils.formatIndianCurrency(0.0));
            return;
        }
        
        // Get today's date range
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);
        
        java.util.Calendar todayEnd = (java.util.Calendar) today.clone();
        todayEnd.set(java.util.Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(java.util.Calendar.MINUTE, 59);
        todayEnd.set(java.util.Calendar.SECOND, 59);
        todayEnd.set(java.util.Calendar.MILLISECOND, 999);
        
        Timestamp startOfDay = new Timestamp(today.getTime());
        Timestamp endOfDay = new Timestamp(todayEnd.getTime());
        
        final int totalCustomers = allCustomers.size();
        
        // Query service_entries collection for today's deliveries
        firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("delivered", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int deliveredCount = 0;
                double todayEarnings = 0.0;
                
                // Filter by today's date in memory
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                    Timestamp entryDate = doc.getTimestamp("date");
                    if (entryDate != null) {
                        long entryTime = entryDate.toDate().getTime();
                        long startTime = startOfDay.toDate().getTime();
                        long endTime = endOfDay.toDate().getTime();
                        
                        if (entryTime >= startTime && entryTime <= endTime) {
                            deliveredCount++;
                            
                            // Calculate earnings (rate × quantity)
                            Double rate = doc.getDouble("rate");
                            Double quantity = doc.getDouble("quantity");
                            
                            if (rate != null && quantity != null) {
                                todayEarnings += (rate * quantity);
                            }
                        }
                    }
                }
                
                // Update UI
                int finalDeliveredCount = deliveredCount;
                double finalTodayEarnings = todayEarnings;
                runOnUiThread(() -> {
                    txtTodayDelivered.setText(getString(R.string.delivered_format, 
                        finalDeliveredCount, totalCustomers));
                    txtTodayAmount.setText(CurrencyUtils.formatIndianCurrency(finalTodayEarnings));
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("Dashboard", "Error loading today's summary: " + e.getMessage());
                runOnUiThread(() -> {
                    txtTodayDelivered.setText(getString(R.string.delivered_format, 0, totalCustomers));
                    txtTodayAmount.setText(CurrencyUtils.formatIndianCurrency(0.0));
                });
            });
    }
    
    private void calculateCurrentMonthRevenue() {
        // Reset counter
        totalRevenueAmount.setText("₹0");
        
        if (allCustomers.isEmpty()) {
            android.util.Log.d("DashboardActivity", "No customers loaded yet, skipping revenue calculation");
            return;
        }
        
        // Get current month start date
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        long startOfMonthMillis = calendar.getTimeInMillis();
        
        android.util.Log.d("DashboardActivity", "Calculating revenue for providerId: " + providerId + ", customers count: " + allCustomers.size());
        
        // Query all service entries for this provider (without date filter to avoid index requirement)
        firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .get()
            .addOnSuccessListener(querySnapshots -> {
                android.util.Log.d("DashboardActivity", "Found " + querySnapshots.size() + " total service entries");
                double totalRevenue = 0;
                int currentMonthCount = 0;
                
                for (QueryDocumentSnapshot doc : querySnapshots) {
                    // Filter by date in memory
                    Timestamp entryDate = doc.getTimestamp("date");
                    if (entryDate != null && entryDate.toDate().getTime() >= startOfMonthMillis) {
                        currentMonthCount++;
                        String customerId = doc.getString("customerId");
                        Double quantityObj = doc.getDouble("quantity");
                        double quantity = quantityObj != null ? quantityObj : 0;
                        
                        // Find customer to get rate
                        for (Customer customer : allCustomers) {
                            if (customer.getId().equals(customerId)) {
                                double rate = customer.getRatePerUnit();
                                double entryRevenue = quantity * rate;
                                totalRevenue += entryRevenue;
                                break;
                            }
                        }
                    }
                }
                
                android.util.Log.d("DashboardActivity", "Current month entries: " + currentMonthCount + ", Total revenue: " + totalRevenue);
                String formattedAmount = CurrencyUtils.formatIndianCurrency(totalRevenue);
                android.util.Log.d("DashboardActivity", "Formatted amount: " + formattedAmount);
                totalRevenueAmount.setText(formattedAmount);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("DashboardActivity", "Error loading revenue", e);
                totalRevenueAmount.setText("₹0");
            });
    }
    
    private void filterCustomers(String query) {
        filteredCustomers.clear();
        
        if (query == null || query.isEmpty()) {
            filteredCustomers.addAll(allCustomers);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Customer customer : allCustomers) {
                String name = customer.getName();
                String serviceType = customer.getServiceType();
                String phone = customer.getPhone();
                
                if ((name != null && name.toLowerCase().contains(lowerCaseQuery)) ||
                    (serviceType != null && serviceType.toLowerCase().contains(lowerCaseQuery)) ||
                    (phone != null && phone.contains(query))) {
                    filteredCustomers.add(customer);
                }
            }
        }
        
        // Apply current sort order
        sortCustomers();
        
        // Update adapter using submit method
        customerAdapter.submit(filteredCustomers);
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (filteredCustomers.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            customerRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            customerRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.sortButton) {
            showSortDialog();
            return true;
        } else if (id == R.id.action_calendar) {
            startActivity(new Intent(this, ServiceEntryActivity.class));
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, ProviderDashboardActivity.class));
        } else if (id == R.id.nav_customers) {
            // Already on customers page, just close drawer
        } else if (id == R.id.nav_service_entry) {
            startActivity(new Intent(this, ServiceEntryActivity.class));
        } else if (id == R.id.nav_bills) {
            startActivity(new Intent(this, BillListActivity.class));
        } else if (id == R.id.nav_reports) {
            startActivity(new Intent(this, ReportsActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_qr_code) {
            startActivity(new Intent(this, com.dailyserviceapp.qr.QRCodeActivity.class));
        } else if (id == R.id.nav_test_data) {
            generateTestData();
        } else if (id == R.id.nav_share) {
            shareApp();
        } else if (id == R.id.nav_logout) {
            logout();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    private void generateTestData() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Generate Test Data")
            .setMessage("This will create:\n\n" +
                    "✓ 5 test customers\n" +
                    "✓ ~120 service entries for January 2026\n" +
                    "✓ Ready for bill generation\n\n" +
                    "Continue?")
            .setPositiveButton("Generate", (dialog, which) -> {
                showToast("Generating test data...");
                
                TestDataGenerator generator = new TestDataGenerator(this, getCurrentUserId());
                generator.generateCompleteTestData(new TestDataGenerator.OnTestDataGeneratedListener() {
                    @Override
                    public void onTestDataGenerated(List<String> customerIds, int entriesCount) {
                        showToast("✅ Generated " + customerIds.size() + " customers and " + 
                                entriesCount + " service entries!");
                        loadData(); // Refresh the list
                    }
                    
                    @Override
                    public void onError(String error) {
                        showToast("Error: " + error);
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "DailyDrop App");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out DailyDrop app for managing daily service deliveries!");
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
    
    private void logout() {
        // Clear SharedPreferences (critical - was missing)
        preferenceManager.clearAllData();
        
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();
        
        // Sign out from Google (prevents account switching issues)
        if (googleSignInClient != null) {
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                // Navigate to login regardless of Google sign-out result
                navigateToLogin();
            });
        } else {
            navigateToLogin();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Remove listener when activity is paused to save resources
        if (customersListener != null) {
            customersListener.remove();
            customersListener = null;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning to dashboard
        loadData();
        // Update sync status
        updateSyncStatus();
    }
    
    private void updateSyncStatus() {
        if (syncStatusChip == null || offlineCache == null) {
            return;
        }
        
        int pendingCount = offlineCache.getPendingEntries().size();
        if (pendingCount > 0) {
            syncStatusChip.setVisibility(View.VISIBLE);
            String text = pendingCount == 1 ? 
                "📤 1 pending sync" : 
                "📤 " + pendingCount + " pending sync";
            syncStatusChip.setText(text);
        } else {
            syncStatusChip.setVisibility(View.GONE);
        }
    }
    
    private void showSortDialog() {
        String[] options = {"Sort by Name (A-Z)", "Sort by Service Type", "Sort by Address"};
        int currentSelection = currentSortOrder.ordinal();
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Sort Customers")
            .setSingleChoiceItems(options, currentSelection, (dialog, which) -> {
                currentSortOrder = SortOrder.values()[which];
                filterCustomers(searchEditText.getText().toString());
                dialog.dismiss();
                String sortType = options[which].toLowerCase().replace("sort by ", "");
                showToast("Sorted by " + sortType);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showCustomerOptions(Customer customer) {
        String vacationText = customer.isOnVacation() ? "Remove from Vacation" : "Mark as On Vacation";
        String[] options = {"View/Edit Details", vacationText};
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(customer.getName())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // View/Edit Details
                    Intent intent = new Intent(this, CustomerEditActivity.class);
                    intent.putExtra("customerId", customer.getId());
                    startActivity(intent);
                } else {
                    // Toggle vacation mode
                    toggleVacationMode(customer);
                }
            })
            .show();
    }
    
    private void toggleVacationMode(Customer customer) {
        boolean newVacationStatus = !customer.isOnVacation();
        
        firestore.collection("customers")
            .document(customer.getId())
            .update("onVacation", newVacationStatus)
            .addOnSuccessListener(aVoid -> {
                String message = newVacationStatus ? 
                    customer.getName() + " marked as on vacation 🏖️" :
                    customer.getName() + " is back from vacation";
                showToast(message);
            })
            .addOnFailureListener(e -> {
                showToast("Failed to update vacation status");
            });
    }
    
    private void sortCustomers() {
        switch (currentSortOrder) {
            case NAME:
                java.util.Collections.sort(filteredCustomers, (c1, c2) -> 
                    c1.getName().compareToIgnoreCase(c2.getName()));
                break;
            case SERVICE_TYPE:
                java.util.Collections.sort(filteredCustomers, (c1, c2) -> {
                    int serviceCompare = c1.getServiceType().compareToIgnoreCase(c2.getServiceType());
                    if (serviceCompare == 0) {
                        return c1.getName().compareToIgnoreCase(c2.getName());
                    }
                    return serviceCompare;
                });
                break;
            case ADDRESS:
                java.util.Collections.sort(filteredCustomers, (c1, c2) -> {
                    // Sort by area first, then by address, then by name
                    String area1 = c1.getArea() != null && !c1.getArea().isEmpty() ? c1.getArea() : "ZZZ";
                    String area2 = c2.getArea() != null && !c2.getArea().isEmpty() ? c2.getArea() : "ZZZ";
                    int areaCompare = area1.compareToIgnoreCase(area2);
                    if (areaCompare != 0) {
                        return areaCompare;
                    }
                    
                    String addr1 = c1.getAddress() != null ? c1.getAddress() : "";
                    String addr2 = c2.getAddress() != null ? c2.getAddress() : "";
                    int addrCompare = addr1.compareToIgnoreCase(addr2);
                    if (addrCompare == 0) {
                        return c1.getName().compareToIgnoreCase(c2.getName());
                    }
                    return addrCompare;
                });
                break;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up search handler to prevent memory leaks
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        // Final cleanup - remove listener if still exists
        if (customersListener != null) {
            customersListener.remove();
            customersListener = null;
        }
    }
}
