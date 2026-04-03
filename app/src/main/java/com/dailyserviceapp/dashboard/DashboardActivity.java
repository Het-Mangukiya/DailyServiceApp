package com.dailyserviceapp.dashboard;

import dagger.hilt.android.AndroidEntryPoint;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.dailyserviceapp.notifications.FCMService;
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
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private FloatingActionButton addCustomerFab;
    
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

    // Notification Permission Launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showToast("Notifications enabled");
                }
            });
    
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
        
        // Notification Setup
        checkNotificationPermission();
        refreshFcmToken();

        if (getIntent() != null && getIntent().getBooleanExtra("openNotifications", false)) {
            startActivity(new Intent(this, NotificationListActivity.class));
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void refreshFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null && !token.isEmpty()) {
                        FCMService.saveTokenToFirestore(token);
                    }
                })
                .addOnFailureListener(e -> android.util.Log.e("DashboardActivity", "FCM Token failed", e));
    }
    
    private void initializeViews() {
        drawerLayout = binding.drawerLayout;
        navigationView = binding.navigationView;
        toolbar = binding.topAppBar;
        
        try {
            totalCustomersCount = binding.txtCustomerCount;
            totalRevenueAmount = binding.txtTotalValue;
            txtTodayDelivered = binding.txtTodayDelivered;
            txtTodayAmount = binding.txtTodayAmount;
            syncStatusChip = binding.syncStatusChip;
            customerRecyclerView = binding.customerRecyclerView;
            emptyState = binding.emptyStateLayout;
            addCustomerFab = binding.fabAddCustomer;
            searchEditText = binding.searchEditText; 
        } catch (Exception e) {
            android.util.Log.e("DashboardActivity", "Error binding views: " + e.getMessage());
        }
        
        setSupportActionBar(toolbar);
    }
    
    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        navigationView.setNavigationItemSelectedListener(this);
        
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;
        
        NavHeaderBinding headerBinding = NavHeaderBinding.bind(headerView);
        bindNavigationHeader(headerBinding, null, null);

        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(providerId)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot == null || !snapshot.exists()) return;
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

        headerBinding.userName.setText(displayName);
        headerBinding.userEmail.setText(displayEmail);
        headerBinding.userInitial.setText(AvatarUtils.getInitials(displayName));
        headerBinding.avatarCard.setCardBackgroundColor(AvatarUtils.getAvatarColor(displayName));

        View.OnClickListener openProfileListener = v -> startActivity(new Intent(this, ProfileActivity.class));
        headerBinding.avatarCard.setOnClickListener(openProfileListener);
        headerBinding.userInitial.setOnClickListener(openProfileListener);
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
    
    private void setupRecyclerView() {
        customerAdapter = new CustomerAdapter(new CustomerAdapter.OnCustomerActionListener() {
            @Override public void onViewProfile(Customer customer) { openCustomerProfile(customer); }
            @Override public void onEditCustomer(Customer customer) { openCustomerEditor(customer); }
            @Override public void onToggleVacation(Customer customer) { toggleVacationMode(customer); }
        });

        pagedCustomerAdapter = new PagedCustomerAdapter(new PagedCustomerAdapter.OnCustomerActionListener() {
            @Override public void onViewProfile(Customer customer) { openCustomerProfile(customer); }
            @Override public void onEditCustomer(Customer customer) { openCustomerEditor(customer); }
            @Override public void onToggleVacation(Customer customer) { toggleVacationMode(customer); }
        });

        if (customerRecyclerView != null) {
            customerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            customerRecyclerView.setAdapter(pagedCustomerAdapter);
        }
    }
    
    private void setupListeners() {
        if (addCustomerFab != null) {
            addCustomerFab.setOnClickListener(v -> startActivity(new Intent(this, CustomerEditActivity.class)));
        }
        
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> filterCustomers(s.toString());
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }
    
    private void loadData() {
        if (providerId == null || providerId.isEmpty()) return;
        loadCustomers();
    }
    
    private void loadCustomers() {
        if (customersListener != null) customersListener.remove();
        
        customersListener = firestore.collection("customers")
            .whereEqualTo("providerId", providerId)
            .addSnapshotListener((queryDocumentSnapshots, error) -> {
                if (error != null) {
                    showToast("Failed to load customers: " + error.getMessage());
                    return;
                }
                
                if (queryDocumentSnapshots != null) {
                    allCustomers.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        if (status != null && !status.trim().isEmpty() && !"ACTIVE".equalsIgnoreCase(status)) continue;
                        Customer customer = document.toObject(Customer.class);
                        customer.setId(document.getId());
                        allCustomers.add(customer);
                    }
                    
                    if (offlineCache != null) offlineCache.cacheCustomers(allCustomers);

                    String query = searchEditText != null && searchEditText.getText() != null
                        ? searchEditText.getText().toString() : "";

                    if (shouldUsePaging(query)) {
                        showPagedCustomers();
                    } else {
                        filterCustomers(query);
                    }
                    updateEmptyState();
                    loadAnalytics();
                }
            });
    }
    
    private void loadAnalytics() {
        if (totalCustomersCount != null) totalCustomersCount.setText(String.valueOf(allCustomers.size()));
        cacheDashboardValue(Constants.PREF_DASHBOARD_TOTAL_CUSTOMERS, String.valueOf(allCustomers.size()));

        if (allCustomers.isEmpty()) {
            setAnalyticsText("0 / 0", "₹ 0.00", "₹ 0.00");
            return;
        }

        int totalCustomers = allCustomers.size();
        java.util.Map<String, Double> rateMap = buildCustomerRateMap();
        loadAnalyticsData(totalCustomers, rateMap);
    }

    private void setAnalyticsText(String delivered, String today, String monthly) {
        if (txtTodayDelivered != null) txtTodayDelivered.setText(delivered);
        if (txtTodayAmount != null) txtTodayAmount.setText(today);
        if (totalRevenueAmount != null) totalRevenueAmount.setText(monthly);
        
        cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_DELIVERED, delivered);
        cacheDashboardValue(Constants.PREF_DASHBOARD_TODAY_AMOUNT, today);
        cacheDashboardValue(Constants.PREF_DASHBOARD_MONTHLY_REVENUE, monthly);
    }

    private void loadAnalyticsData(int totalCustomers, java.util.Map<String, Double> rateMap) {
        Calendar monthStartCal = Calendar.getInstance();
        monthStartCal.set(Calendar.DAY_OF_MONTH, 1);
        monthStartCal.set(Calendar.HOUR_OF_DAY, 0);
        monthStartCal.set(Calendar.MINUTE, 0);
        monthStartCal.set(Calendar.SECOND, 0);
        
        Calendar todayStartCal = Calendar.getInstance();
        todayStartCal.set(Calendar.HOUR_OF_DAY, 0);
        todayStartCal.set(Calendar.MINUTE, 0);
        todayStartCal.set(Calendar.SECOND, 0);

        final long todayStartMillis = todayStartCal.getTimeInMillis();
        final long tomorrowStartMillis = todayStartMillis + (24 * 60 * 60 * 1000);

        firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("delivered", true)
            .whereGreaterThanOrEqualTo("date", new Timestamp(monthStartCal.getTime()))
            .get()
            .addOnSuccessListener(querySnapshot -> {
                double monthlyRevenue = 0.0, todayEarnings = 0.0;
                int deliveredToday = 0;

                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                    Double rate = doc.getDouble("rate");
                    Double quantity = doc.getDouble("quantity");
                    Timestamp entryDate = doc.getTimestamp("date");
                    if (quantity == null || entryDate == null) continue;

                    if ((rate == null || rate == 0.0)) rate = rateMap.get(doc.getString("customerId"));
                    if (rate == null) continue;

                    double amount = rate * quantity;
                    monthlyRevenue += amount;

                    long entryMillis = entryDate.toDate().getTime();
                    if (entryMillis >= todayStartMillis && entryMillis < tomorrowStartMillis) {
                        deliveredToday++;
                        todayEarnings += amount;
                    }
                }
                setAnalyticsText(
                    getString(R.string.delivered_format, deliveredToday, totalCustomers),
                    CurrencyUtils.formatIndianCurrency(todayEarnings),
                    CurrencyUtils.formatIndianCurrency(monthlyRevenue)
                );
            });
    }

    private java.util.Map<String, Double> buildCustomerRateMap() {
        java.util.Map<String, Double> rateMap = new java.util.HashMap<>();
        for (Customer customer : allCustomers) {
            if (customer.getId() != null) rateMap.put(customer.getId(), customer.getRatePerUnit());
        }
        return rateMap;
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
                if (customerMatches(customer, lowerCaseQuery, normalizedQuery)) filteredCustomers.add(customer);
            }
        }
        
        sortCustomers();
        showLocalCustomers(filteredCustomers);
        updateEmptyState();
    }

    private boolean customerMatches(Customer customer, String lowerQuery, String rawQuery) {
        String name = customer.getName(), service = customer.getServiceType(), phone = customer.getPhone();
        return (name != null && name.toLowerCase(Locale.getDefault()).contains(lowerQuery)) ||
               (service != null && service.toLowerCase(Locale.getDefault()).contains(lowerQuery)) ||
               (phone != null && phone.contains(rawQuery));
    }
    
    private void updateEmptyState() {
        if (emptyState == null || customerRecyclerView == null) return;
        boolean isEmpty = pagingMode ? allCustomers.isEmpty() : filteredCustomers.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        customerRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void loadUnreadNotificationCount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        if (notifListener != null) notifListener.remove();

        notifListener = firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
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
        notifBadge.setVisible(unreadNotifCount > 0);
        if (unreadNotifCount > 0) notifBadge.setNumber(unreadNotifCount);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sortButton) { showSortDialog(); return true; }
        if (id == R.id.action_calendar) { startActivity(new Intent(this, ServiceEntryActivity.class)); return true; }
        if (id == R.id.action_notifications) { startActivity(new Intent(this, NotificationListActivity.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) startActivity(new Intent(this, ProviderDashboardActivity.class));
        else if (id == R.id.nav_join_requests) startActivity(new Intent(this, JoinRequestsActivity.class));
        else if (id == R.id.nav_complaints) startActivity(new Intent(this, ProviderComplaintsActivity.class));
        else if (id == R.id.nav_service_entry) startActivity(new Intent(this, ServiceEntryActivity.class));
        else if (id == R.id.nav_route) startActivity(new Intent(this, DeliveryRouteActivity.class));
        else if (id == R.id.nav_bills) startActivity(new Intent(this, BillListActivity.class));
        else if (id == R.id.nav_reports) startActivity(new Intent(this, ReportsActivity.class));
        else if (id == R.id.nav_sales) startActivity(new Intent(this, SalesPredictionActivity.class));
        else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
        else if (id == R.id.nav_qr_code) startActivity(new Intent(this, com.dailyserviceapp.qr.QRCodeActivity.class));
        else if (id == R.id.nav_share) shareApp();
        else if (id == R.id.nav_logout) logout();
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out DailyDrop app for managing daily service deliveries!");
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
    
    private void logout() {
        preferenceManager.clearAllData();
        FirebaseAuth.getInstance().signOut();
        if (googleSignInClient != null) {
            googleSignInClient.signOut().addOnCompleteListener(this, task -> navigateToLogin());
        } else {
            navigateToLogin();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (customersListener != null) { customersListener.remove(); customersListener = null; }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        updateSyncStatus();
    }
    
    private void updateSyncStatus() {
        if (syncStatusChip == null || offlineCache == null) return;
        int pendingCount = offlineCache.getPendingEntries().size();
        syncStatusChip.setVisibility(pendingCount > 0 ? View.VISIBLE : View.GONE);
        if (pendingCount > 0) syncStatusChip.setText(pendingCount == 1 ? "📤 1 pending sync" : "📤 " + pendingCount + " pending sync");
    }

    private void loadCachedDashboardMetrics() {
        if (totalCustomersCount != null) totalCustomersCount.setText(preferenceManager.getString(prefKey(Constants.PREF_DASHBOARD_TOTAL_CUSTOMERS), "0"));
        if (txtTodayDelivered != null) txtTodayDelivered.setText(preferenceManager.getString(prefKey(Constants.PREF_DASHBOARD_TODAY_DELIVERED), "0 / 0"));
        if (txtTodayAmount != null) txtTodayAmount.setText(preferenceManager.getString(prefKey(Constants.PREF_DASHBOARD_TODAY_AMOUNT), "₹ 0.00"));
        if (totalRevenueAmount != null) totalRevenueAmount.setText(preferenceManager.getString(prefKey(Constants.PREF_DASHBOARD_MONTHLY_REVENUE), "₹ 0.00"));
    }

    private void cacheDashboardValue(String key, String value) {
        if (value != null) preferenceManager.putString(prefKey(key), value);
    }

    private String prefKey(String key) {
        return providerId == null ? key : key + "_" + providerId;
    }
    
    private void showSortDialog() {
        String[] options = {"Sort by Name (A-Z)", "Sort by Service Type", "Sort by Address"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Sort Customers")
            .setSingleChoiceItems(options, currentSortOrder.ordinal(), (dialog, which) -> {
                currentSortOrder = SortOrder.values()[which];
                filterCustomers(searchEditText != null ? searchEditText.getText().toString() : "");
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null).show();
    }
    
    private void openCustomerProfile(Customer customer) {
        if (customer == null || customer.getId() == null) return;
        Intent intent = new Intent(this, CustomerDetailActivity.class);
        intent.putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customer.getId());
        startActivity(intent);
    }

    private void openCustomerEditor(Customer customer) {
        if (customer == null || customer.getId() == null) return;
        Intent intent = new Intent(this, CustomerEditActivity.class);
        intent.putExtra("customerId", customer.getId());
        startActivity(intent);
    }
    
    private void toggleVacationMode(Customer customer) {
        if (customer == null || customer.getId() == null) return;
        boolean newStatus = !customer.isOnVacation();
        firestore.collection("customers").document(customer.getId()).update("onVacation", newStatus)
            .addOnSuccessListener(aVoid -> showToast(customer.getName() + (newStatus ? " on vacation" : " is back")));
    }
    
    private void sortCustomers() {
        java.util.Collections.sort(filteredCustomers, (c1, c2) -> {
            if (currentSortOrder == SortOrder.SERVICE_TYPE) {
                int res = c1.getServiceType().compareToIgnoreCase(c2.getServiceType());
                return res != 0 ? res : c1.getName().compareToIgnoreCase(c2.getName());
            }
            if (currentSortOrder == SortOrder.ADDRESS) {
                String a1 = c1.getArea() == null ? "" : c1.getArea(), a2 = c2.getArea() == null ? "" : c2.getArea();
                int res = a1.compareToIgnoreCase(a2);
                return res != 0 ? res : c1.getName().compareToIgnoreCase(c2.getName());
            }
            return c1.getName().compareToIgnoreCase(c2.getName());
        });
    }

    private boolean shouldUsePaging(String query) {
        return (query == null || query.trim().isEmpty()) && currentSortOrder == SortOrder.NAME && allCustomers.size() >= PAGING_THRESHOLD;
    }

    private void showPagedCustomers() {
        pagingMode = true;
        if (customerRecyclerView != null && customerRecyclerView.getAdapter() != pagedCustomerAdapter) customerRecyclerView.setAdapter(pagedCustomerAdapter);
        loadPagedCustomers();
    }

    private void showLocalCustomers(List<Customer> customers) {
        pagingMode = false;
        if (customerRecyclerView != null && customerRecyclerView.getAdapter() != customerAdapter) customerRecyclerView.setAdapter(customerAdapter);
        customerAdapter.submit(customers);
    }

    private void loadPagedCustomers() {
        if (providerId == null || providerId.isEmpty()) return;
        Pager<DocumentSnapshot, Customer> pager = new Pager<>(new PagingConfig(Constants.PAGE_SIZE), () -> new CustomerPagingSource(firestore, providerId, Constants.PAGE_SIZE, "name"));
        if (pagedCustomersLiveData != null) pagedCustomersLiveData.removeObservers(this);
        pagedCustomersLiveData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), getLifecycle());
        pagedCustomersLiveData.observe(this, data -> pagedCustomerAdapter.submitData(getLifecycle(), data));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        if (customersListener != null) customersListener.remove();
        if (notifListener != null) notifListener.remove();
        binding = null;
    }
}
