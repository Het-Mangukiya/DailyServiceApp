package com.dailyserviceapp.products;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.data.models.Product;
import com.dailyserviceapp.profile.ProfileActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Products Activity for managing inventory/products.
 * Features drawer navigation, product listing, search, and add product functionality.
 * 
 * @author DailyDrop Team
 * @version 1.0
 */
public class ProductsActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TextView productsCountText;
    private TextView totalValueText;
    private EditText searchEditText;
    private RecyclerView productsRecyclerView;
    private MaterialButton addProductFab;
    private View emptyStateView;

    private ProductAdapter productAdapter;
    private List<Product> productList;
    private List<Product> filteredProductList;

    private FirebaseFirestore firestore;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }

        currentUserId = getCurrentUserId();
        firestore = FirebaseFirestore.getInstance();

        initializeViews();
        setupDrawer();
        setupRecyclerView();
        setupSearch();
        setupClickListeners();
        loadProducts();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        productsCountText = findViewById(R.id.productsCountText);
        totalValueText = findViewById(R.id.totalValueText);
        searchEditText = findViewById(R.id.searchEditText);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        addProductFab = findViewById(R.id.addProductFab);
        emptyStateView = findViewById(R.id.emptyStateView);

        // Setup toolbar (hidden, used for drawer toggle)
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Products");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Setup menu icon click to open drawer
        ImageView menuIcon = findViewById(R.id.menuIcon);
        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // Setup navigation view header
        setupNavigationHeader();
    }

    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView userNameText = headerView.findViewById(R.id.navUserName);
        TextView userEmailText = headerView.findViewById(R.id.navUserEmail);

        String userName = preferenceManager.getUserName();
        String userEmail = preferenceManager.getUserEmail();

        if (userName != null && !userName.isEmpty()) {
            userNameText.setText(userName.toUpperCase());
        }
        if (userEmail != null && !userEmail.isEmpty()) {
            userEmailText.setText(userEmail);
        }

        // Style logout menu item in red
        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.nav_logout);
        if (logoutItem != null) {
            logoutItem.setTitle(logoutItem.getTitle());
            // The icon and text color will be styled via theme or programmatically
        }
    }

    private void setupRecyclerView() {
        productList = new ArrayList<>();
        filteredProductList = new ArrayList<>();
        productAdapter = new ProductAdapter(filteredProductList, this::onProductClick);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(productAdapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        addProductFab.setOnClickListener(v -> showAddProductDialog());

        // Header icons
        ImageView bellIcon = findViewById(R.id.bellIcon);
        ImageView calendarIcon = findViewById(R.id.calendarIcon);
        ImageView sparkleIcon = findViewById(R.id.sparkleIcon);
        ImageView gridIcon = findViewById(R.id.gridIcon);

        if (bellIcon != null) {
            bellIcon.setOnClickListener(v -> {
                // TODO: Open notifications
                showToast("Notifications");
            });
        }
        if (calendarIcon != null) {
            calendarIcon.setOnClickListener(v -> {
                // TODO: Open calendar
                showToast("Calendar");
            });
        }
        if (sparkleIcon != null) {
            sparkleIcon.setOnClickListener(v -> {
                // TODO: Open features
                showToast("Features");
            });
        }
        if (gridIcon != null) {
            gridIcon.setOnClickListener(v -> {
                // TODO: Toggle grid/list view
                showToast("View Options");
            });
        }
    }

    private void loadProducts() {
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("providerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        productList.add(product);
                    }
                    filterProducts("");
                    updateSummary();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load products");
                });
    }

    private void filterProducts(String query) {
        filteredProductList.clear();
        if (query.isEmpty()) {
            filteredProductList.addAll(productList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Product product : productList) {
                if (product.getName().toLowerCase().contains(lowerQuery) ||
                    (product.getCategory() != null && product.getCategory().toLowerCase().contains(lowerQuery))) {
                    filteredProductList.add(product);
                }
            }
        }
        productAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateSummary() {
        int count = productList.size();
        double totalValue = 0;
        for (Product product : productList) {
            totalValue += product.getTotalValue();
        }

        productsCountText.setText(count + " Products");
        totalValueText.setText("₹" + String.format("%.0f", totalValue) + " Total Value");
    }

    private void updateEmptyState() {
        if (filteredProductList.isEmpty()) {
            productsRecyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            productsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void onProductClick(Product product) {
        // TODO: Open product detail/edit screen
        showToast("Product: " + product.getName());
    }

    private void showAddProductDialog() {
        AddProductDialog dialog = new AddProductDialog(this, (name, category, price, quantity, description) -> {
            Product product = new Product(name, category, price, quantity, description, currentUserId);
            addProduct(product);
        });
        dialog.show();
    }

    private void addProduct(Product product) {
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .add(product)
                .addOnSuccessListener(documentReference -> {
                    product.setId(documentReference.getId());
                    productList.add(product);
                    filterProducts(searchEditText.getText().toString());
                    updateSummary();
                    showToast("Product added successfully");
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to add product");
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_requests) {
            showToast("My Requests");
        } else if (id == R.id.nav_calendar) {
            showToast("Future Orders Calendar");
        } else if (id == R.id.nav_deleted_history) {
            showToast("Deleted History");
        } else if (id == R.id.nav_share) {
            shareApp();
        } else if (id == R.id.nav_rate_us) {
            showToast("Rate Us");
        } else if (id == R.id.nav_terms) {
            showToast("Terms & Conditions");
        } else if (id == R.id.nav_privacy) {
            showToast("Privacy Policy");
        } else if (id == R.id.nav_language) {
            // Language selector - could show a dialog or navigate to settings
            showToast("Language: English");
        } else if (id == R.id.nav_logout) {
            // Show confirmation dialog before logout
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> logout())
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out DailyDrop app!");
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        preferenceManager.clearAllData();
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
}

