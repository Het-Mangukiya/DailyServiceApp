package com.dailyserviceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Customer List Activity displaying all customers for a provider.
 * Shows a scrollable list of customers with click-to-view-details functionality.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>RecyclerView displaying all provider's customers</li>
 *   <li>Click on customer to view details</li>
 *   <li>Floating Action Button to add new customers</li>
 *   <li>Toolbar with back navigation</li>
 *   <li>Real-time data loading from Firestore</li>
 * </ul>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class CustomerListActivity extends BaseActivity {

    /** Firestore repository for data operations */
    private FirestoreRepository repo;
    
    /** RecyclerView adapter for customer list */
    private CustomerAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, "Customers", true);

        repo = new FirestoreRepository();

        RecyclerView recycler = findViewById(R.id.customerRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CustomerAdapter(customer -> {
            Intent intent = new Intent(this, CustomerDetailActivity.class);
            intent.putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customer.getId());
            startActivity(intent);
        });
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.addCustomerFab);
        fab.setOnClickListener(v -> startActivity(new Intent(this, CustomerEditActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers();
    }

    private void loadCustomers() {
        String providerId = getCurrentUserId();
        
        repo.listCustomers(
                snapshot -> {
                    List<Customer> customers = snapshot.toObjects(Customer.class);
                    adapter.submit(customers);
                },
                e -> showToast(e.getMessage())
        );
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.customer_list_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            // TODO: Implement search
            showToast("Search coming soon");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
