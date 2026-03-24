package com.dailyserviceapp.maps;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RouteOptimizationActivity — helps the service provider plan their delivery route.
 *
 * FEATURES:
 *  • Loads all active customers from Firestore, grouped by area.
 *  • Shows a drag-and-drop list — the provider can reorder stops manually.
 *  • "Open in Maps" button builds a Google Maps URL with all addresses as waypoints
 *    and launches Google Maps (or any navigation app) for turn-by-turn directions.
 *  • "Sort by area" sorts alphabetically as a quick baseline route.
 *
 * IMPORTANT — Geocoding:
 *  This screen uses the customer's *address text* as a waypoint (Google Maps will
 *  geocode it automatically). If you want GPS coordinates instead, add lat/lng
 *  fields to the Customer model and use those in buildMapsUrl().
 *
 * SETUP:
 *  1. Add this activity to AndroidManifest.xml:
 *       <activity android:name=".maps.RouteOptimizationActivity"
 *                 android:exported="false"
 *                 android:theme="@style/Theme.DailyServiceApp.NoActionBar" />
 *
 *  2. Launch from DashboardActivity drawer or a FAB:
 *       startActivity(new Intent(this, RouteOptimizationActivity.class));
 *
 *  3. Add layout file: res/layout/activity_route_optimization.xml
 *     Required view IDs:
 *       @id/toolbar, @id/routeRecycler, @id/progressBar,
 *       @id/emptyState, @id/fabOpenMaps
 *
 *  4. Add layout file: res/layout/row_route_stop.xml
 *     Required view IDs:
 *       @id/stopNumber, @id/customerName, @id/customerAddress,
 *       @id/customerArea, @id/dragHandle
 */
public class RouteOptimizationActivity extends BaseActivity {

    private static final String TAG = "RouteOptimization";

    // Google Maps routing URL — supports up to ~23 waypoints free
    private static final String MAPS_DIRECTIONS_URL =
            "https://www.google.com/maps/dir/?api=1";

    // Views
    private RecyclerView      recyclerView;
    private ProgressBar       progressBar;
    private TextView          emptyState;
    private ExtendedFloatingActionButton fabOpenMaps;

    // Data
    private RouteAdapter adapter;
    private final List<Customer> routeStops = new ArrayList<>();

    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_optimization);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Delivery Route");
        }

        recyclerView = findViewById(R.id.routeRecycler);
        progressBar  = findViewById(R.id.progressBar);
        emptyState   = findViewById(R.id.emptyState);
        fabOpenMaps  = findViewById(R.id.fabOpenMaps);

        adapter = new RouteAdapter(routeStops);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Attach drag-and-drop so provider can reorder stops manually
        ItemTouchHelper touchHelper = new ItemTouchHelper(new DragCallback(adapter));
        touchHelper.attachToRecyclerView(recyclerView);
        adapter.setTouchHelper(touchHelper);

        fabOpenMaps.setOnClickListener(v -> openInGoogleMaps());

        loadCustomers();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Menu
    // ────────────────────────────────────────────────────────────────────────

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

    // ────────────────────────────────────────────────────────────────────────
    // Load data
    // ────────────────────────────────────────────────────────────────────────

    private void loadCustomers() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_CUSTOMERS)
                .whereEqualTo("providerId", user.getUid())
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("onVacation", false)
                .orderBy("area", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);
                    routeStops.clear();

                    for (var doc : snapshots.getDocuments()) {
                        Customer c = doc.toObject(Customer.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            routeStops.add(c);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    updateFabText();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to load customers for route", e);
                    Snackbar.make(recyclerView,
                            "Could not load customer list", Snackbar.LENGTH_LONG).show();
                });
    }

    // ────────────────────────────────────────────────────────────────────────
    // Sorting
    // ────────────────────────────────────────────────────────────────────────

    private void sortByArea() {
        Collections.sort(routeStops, (a, b) -> {
            String aArea = a.getArea() != null ? a.getArea() : "";
            String bArea = b.getArea() != null ? b.getArea() : "";
            int areaComp = aArea.compareToIgnoreCase(bArea);
            if (areaComp != 0) return areaComp;
            // Secondary sort by name within the same area
            String aName = a.getName() != null ? a.getName() : "";
            String bName = b.getName() != null ? b.getName() : "";
            return aName.compareToIgnoreCase(bName);
        });
        adapter.notifyDataSetChanged();
        Snackbar.make(recyclerView, "Sorted by area", Snackbar.LENGTH_SHORT).show();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Open Google Maps
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Builds a Google Maps multi-stop directions URL and opens it.
     *
     * URL format:
     *   https://www.google.com/maps/dir/?api=1&origin=ADDRESS&destination=ADDRESS&waypoints=A|B|C
     *
     * Google Maps free tier supports up to 23 waypoints.
     * If there are more customers, only the first 23 are added as waypoints
     * and the rest are shown in a Snackbar as a warning.
     */
    private void openInGoogleMaps() {
        if (routeStops.isEmpty()) {
            Snackbar.make(recyclerView, "No stops to navigate", Snackbar.LENGTH_SHORT).show();
            return;
        }

        int MAX_WAYPOINTS = 23;
        boolean truncated = routeStops.size() > MAX_WAYPOINTS + 2;

        List<Customer> stops = truncated
                ? routeStops.subList(0, MAX_WAYPOINTS + 2)
                : routeStops;

        String mapsUrl = buildMapsUrl(stops);
        if (mapsUrl == null) {
            Snackbar.make(recyclerView, "Customers have no address data", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (truncated) {
            Snackbar.make(recyclerView,
                    "Maps supports 25 stops — showing first " + (MAX_WAYPOINTS + 2),
                    Snackbar.LENGTH_LONG).show();
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)));
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Maps", e);
            Snackbar.make(recyclerView,
                    "Google Maps not available", Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Builds the multi-stop Google Maps URL.
     * First stop = origin, last stop = destination, middle stops = waypoints.
     */
    private String buildMapsUrl(List<Customer> stops) {
        List<String> addresses = new ArrayList<>();
        for (Customer c : stops) {
            if (c.getAddress() != null && !c.getAddress().isEmpty()) {
                addresses.add(c.getAddress());
            }
        }
        if (addresses.isEmpty()) return null;

        StringBuilder sb = new StringBuilder(MAPS_DIRECTIONS_URL);
        sb.append("&travelmode=driving");
        sb.append("&origin=").append(Uri.encode(addresses.get(0)));
        sb.append("&destination=").append(Uri.encode(addresses.get(addresses.size() - 1)));

        if (addresses.size() > 2) {
            StringBuilder waypoints = new StringBuilder();
            for (int i = 1; i < addresses.size() - 1; i++) {
                if (i > 1) waypoints.append("|");
                waypoints.append(Uri.encode(addresses.get(i)));
            }
            sb.append("&waypoints=").append(waypoints);
        }

        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ────────────────────────────────────────────────────────────────────────

    private void updateEmptyState() {
        boolean empty = routeStops.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE  : View.VISIBLE);
        if (empty) emptyState.setText("No active customers today");
    }

    private void updateFabText() {
        fabOpenMaps.setText(String.format(Locale.getDefault(),
                "Open in Maps (%d stops)", routeStops.size()));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RecyclerView Adapter
    // ════════════════════════════════════════════════════════════════════════

    private static class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.VH> {

        private final List<Customer> items;
        private ItemTouchHelper touchHelper;

        RouteAdapter(List<Customer> items) { this.items = items; }

        void setTouchHelper(ItemTouchHelper helper) { this.touchHelper = helper; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_route_stop, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Customer c = items.get(position);

            holder.stopNumber.setText(String.valueOf(position + 1));
            holder.customerName.setText(c.getName() != null ? c.getName() : "");
            holder.customerAddress.setText(c.getAddress() != null ? c.getAddress() : "No address");
            holder.customerArea.setText(c.getArea() != null ? c.getArea() : "");

            // Start drag on handle touch
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (touchHelper != null) touchHelper.startDrag(holder);
                return false;
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        /** Called by DragCallback to reorder stops */
        void onItemMove(int from, int to) {
            Collections.swap(items, from, to);
            notifyItemMoved(from, to);
            // Refresh stop numbers
            notifyItemRangeChanged(Math.min(from, to), Math.abs(from - to) + 1);
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView  stopNumber, customerName, customerAddress, customerArea;
            android.view.View dragHandle;

            VH(android.view.View v) {
                super(v);
                stopNumber      = v.findViewById(R.id.stopNumber);
                customerName    = v.findViewById(R.id.customerName);
                customerAddress = v.findViewById(R.id.customerAddress);
                customerArea    = v.findViewById(R.id.customerArea);
                dragHandle      = v.findViewById(R.id.dragHandle);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Drag & Drop callback
    // ════════════════════════════════════════════════════════════════════════

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
            // Swipe not used — do nothing
        }
    }
}
