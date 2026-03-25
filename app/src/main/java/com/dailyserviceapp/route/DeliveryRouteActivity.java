package com.dailyserviceapp.route;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.data.models.Customer;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DeliveryRouteActivity extends BaseActivity {

    private final List<Customer> routeStops = new ArrayList<>();

    private FirebaseFirestore firestore;
    private RouteAdapter adapter;
    private TextView txtStopCount;
    private ProgressBar progressBar;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_route);

        firestore = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, getString(R.string.delivery_route_title), true);
        toolbar.setNavigationOnClickListener(v -> finish());

        txtStopCount = findViewById(R.id.txtStopCount);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        MaterialButton btnOpenFirstStop = findViewById(R.id.btnOpenFirstStop);
        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        adapter = new RouteAdapter(routeStops);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnOpenFirstStop.setOnClickListener(v -> {
            if (routeStops.isEmpty()) {
                showToast(getString(R.string.delivery_route_empty_title));
                return;
            }
            openInMaps(routeStops.get(0));
        });

        btnRefresh.setOnClickListener(v -> loadRoute());

        loadRoute();
    }

    private void loadRoute() {
        String providerId = getCurrentUserId();
        if (providerId == null || providerId.trim().isEmpty()) {
            showToast("Please login again.");
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        firestore.collection("customers")
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routeStops.clear();
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Customer customer = snapshot.toObject(Customer.class);
                        customer.setId(snapshot.getId());

                        if (!isDeliverable(customer)) {
                            continue;
                        }
                        routeStops.add(customer);
                    }

                    routeStops.sort(
                            Comparator.comparing(this::areaValue, String.CASE_INSENSITIVE_ORDER)
                                    .thenComparing(this::addressValue, String.CASE_INSENSITIVE_ORDER)
                                    .thenComparing(this::nameValue, String.CASE_INSENSITIVE_ORDER)
                    );

                    progressBar.setVisibility(View.GONE);
                    emptyState.setVisibility(routeStops.isEmpty() ? View.VISIBLE : View.GONE);
                    txtStopCount.setText(getString(R.string.delivery_route_active_count, routeStops.size()));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyState.setVisibility(routeStops.isEmpty() ? View.VISIBLE : View.GONE);
                    showLongToast("Failed to load route: " + e.getMessage());
                });
    }

    private boolean isDeliverable(Customer customer) {
        if (customer == null) {
            return false;
        }
        String status = customer.getStatus();
        boolean active = status == null || status.trim().isEmpty() || "ACTIVE".equalsIgnoreCase(status);
        return active && !customer.isOnVacation();
    }

    private String areaValue(Customer customer) {
        String area = customer.getArea();
        return TextUtils.isEmpty(area) ? getString(R.string.delivery_route_area_fallback) : area.trim();
    }

    private String addressValue(Customer customer) {
        String address = customer.getAddress();
        return TextUtils.isEmpty(address) ? "zzz" : address.trim();
    }

    private String nameValue(Customer customer) {
        String name = customer.getName();
        return TextUtils.isEmpty(name) ? "" : name.trim();
    }

    private void openInMaps(Customer customer) {
        String address = customer.getAddress();
        if (TextUtils.isEmpty(address)) {
            showToast(getString(R.string.delivery_route_maps_missing));
            return;
        }

        String query = customer.getName() + ", " + address;
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(fallbackIntent);
        }
    }

    private void callCustomer(Customer customer) {
        String phone = customer.getPhone();
        if (TextUtils.isEmpty(phone)) {
            showToast("Phone number not available");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    private final class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

        private final List<Customer> items;

        private RouteAdapter(List<Customer> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_delivery_route_stop, parent, false);
            return new RouteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
            Customer customer = items.get(position);
            holder.txtStopNumber.setText(getString(R.string.delivery_route_stop_number, position + 1));
            holder.txtCustomerName.setText(nameValue(customer));

            String serviceType = TextUtils.isEmpty(customer.getServiceType()) ? "Service" : customer.getServiceType();
            holder.txtServiceLine.setText(String.format(
                    Locale.getDefault(),
                    "%s - %s",
                    areaValue(customer),
                    serviceType
            ));

            holder.txtAddress.setText(TextUtils.isEmpty(customer.getAddress())
                    ? getString(R.string.delivery_route_address_missing)
                    : customer.getAddress());

            holder.btnMaps.setOnClickListener(v -> openInMaps(customer));
            holder.btnCall.setOnClickListener(v -> callCustomer(customer));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class RouteViewHolder extends RecyclerView.ViewHolder {
            private final TextView txtStopNumber;
            private final TextView txtCustomerName;
            private final TextView txtServiceLine;
            private final TextView txtAddress;
            private final MaterialButton btnMaps;
            private final MaterialButton btnCall;

            private RouteViewHolder(@NonNull View itemView) {
                super(itemView);
                txtStopNumber = itemView.findViewById(R.id.txtStopNumber);
                txtCustomerName = itemView.findViewById(R.id.txtCustomerName);
                txtServiceLine = itemView.findViewById(R.id.txtServiceLine);
                txtAddress = itemView.findViewById(R.id.txtAddress);
                btnMaps = itemView.findViewById(R.id.btnMaps);
                btnCall = itemView.findViewById(R.id.btnCall);
            }
        }
    }
}
