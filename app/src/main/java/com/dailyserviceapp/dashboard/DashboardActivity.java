package com.dailyserviceapp.dashboard;

import dagger.hilt.android.AndroidEntryPoint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dailyserviceapp.R;
import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.billing.BillListActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.offline.OfflineCache;
import com.dailyserviceapp.core.utils.AvatarUtils;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.databinding.ActivityHomeBinding;
import com.dailyserviceapp.databinding.NavHeaderBinding;
import com.dailyserviceapp.notifications.NotificationListActivity;
import com.dailyserviceapp.payment.PaymentActivity;
import com.dailyserviceapp.profile.ProfileActivity;
import com.dailyserviceapp.provider.ProviderComplaintsActivity;
import com.dailyserviceapp.provider.JoinRequestsActivity;
import com.dailyserviceapp.reports.ReportsActivity;
import com.dailyserviceapp.route.DeliveryRouteActivity;
import com.dailyserviceapp.sales.ui.SalesPredictionActivity;
import com.dailyserviceapp.service.ServiceEntryActivity;
import com.dailyserviceapp.ui.CustomerAdapter;
import com.dailyserviceapp.ui.CustomerPagingSource;
import com.dailyserviceapp.ui.CustomerEditActivity;
import com.dailyserviceapp.ui.PagedCustomerAdapter;
import com.dailyserviceapp.ui.CustomerDetailActivity;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Main Dashboard Activity - Landing page with customer list and analytics
 * 
 * Features:
 * - Navigation drawer with menu options
 * - Customer list with search functionality
 * - Analytics cards (total customers, monthly revenue)
 * - Add customer via FAB (manual or QR code)
 */
@AndroidEntryPoint
public class DashboardActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityHomeBinding binding;
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private TextView totalCustomersCount, totalRevenueAmount;
    private TextView txtTodayDelivered, txtTodayAmount;
    private com.google.android.material.chip.Chip syncStatusChip;
    private RecyclerView customerRecyclerView;
    private CustomerAdapter customerAdapter;
    private PagedCustomerAdapter pagedCustomerAdapter;
    private LinearLayout emptyState;
    private EditText searchEditText;
    private ExtendedFloatingActionButton addCustomerFab;
    private com.google.android.material.button.MaterialButton sortButton;
    
    private FirebaseFirestore firestore;
    @javax.inject.Inject
    OfflineCache offlineCache;
    private GoogleSignInClient googleSignInClient;
    private String providerId;
    private List<Customer> allCustomers = new ArrayList<>();
    private List<Customer> filteredCustomers = new ArrayList<>();
    private ListenerRegistration customersListener;
    private LiveData<PagingData<Customer>> pagedCustomersLiveData;
    private boolean pagingMode = false;

    // Notification badge
    private ListenerRegistration notifListener;
    private int unreadNotifCount = 0;
    private BadgeDrawable notifBadge;
    
    // Search debouncing
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY_MS = 300;
    private static final int PAGING_THRESHOLD = 500;
    
    // Sorting
    private enum SortOrder { NAME, SERVICE_TYPE, ADDRESS }
    private SortOrder currentSortOrder = SortOrder.NAME;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        providerId = getCurrentUserId();
        firestore = FirebaseFirestore.getInstance();
        // offlineCache is injected by Hilt
        
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
        loadCachedDashboardMetrics();
        loadData();

        loadUnreadNotificationCount();
        if (getIntent() != null && getIntent().getBooleanExtra("openNotifications", false)) {
            startActivity(new Intent(this, NotificationListActivity.class));
        }
    }
    
    private void initializeViews() {
        drawerLayout = binding.drawerLayout;
        navigationView = binding.navigationView;
        toolbar = binding.topAppBar;
        swipeRefreshLayout = null;
        
        totalCustomersCount = binding.txtCustomerCount;
        totalRevenueAmount = binding.txtTotalValue;
        txtTodayDelivered = binding.txtTodayDelivered;
        txtTodayAmount = binding.txtTodayAmount;
        syncStatusChip = binding.syncStatusChip;
        customerRecyclerView = binding.customerRecyclerView;
        emptyState = binding.emptyStateLayout;
        searchEditText = binding.edtSearch;
        addCustomerFab = binding.fabAddCustomer;
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
        if (headerView == null) {
            return;
        }
        NavHeaderBinding headerBinding = NavHeaderBinding.bind(headerView);

        bindNavigationHeader(headerBinding, null, null);

        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(providerId)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot == null || !snapshot.exists()) {
                    return;
                }

                String profileName = snapshot.getString("businessName");
                if (profileName == null || profileName.trim().isEmpty()) {
                    profileName = snapshot.getString("name");
                }

                String profileEmail = snapshot.getString("email");
                bindNavigationHeader(headerBinding, profileName, profileEmail);
            });
    }

    private void bindNavigationHeader(NavHeaderBinding headerBinding, String profileName, String profileEmail) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        String authName = currentUser != null ? currentUser.getDisplayName() : null;
        String authEmail = currentUser != null ? currentUser.getEmail() : null;

        String prefName = preferenceManager != null ? preferenceManager.getUserName() : null;
        String prefEmail = preferenceManager != null ? preferenceManager.getUserEmail() : null;

        String displayName = AvatarUtils.resolveDisplayName(
            firstNonEmpty(profileName, authName, prefName),
            firstNonEmpty(profileEmail, authEmail, prefEmail),
            getString(R.string.default_user_name)
        );
        String displayEmail = firstNonEmpty(profileEmail, authEmail, prefEmail, getString(R.string.default_user_email));

        String initials = AvatarUtils.getInitials(displayName);

        headerBinding.userName.setText(displayName);
        headerBinding.userEmail.setText(displayEmail);
        headerBinding.userInitial.setText(initials);
        headerBinding.userInitial.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        headerBinding.userInitial.setContentDescription(getString(R.string.profile_initial_for, displayName));
        headerBinding.avatarCard.setCardBackgroundColor(AvatarUtils.getAvatarColor(displayName));

        View.OnClickListener openProfileListener = v -> startActivity(new Intent(this, ProfileActivity.class));
        headerBinding.avatarCard.setOnClickListener(openProfileListener);
        headerBinding.userInitial.setOnClickListener(openProfileListener);
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
    
    private void setupRecyclerView() {
        customerAdapter = new CustomerAdapter(new CustomerAdapter.OnCustomerActionListener() {
            @Override
            public void onViewProfile(Customer customer) {
                openCustomerProfile(customer);
            }

            @Override
            public void onEditCustomer(Customer customer) {
                openCustomerEditor(customer);
            }

            @Override
            public void onToggleVacation(Customer customer) {
                toggleVacationMode(customer);
            }
        });

        pagedCustomerAdapter = new PagedCustomerAdapter(new PagedCustomerAdapter.OnCustomerActionListener() {
            @Override
            public void onViewProfile(Customer customer) {
                openCustomerProfile(customer);
            }

            @Override
            public void onEditCustomer(Customer customer) {
                openCustomerEditor(customer);
            }

            @Override
            public void onToggleVacation(Customer customer) {
                toggleVacationMode(customer);
            }
        });

        customerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        customerRecyclerView.setAdapter(pagedCustomerAdapter);
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
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        if (status != null && !status.trim().isEmpty()
                            && !"ACTIVE".equalsIgnoreCase(status)) {
                            continue;
                        }
                        Customer customer = document.toObject(Customer.class);
                        customer.setId(document.getId());
                        allCustomers.add(customer);
                    }
                    
                    // Cache for offline access
                    offlineCache.cacheCustomers(allCustomers);

                    String query = searchEditText != null && searchEditText.getText() != null
                        ? searchEditText.getText().toString() : "";

                    if (shouldUsePaging(query)) {
                        showPagedCustomers();
                    } else {
                        filterCustomers(query);
                    }
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
        cacheDashboardValue(Constants.PREF_DASHBOARD_TOTAL_CUSTOMERS, totalCustomersCount.getText().toString());

        if (allCustomers.isEmpty()) {
            String deliveredText = getString(R.string.delivered_format, 0, 0);
            String zeroAmount = CurrencyUtils.formatIndianCurrency(0.0);
            txtTodayDelivered.setText(deliveredText);
            txtTodayAmount.setText(zeroAmount);
            totalRevenueAmount.setText(zeroAmount);
            cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_DELIVERED, deliveredText);
            cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_AMOUNT, zeroAmount);
            cacheDashboardValue(Constants.PREF_DASHBOARD_MONTHLY_REVENUE, zeroAmount);
            return;
        }

        int totalCustomers = allCustomers.size();
        java.util.Map<String, Double> rateMap = buildCustomerRateMap();
        loadRevenueAndTodaySummaryCombined(totalCustomers, rateMap);
    }

    /**
     * Optimized analytics fetch: one monthly query computes
     * monthly revenue + today's delivered count + today's amount.
     */
    private void loadRevenueAndTodaySummaryCombined(int totalCustomers, java.util.Map<String, Double> rateMap) {
        Calendar monthStartCal = Calendar.getInstance();
        monthStartCal.set(Calendar.DAY_OF_MONTH, 1);
        monthStartCal.set(Calendar.HOUR_OF_DAY, 0);
        monthStartCal.set(Calendar.MINUTE, 0);
        monthStartCal.set(Calendar.SECOND, 0);
        monthStartCal.set(Calendar.MILLISECOND, 0);

        Calendar nextMonthCal = (Calendar) monthStartCal.clone();
        nextMonthCal.add(Calendar.MONTH, 1);

        Calendar todayStartCal = Calendar.getInstance();
        todayStartCal.set(Calendar.HOUR_OF_DAY, 0);
        todayStartCal.set(Calendar.MINUTE, 0);
        todayStartCal.set(Calendar.SECOND, 0);
        todayStartCal.set(Calendar.MILLISECOND, 0);

        Calendar tomorrowStartCal = (Calendar) todayStartCal.clone();
        tomorrowStartCal.add(Calendar.DAY_OF_YEAR, 1);

        final long todayStartMillis = todayStartCal.getTimeInMillis();
        final long tomorrowStartMillis = tomorrowStartCal.getTimeInMillis();

        Timestamp startOfMonth = new Timestamp(monthStartCal.getTime());
        Timestamp endExclusive = new Timestamp(nextMonthCal.getTime());

        firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("delivered", true)
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThan("date", endExclusive)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                double monthlyRevenue = 0.0;
                double todayEarnings = 0.0;
                int deliveredToday = 0;

                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                    Double rate = doc.getDouble("rate");
                    Double quantity = doc.getDouble("quantity");
                    String customerId = doc.getString("customerId");
                    Timestamp entryDate = doc.getTimestamp("date");
                    if (quantity == null || entryDate == null) {
                        continue;
                    }

                    if ((rate == null || rate == 0.0) && customerId != null) {
                        rate = rateMap.get(customerId);
                    }
                    if (rate == null) {
                        continue;
                    }

                    double amount = rate * quantity;
                    monthlyRevenue += amount;

                    long entryMillis = entryDate.toDate().getTime();
                    if (entryMillis >= todayStartMillis && entryMillis < tomorrowStartMillis) {
                        deliveredToday++;
                        todayEarnings += amount;
                    }
                }

                String deliveredText = getString(R.string.delivered_format, deliveredToday, totalCustomers);
                String todayAmount = CurrencyUtils.formatIndianCurrency(todayEarnings);
                String monthlyAmount = CurrencyUtils.formatIndianCurrency(monthlyRevenue);

                txtTodayDelivered.setText(deliveredText);
                txtTodayAmount.setText(todayAmount);
                totalRevenueAmount.setText(monthlyAmount);

                cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_DELIVERED, deliveredText);
                cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_AMOUNT, todayAmount);
                cacheDashboardValue(Constants.PREF_DASHBOARD_MONTHLY_REVENUE, monthlyAmount);
            })
            .addOnFailureListener(e -> {
                android.util.Log.w("DashboardActivity", "Combined analytics query failed, using fallback", e);
                calculateCurrentMonthRevenue();
                calculateTodaysSummary();
            });
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
        
        java.util.Calendar tomorrow = (java.util.Calendar) today.clone();
        tomorrow.add(java.util.Calendar.DAY_OF_YEAR, 1);
        
        Timestamp startOfDay = new Timestamp(today.getTime());
        Timestamp endExclusive = new Timestamp(tomorrow.getTime());
        
        final int totalCustomers = allCustomers.size();

        java.util.Map<String, Double> rateMap = buildCustomerRateMap();
        loadTodaysSummaryOptimized(startOfDay, endExclusive, totalCustomers, rateMap);
    }
    
    private void calculateCurrentMonthRevenue() {
        // Reset counter only if no cached value exists
        if (totalRevenueAmount.getText() == null || totalRevenueAmount.getText().toString().trim().isEmpty()) {
            totalRevenueAmount.setText(CurrencyUtils.formatIndianCurrency(0.0));
        }
        
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
        
        Timestamp startOfMonth = new Timestamp(calendar.getTime());
        Calendar nextMonth = (Calendar) calendar.clone();
        nextMonth.add(Calendar.MONTH, 1);
        Timestamp endExclusive = new Timestamp(nextMonth.getTime());

        android.util.Log.d("DashboardActivity", "Calculating revenue for providerId: " + providerId + ", customers count: " + allCustomers.size());
        java.util.Map<String, Double> rateMap = buildCustomerRateMap();
        loadMonthlyRevenueOptimized(startOfMonth, endExclusive, rateMap);
    }

    private java.util.Map<String, Double> buildCustomerRateMap() {
        java.util.Map<String, Double> rateMap = new java.util.HashMap<>();
        for (Customer customer : allCustomers) {
            if (customer.getId() != null) {
                rateMap.put(customer.getId(), customer.getRatePerUnit());
            }
        }
        return rateMap;
    }

    private void loadTodaysSummaryOptimized(Timestamp startOfDay, Timestamp endExclusive,
                                            int totalCustomers, java.util.Map<String, Double> rateMap) {
        // Optimized: Use single query with all filters and limit to 1000 docs
        firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("delivered", true)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThan("date", endExclusive)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int deliveredCount = 0;
                double todayEarnings = 0.0;

                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                    deliveredCount++;
                    Double rate = doc.getDouble("rate");
                    Double quantity = doc.getDouble("quantity");
                    String customerId = doc.getString("customerId");

                    if ((rate == null || rate == 0.0) && customerId != null) {
                        rate = rateMap.get(customerId);
                    }

                    if (rate != null && quantity != null) {
                        todayEarnings += (rate * quantity);
                    }
                }

                int finalDeliveredCount = deliveredCount;
                double finalTodayEarnings = todayEarnings;
                runOnUiThread(() -> {
                    String deliveredText = getString(R.string.delivered_format,
                        finalDeliveredCount, totalCustomers);
                    String amountText = CurrencyUtils.formatIndianCurrency(finalTodayEarnings);
                    txtTodayDelivered.setText(deliveredText);
                    txtTodayAmount.setText(amountText);
                    cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_DELIVERED, deliveredText);
                    cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_AMOUNT, amountText);
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.w("Dashboard", "Optimized today query failed, falling back: " + e.getMessage());
                loadTodaysSummaryFallback(startOfDay, endExclusive, totalCustomers, rateMap);
            });
    }

    private void loadTodaysSummaryFallback(Timestamp startOfDay, Timestamp endExclusive,
                                           int totalCustomers, java.util.Map<String, Double> rateMap) {
        firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("delivered", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int deliveredCount = 0;
                double todayEarnings = 0.0;

                long startTime = startOfDay.toDate().getTime();
                long endTime = endExclusive.toDate().getTime();

                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                    Timestamp entryDate = doc.getTimestamp("date");
                    if (entryDate == null) continue;
                    long entryTime = entryDate.toDate().getTime();
                    if (entryTime >= startTime && entryTime < endTime) {
                        deliveredCount++;
                        Double rate = doc.getDouble("rate");
                        Double quantity = doc.getDouble("quantity");
                        String customerId = doc.getString("customerId");

                        if ((rate == null || rate == 0.0) && customerId != null) {
                            rate = rateMap.get(customerId);
                        }
                        if (rate != null && quantity != null) {
                            todayEarnings += (rate * quantity);
                        }
                    }
                }

                int finalDeliveredCount = deliveredCount;
                double finalTodayEarnings = todayEarnings;
                runOnUiThread(() -> {
                    String deliveredText = getString(R.string.delivered_format,
                        finalDeliveredCount, totalCustomers);
                    String amountText = CurrencyUtils.formatIndianCurrency(finalTodayEarnings);
                    txtTodayDelivered.setText(deliveredText);
                    txtTodayAmount.setText(amountText);
                    cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_DELIVERED, deliveredText);
                    cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_AMOUNT, amountText);
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

    private void loadMonthlyRevenueOptimized(Timestamp startOfMonth, Timestamp endExclusive,
                                             java.util.Map<String, Double> rateMap) {
        // Optimized: Add limit to prevent fetching too much data
        firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("delivered", true)
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThan("date", endExclusive)
            .get()
            .addOnSuccessListener(querySnapshots -> {
                double totalRevenue = 0;

                for (QueryDocumentSnapshot doc : querySnapshots) {
                    Double rate = doc.getDouble("rate");
                    Double quantity = doc.getDouble("quantity");
                    String customerId = doc.getString("customerId");

                    if ((rate == null || rate == 0.0) && customerId != null) {
                        rate = rateMap.get(customerId);
                    }

                    if (rate != null && quantity != null) {
                        totalRevenue += (rate * quantity);
                    }
                }

                String formattedAmount = CurrencyUtils.formatIndianCurrency(totalRevenue);
                totalRevenueAmount.setText(formattedAmount);
                cacheDashboardValue(Constants.PREF_DASHBOARD_MONTHLY_REVENUE, formattedAmount);
            })
            .addOnFailureListener(e -> {
                android.util.Log.w("DashboardActivity", "Optimized revenue query failed, falling back", e);
                loadMonthlyRevenueFallback(startOfMonth, endExclusive, rateMap);
            });
    }

    private void loadMonthlyRevenueFallback(Timestamp startOfMonth, Timestamp endExclusive,
                                            java.util.Map<String, Double> rateMap) {
        long startMillis = startOfMonth.toDate().getTime();
        long endMillis = endExclusive.toDate().getTime();

        firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .get()
            .addOnSuccessListener(querySnapshots -> {
                double totalRevenue = 0;

                for (QueryDocumentSnapshot doc : querySnapshots) {
                    Timestamp entryDate = doc.getTimestamp("date");
                    if (entryDate == null) continue;
                    long entryTime = entryDate.toDate().getTime();
                    if (entryTime >= startMillis && entryTime < endMillis) {
                        Double rate = doc.getDouble("rate");
                        Double quantity = doc.getDouble("quantity");
                        String customerId = doc.getString("customerId");

                        if ((rate == null || rate == 0.0) && customerId != null) {
                            rate = rateMap.get(customerId);
                        }
                        if (rate != null && quantity != null) {
                            totalRevenue += (rate * quantity);
                        }
                    }
                }

                totalRevenueAmount.setText(CurrencyUtils.formatIndianCurrency(totalRevenue));
                cacheDashboardValue(Constants.PREF_DASHBOARD_MONTHLY_REVENUE, totalRevenueAmount.getText().toString());
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("DashboardActivity", "Error loading revenue", e);
                totalRevenueAmount.setText(CurrencyUtils.formatIndianCurrency(0.0));
            });
    }
    
    private void filterCustomers(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (shouldUsePaging(normalizedQuery)) {
            showPagedCustomers();
            updateEmptyState();
            return;
        }

        filteredCustomers.clear();
        
        if (normalizedQuery.isEmpty()) {
            filteredCustomers.addAll(allCustomers);
        } else {
            String lowerCaseQuery = normalizedQuery.toLowerCase(Locale.getDefault());
            for (Customer customer : allCustomers) {
                String name = customer.getName();
                String serviceType = customer.getServiceType();
                String phone = customer.getPhone();
                
                if ((name != null && name.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                    (serviceType != null && serviceType.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                    (phone != null && phone.contains(normalizedQuery))) {
                    filteredCustomers.add(customer);
                }
            }
        }
        
        // Apply current sort order
        sortCustomers();
        
        showLocalCustomers(filteredCustomers);
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        boolean isEmpty = pagingMode ? allCustomers.isEmpty() : filteredCustomers.isEmpty();
        if (isEmpty) {
            emptyState.setVisibility(View.VISIBLE);
            customerRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            customerRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadUnreadNotificationCount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (notifListener != null) {
            notifListener.remove();
        }

        notifListener = FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", user.getUid())
            .whereEqualTo("read", false)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) return;
                unreadNotifCount = snapshots.size();
                updateNotificationBadge();
            });
    }

    @OptIn(markerClass = ExperimentalBadgeUtils.class)
    private void updateNotificationBadge() {
        if (toolbar == null) return;
        Menu menu = toolbar.getMenu();
        if (menu == null) return;

        MenuItem notifItem = menu.findItem(R.id.action_notifications);
        if (notifItem == null) return;

        if (notifBadge == null) {
            notifBadge = BadgeDrawable.create(this);
            BadgeUtils.attachBadgeDrawable(notifBadge, toolbar, R.id.action_notifications);
        }

        if (unreadNotifCount > 0) {
            notifBadge.setVisible(true);
            notifBadge.setNumber(unreadNotifCount);
        } else {
            notifBadge.setVisible(false);
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
        } else if (id == R.id.action_notifications) {
            startActivity(new Intent(this, NotificationListActivity.class));
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
        } else if (id == R.id.nav_join_requests) {
            startActivity(new Intent(this, JoinRequestsActivity.class));
        } else if (id == R.id.nav_complaints) {
            startActivity(new Intent(this, ProviderComplaintsActivity.class));
        } else if (id == R.id.nav_service_entry) {
            startActivity(new Intent(this, ServiceEntryActivity.class));
        } else if (id == R.id.nav_route) {
            startActivity(new Intent(this, DeliveryRouteActivity.class));
        } else if (id == R.id.nav_bills) {
            startActivity(new Intent(this, BillListActivity.class));
        } else if (id == R.id.nav_reports) {
            startActivity(new Intent(this, ReportsActivity.class));
        } else if (id == R.id.nav_sales) {
            startActivity(new Intent(this, SalesPredictionActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_qr_code) {
            startActivity(new Intent(this, com.dailyserviceapp.qr.QRCodeActivity.class));
        } else if (id == R.id.nav_share) {
            shareApp();
        } else if (id == R.id.nav_logout) {
            logout();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
    protected void onStop() {
        super.onStop();
        // Keep listener during transient pauses (dialogs/overlays), release when fully not visible.
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

    private void loadCachedDashboardMetrics() {
        String totalCustomers = preferenceManager.getString(prefKey(Constants.PREF_DASHBOARD_TOTAL_CUSTOMERS), null);
        if (totalCustomers != null && totalCustomersCount != null) {
            totalCustomersCount.setText(totalCustomers);
        }

        String todayDelivered = preferenceManager.getString(prefKey(Constants.PREF_DASHBOARD_TODAY_DELIVERED), null);
        if (todayDelivered != null && txtTodayDelivered != null) {
            txtTodayDelivered.setText(todayDelivered);
        }

        String todayAmount = preferenceManager.getString(prefKey(Constants.PREF_DASHBOARD_TODAY_AMOUNT), null);
        if (todayAmount != null && txtTodayAmount != null) {
            txtTodayAmount.setText(todayAmount);
        }

        String monthlyRevenue = preferenceManager.getString(prefKey(Constants.PREF_DASHBOARD_MONTHLY_REVENUE), null);
        if (monthlyRevenue != null && totalRevenueAmount != null) {
            totalRevenueAmount.setText(monthlyRevenue);
        }
    }

    private void cacheDashboardValue(String key, String value) {
        if (value == null) return;
        preferenceManager.putString(prefKey(key), value);
    }

    private String prefKey(String key) {
        if (providerId == null || providerId.isEmpty()) {
            return key;
        }
        return key + "_" + providerId;
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
                String sortType = options[which]
                    .toLowerCase(Locale.getDefault())
                    .replace("sort by ", "");
                showToast("Sorted by " + sortType);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void openCustomerProfile(Customer customer) {
        if (customer == null || customer.getId() == null) {
            return;
        }
        Intent intent = new Intent(this, CustomerDetailActivity.class);
        intent.putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customer.getId());
        startActivity(intent);
    }

    private void openCustomerEditor(Customer customer) {
        if (customer == null || customer.getId() == null) {
            return;
        }
        Intent intent = new Intent(this, CustomerEditActivity.class);
        intent.putExtra("customerId", customer.getId());
        startActivity(intent);
    }
    
    private void toggleVacationMode(Customer customer) {
        if (customer == null || customer.getId() == null) {
            showToast("Customer details are incomplete");
            return;
        }

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

    private boolean shouldUsePaging(String query) {
        return (query == null || query.trim().isEmpty())
            && currentSortOrder == SortOrder.NAME
            && allCustomers.size() >= PAGING_THRESHOLD;
    }

    private void showPagedCustomers() {
        pagingMode = true;
        if (customerRecyclerView.getAdapter() != pagedCustomerAdapter) {
            customerRecyclerView.setAdapter(pagedCustomerAdapter);
        }
        loadPagedCustomers();
    }

    private void showLocalCustomers(List<Customer> customers) {
        pagingMode = false;
        if (customerRecyclerView.getAdapter() != customerAdapter) {
            customerRecyclerView.setAdapter(customerAdapter);
        }
        customerAdapter.submit(customers);
    }

    private void loadPagedCustomers() {
        if (providerId == null || providerId.isEmpty()) {
            return;
        }

        PagingConfig config = new PagingConfig(Constants.PAGE_SIZE, Constants.PAGE_SIZE, false);
        Pager<DocumentSnapshot, Customer> pager = new Pager<>(
            config,
            () -> new CustomerPagingSource(firestore, providerId, Constants.PAGE_SIZE, "name")
        );

        LiveData<PagingData<Customer>> newLiveData = PagingLiveData.cachedIn(
            PagingLiveData.getLiveData(pager),
            getLifecycle()
        );

        if (pagedCustomersLiveData != null) {
            pagedCustomersLiveData.removeObservers(this);
        }
        pagedCustomersLiveData = newLiveData;
        pagedCustomersLiveData.observe(this,
            pagingData -> pagedCustomerAdapter.submitData(getLifecycle(), pagingData));
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
        if (notifListener != null) {
            notifListener.remove();
            notifListener = null;
        }
        binding = null;
    }
}
