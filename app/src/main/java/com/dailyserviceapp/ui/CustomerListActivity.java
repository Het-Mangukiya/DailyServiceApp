package com.dailyserviceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.data.Customer;
import com.dailyserviceapp.data.FirestoreRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CustomerListActivity extends AppCompatActivity {

    private FirestoreRepository repo;
    private CustomerAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);

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
        repo.listCustomers(
                snapshot -> {
                    List<Customer> customers = snapshot.toObjects(Customer.class);
                    adapter.submit(customers);
                },
                e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }
}
