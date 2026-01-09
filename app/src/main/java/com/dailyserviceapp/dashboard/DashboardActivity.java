package com.dailyserviceapp.dashboard;

import android.content.Intent;
import android.os.Bundle;
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

import com.dailyserviceapp.R;
import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.billing.BillListActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.payment.PaymentActivity;
import com.dailyserviceapp.profile.ProfileActivity;
import com.dailyserviceapp.reports.ReportsActivity;
import com.dailyserviceapp.service.ServiceEntryActivity;
import com.dailyserviceapp.ui.CustomerAdapter;
import com.dailyserviceapp.ui.CustomerEditActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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
    
    private TextView totalCustomersCount, totalRevenueAmount;
    private RecyclerView customerRecyclerView;
    private CustomerAdapter customerAdapter;
    private LinearLayout emptyState;
    private EditText searchEditText;
    private FloatingActionButton addCustomerFab;
    
    private FirebaseFirestore firestore;
    private String providerId;
    private List<Customer> allCustomers = new ArrayList<>();
    private List<Customer> filteredCustomers = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        providerId = getCurrentUserId();
        firestore = FirebaseFirestore.getInstance();
        
        initializeViews();
        setupNavigationDrawer();
        setupRecyclerView();
        setupListeners();
        loadData();
    }
    
    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        
        totalCustomersCount = findViewById(R.id.totalCustomersCount);
        totalRevenueAmount = findViewById(R.id.totalRevenueAmount);
        customerRecyclerView = findViewById(R.id.customerRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        searchEditText = findViewById(R.id.searchEditText);
        addCustomerFab = findViewById(R.id.addCustomerFab);
        
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
        
        // Set user info in navigation header
        View headerView = navigationView.getHeaderView(0);
        TextView userName = headerView.findViewById(R.id.userName);
        TextView userEmail = headerView.findViewById(R.id.userEmail);
        TextView userInitial = headerView.findViewById(R.id.userInitial);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            
            userName.setText(name != null ? name : "User");
            userEmail.setText(email != null ? email : "user@example.com");
            
            if (name != null && !name.isEmpty()) {
                userInitial.setText(name.substring(0, 1).toUpperCase());
            }
        }
    }
    
    private void setupRecyclerView() {
        customerAdapter = new CustomerAdapter(customer -> {
            // Handle customer click - navigate to detail or edit
            Intent intent = new Intent(this, CustomerEditActivity.class);
            intent.putExtra("customerId", customer.getId());
            startActivity(intent);
        });
        customerAdapter.submit(filteredCustomers);
        customerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        customerRecyclerView.setAdapter(customerAdapter);
    }
    
    private void setupListeners() {
        addCustomerFab.setOnClickListener(v -> {
            startActivity(new Intent(this, CustomerEditActivity.class));
        });
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCustomers(s.toString());
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
        firestore.collection("customers")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                allCustomers.clear();
                filteredCustomers.clear();
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Customer customer = document.toObject(Customer.class);
                    customer.setId(document.getId());
                    allCustomers.add(customer);
                    filteredCustomers.add(customer);
                }
                
                customerAdapter.notifyDataSetChanged();
                updateEmptyState();
                
                // Load analytics after customers are loaded to avoid race condition
                loadAnalytics();
            })
            .addOnFailureListener(e -> {
                showToast("Failed to load customers: " + e.getMessage());
            });
    }
    
    private void loadAnalytics() {
        // Load total customers
        totalCustomersCount.setText(String.valueOf(allCustomers.size()));
        
        // Load monthly revenue from bills
        firestore.collection("bills")
            .whereEqualTo("providerId", providerId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                double totalRevenue = 0;
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Double totalAmount = document.getDouble("totalAmount");
                    if (totalAmount != null) {
                        totalRevenue += totalAmount;
                    }
                }
                totalRevenueAmount.setText(CurrencyUtils.formatIndianCurrency(totalRevenue));
            })
            .addOnFailureListener(e -> {
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
        
        customerAdapter.notifyDataSetChanged();
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
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_customers) {
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
        FirebaseAuth.getInstance().signOut();
        navigateToLogin();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
    protected void onResume() {
        super.onResume();
        loadData(); // Refresh data when returning to dashboard
    }
}
