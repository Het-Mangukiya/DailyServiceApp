package com.dailyserviceapp.maps;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.data.models.Customer;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * RouteOptimizationActivity — helps the service provider plan their delivery route.
 */
public class RouteOptimizationActivity extends BaseActivity {

    private static final String TAG = "RouteOptimization";
    private static final String MAPS_PACKAGE = "com.google.android.apps.maps";

    private static final String MAPS_DIRECTIONS_URL =
        "https://www.google.com/maps/dir/?api=1";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyStateContainer;
    private TextView emptyStateText;
    private TextView routeInfoText;
    private ExtendedFloatingActionButton fabOpenMaps;

    private RouteAdapter adapter;
    private final List<Customer> routeStops = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_optimization);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.delivery_route_title);
        }

        recyclerView = findViewById(R.id.routeRecycler);
        progressBar = findViewById(R.id.progressBar);
        emptyStateContainer = findViewById(R.id.emptyState);
        emptyStateText = findViewById(R.id.emptyStateText);
        routeInfoText = findViewById(R.id.routeInfoText);
        fabOpenMaps = findViewById(R.id.fabOpenMaps);

        adapter = new RouteAdapter(routeStops);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new DragCallback(adapter));
        touchHelper.attachToRecyclerView(recyclerView);
        adapter.setTouchHelper(touchHelper);

        fabOpenMaps.setOnClickListener(v -> openInGoogleMaps());

        loadCustomers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_route, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_sort_by_area) {
            sortByArea();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadCustomers() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Snackbar.make(findViewById(android.R.id.content),
                "Please log in again", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_CUSTOMERS)
            .whereEqualTo("providerId", user.getUid())
            .get()
            .addOnSuccessListener(snapshots -> {
                progressBar.setVisibility(View.GONE);
                routeStops.clear();

                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Customer c = doc.toObject(Customer.class);
                    if (c != null && isActiveAndAvailable(c)) {
                        c.setId(doc.getId());
                        routeStops.add(c);
                    }
                }

                sortByAreaInternal();
                adapter.notifyDataSetChanged();
                updateEmptyState();
                updateRouteInfo();
                updateFabText();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load customers for route", e);
                Snackbar.make(recyclerView,
                    "Could not load customer list", Snackbar.LENGTH_LONG).show();
            });
    }

    private void sortByArea() {
        sortByAreaInternal();
        adapter.notifyDataSetChanged();
        updateRouteInfo();
        Snackbar.make(recyclerView, "Sorted by area", Snackbar.LENGTH_SHORT).show();
    }

    private void sortByAreaInternal() {
        Collections.sort(routeStops, (a, b) -> {
            String aArea = normalize(a.getArea());
            String bArea = normalize(b.getArea());
            int areaComp = aArea.compareToIgnoreCase(bArea);
            if (areaComp != 0) return areaComp;
            String aName = normalize(a.getName());
            String bName = normalize(b.getName());
            return aName.compareToIgnoreCase(bName);
        });
    }

    private void openInGoogleMaps() {
        if (routeStops.isEmpty()) {
            Snackbar.make(recyclerView, "No stops to navigate", Snackbar.LENGTH_SHORT).show();
            return;
        }

        List<String> allStops = buildRouteStops(routeStops);
        if (allStops.isEmpty()) {
            Snackbar.make(recyclerView,
                "Customers need address or area for route", Snackbar.LENGTH_LONG).show();
            return;
        }

        int missingStops = routeStops.size() - allStops.size();
        if (missingStops > 0) {
            Snackbar.make(recyclerView,
                missingStops + " customers skipped (missing address/area)",
                Snackbar.LENGTH_LONG).show();
        }

        int maxWaypoints = 23;
        boolean truncated = allStops.size() > maxWaypoints + 2;

        List<String> stops = truncated
            ? allStops.subList(0, maxWaypoints + 2)
            : allStops;

        String mapsUrl = buildMapsUrl(stops);

        if (truncated) {
            Snackbar.make(recyclerView,
                "Maps supports 25 stops — showing first " + (maxWaypoints + 2),
                Snackbar.LENGTH_LONG).show();
        }

        try {
            Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl));
            if (isPackageAvailable(MAPS_PACKAGE)) {
                mapsIntent.setPackage(MAPS_PACKAGE);
            }
            startActivity(mapsIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Maps", e);
            Snackbar.make(recyclerView,
                "Google Maps not available", Snackbar.LENGTH_LONG).show();
        }
    }

    private List<String> buildRouteStops(List<Customer> customers) {
        List<String> stops = new ArrayList<>();
        for (Customer c : customers) {
            String routeAddress = getRouteAddress(c);
            if (!routeAddress.isEmpty()) {
                stops.add(routeAddress);
            }
        }
        return stops;
    }

    private String buildMapsUrl(List<String> routeStops) {
        if (routeStops.isEmpty()) return MAPS_DIRECTIONS_URL;

        StringBuilder sb = new StringBuilder(MAPS_DIRECTIONS_URL);
        sb.append("&travelmode=driving");
        sb.append("&origin=").append(Uri.encode(routeStops.get(0)));
        sb.append("&destination=").append(Uri.encode(routeStops.get(routeStops.size() - 1)));

        if (routeStops.size() > 2) {
            StringBuilder waypoints = new StringBuilder();
            for (int i = 1; i < routeStops.size() - 1; i++) {
                if (i > 1) waypoints.append("|");
                waypoints.append(Uri.encode(routeStops.get(i)));
            }
            sb.append("&waypoints=").append(waypoints);
        }

        return sb.toString();
    }

    private boolean isActiveAndAvailable(Customer customer) {
        String status = normalize(customer.getStatus());
        boolean active = status.isEmpty() || "ACTIVE".equalsIgnoreCase(status);
        return active && !customer.isOnVacation();
    }

    private static String getRouteAddress(Customer customer) {
        String address = normalize(customer.getAddress());
        String area = normalize(customer.getArea());
        if (!address.isEmpty() && !area.isEmpty() &&
            !address.toLowerCase(Locale.getDefault()).contains(area.toLowerCase(Locale.getDefault()))) {
            return address + ", " + area;
        }
        if (!address.isEmpty()) return address;
        return area;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isPackageAvailable(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void updateEmptyState() {
        boolean empty = routeStops.isEmpty();
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (emptyStateText != null) {
            emptyStateText.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty) emptyStateText.setText("No active customers today");
        }
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateFabText() {
        int routableStops = buildRouteStops(routeStops).size();
        fabOpenMaps.setText(String.format(Locale.getDefault(),
            "Open in Maps (%d stops)", routableStops));
        fabOpenMaps.setEnabled(routableStops > 0);
    }

    private void updateRouteInfo() {
        if (routeInfoText == null) return;

        int totalActive = routeStops.size();
        int routableStops = buildRouteStops(routeStops).size();
        int missingData = Math.max(0, totalActive - routableStops);

        String info = String.format(Locale.getDefault(),
            "Required: ACTIVE + not on vacation + address/area\n" +
            "Loaded: %d active, Routable: %d, Missing address/area: %d",
            totalActive,
            routableStops,
            missingData);
        routeInfoText.setText(info);
    }

    private static class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.VH> {

        private final List<Customer> items;
        private ItemTouchHelper touchHelper;

        RouteAdapter(List<Customer> items) { this.items = items; }

        void setTouchHelper(ItemTouchHelper helper) { this.touchHelper = helper; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_route_stop, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Customer c = items.get(position);

            holder.stopNumber.setText(String.valueOf(position + 1));
            holder.customerName.setText(c.getName() != null ? c.getName() : "");
            String routeAddress = getRouteAddress(c);
            holder.customerAddress.setText(routeAddress.isEmpty()
                ? holder.itemView.getContext().getString(R.string.delivery_route_address_missing)
                : routeAddress);
            holder.customerArea.setText(c.getArea() != null ? c.getArea() : "");

            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && touchHelper != null) {
                    touchHelper.startDrag(holder);
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return false;
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        void onItemMove(int from, int to) {
            Collections.swap(items, from, to);
            notifyItemMoved(from, to);
            notifyItemRangeChanged(Math.min(from, to), Math.abs(from - to) + 1);
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView stopNumber, customerName, customerAddress, customerArea;
            View dragHandle;

            VH(View v) {
                super(v);
                stopNumber = v.findViewById(R.id.stopNumber);
                customerName = v.findViewById(R.id.customerName);
                customerAddress = v.findViewById(R.id.customerAddress);
                customerArea = v.findViewById(R.id.customerArea);
                dragHandle = v.findViewById(R.id.dragHandle);
            }
        }
    }

    private static class DragCallback extends ItemTouchHelper.SimpleCallback {

        private final RouteAdapter adapter;

        DragCallback(RouteAdapter adapter) {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            this.adapter = adapter;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            adapter.onItemMove(
                viewHolder.getAdapterPosition(),
                target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // no-op
        }
    }
}
