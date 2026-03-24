package com.dailyserviceapp.provider;

import dagger.hilt.android.AndroidEntryPoint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.models.QuantityRequest;
import com.dailyserviceapp.databinding.ActivityQuantityRequestsBinding;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provider-side activity to view and approve/reject customer extra quantity requests.
 * Shows all PENDING requests for today with approve/reject actions.
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-03-23
 */
@AndroidEntryPoint
public class QuantityRequestsActivity extends BaseActivity {

    private static final String TAG = "QuantityRequests";

    private ActivityQuantityRequestsBinding binding;
    private FirebaseFirestore firestore;
    private String providerId;
    private final List<QuantityRequest> requests = new ArrayList<>();
    private RequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuantityRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }

        providerId = getCurrentUserId();
        if (providerId == null || providerId.trim().isEmpty()) {
            showToast("Session expired. Please login again.");
            navigateToLogin();
            return;
        }

        firestore = FirebaseFirestore.getInstance();

        setupToolbar(binding.toolbar, "Quantity Requests", true);
        setupRecyclerView();
        loadPendingRequests();
    }

    private void setupRecyclerView() {
        adapter = new RequestAdapter();
        binding.recyclerRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerRequests.setAdapter(adapter);
    }

    private void loadPendingRequests() {
        setLoading(true);
        firestore.collection(Constants.COLLECTION_QUANTITY_REQUESTS)
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("status", QuantityRequest.STATUS_PENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!isUiActive()) return;
                setLoading(false);
                requests.clear();

                if (querySnapshot != null) {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        QuantityRequest request = doc.toObject(QuantityRequest.class);
                        if (request != null) {
                            request.setId(doc.getId());
                            requests.add(request);
                        }
                    }
                }

                adapter.notifyDataSetChanged();
                updateEmptyState();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                Log.e(TAG, "Failed to load quantity requests", e);
                showToast("Failed to load requests");
                updateEmptyState();
            });
    }

    private void approveRequest(QuantityRequest request, int position) {
        if (request == null || request.getId() == null) return;

        firestore.collection(Constants.COLLECTION_QUANTITY_REQUESTS)
            .document(request.getId())
            .update(
                "status", QuantityRequest.STATUS_APPROVED,
                "respondedAt", Timestamp.now()
            )
            .addOnSuccessListener(unused -> {
                if (!isUiActive()) return;
                showToast("Request approved for " + safeTrim(request.getCustomerName()));
                requests.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, requests.size());
                updateEmptyState();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                Log.e(TAG, "Failed to approve request", e);
                showToast("Failed to approve request");
            });
    }

    private void rejectRequest(QuantityRequest request, int position) {
        if (request == null || request.getId() == null) return;

        firestore.collection(Constants.COLLECTION_QUANTITY_REQUESTS)
            .document(request.getId())
            .update(
                "status", QuantityRequest.STATUS_REJECTED,
                "respondedAt", Timestamp.now()
            )
            .addOnSuccessListener(unused -> {
                if (!isUiActive()) return;
                showToast("Request rejected");
                requests.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, requests.size());
                updateEmptyState();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                Log.e(TAG, "Failed to reject request", e);
                showToast("Failed to reject request");
            });
    }

    private void updateEmptyState() {
        if (!isUiActive()) return;
        binding.txtEmptyState.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerRequests.setVisibility(requests.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        if (!isUiActive()) return;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    // ─── RecyclerView Adapter ────────────────────────────────────────────

    private class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quantity_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QuantityRequest request = requests.get(position);

            // Customer name & initial
            String name = safeTrim(request.getCustomerName());
            if (name.isEmpty()) name = "Customer";
            holder.txtCustomerName.setText(name);
            holder.txtCustomerInitial.setText(name.substring(0, 1).toUpperCase(Locale.US));

            // Service type badge
            String serviceType = safeTrim(request.getServiceType());
            if (serviceType.isEmpty()) serviceType = "Service";
            holder.txtServiceType.setText(serviceType);

            // Quantity display: current, requested, extra
            holder.txtCurrentQty.setText(String.format(Locale.US, "%.1f", request.getCurrentQuantity()));
            holder.txtRequestedQty.setText(String.format(Locale.US, "%.1f", request.getRequestedQuantity()));
            holder.txtExtraQty.setText(String.format(Locale.US, "+%.1f", request.getExtraQuantity()));

            // Request date
            String dateStr = request.getRequestDate() != null
                ? DateUtils.formatShortDate(request.getRequestDate().toDate())
                : "Today";
            holder.txtRequestDate.setText("For: " + dateStr);

            // Timestamp (relative)
            if (request.getCreatedAt() != null) {
                long diffMs = System.currentTimeMillis() - request.getCreatedAt().toDate().getTime();
                long diffMin = diffMs / (1000 * 60);
                String timeAgo;
                if (diffMin < 1) timeAgo = "Requested just now";
                else if (diffMin < 60) timeAgo = "Requested " + diffMin + " min ago";
                else if (diffMin < 1440) timeAgo = "Requested " + (diffMin / 60) + "h ago";
                else timeAgo = "Requested " + (diffMin / 1440) + "d ago";
                holder.txtTimestamp.setText(timeAgo);
                holder.txtTimestamp.setVisibility(View.VISIBLE);
            } else {
                holder.txtTimestamp.setVisibility(View.GONE);
            }

            // Note
            String note = safeTrim(request.getNote());
            if (!note.isEmpty()) {
                holder.cardNote.setVisibility(View.VISIBLE);
                holder.txtNote.setText("\"" + note + "\"");
            } else {
                holder.cardNote.setVisibility(View.GONE);
            }

            // Action buttons
            holder.btnApprove.setOnClickListener(v ->
                approveRequest(request, holder.getAdapterPosition()));
            holder.btnReject.setOnClickListener(v ->
                rejectRequest(request, holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtCustomerInitial, txtCustomerName, txtServiceType;
            TextView txtCurrentQty, txtRequestedQty, txtExtraQty;
            TextView txtRequestDate, txtTimestamp, txtNote;
            View cardNote;
            MaterialButton btnApprove, btnReject;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtCustomerInitial = itemView.findViewById(R.id.txtCustomerInitial);
                txtCustomerName = itemView.findViewById(R.id.txtCustomerName);
                txtServiceType = itemView.findViewById(R.id.txtServiceType);
                txtCurrentQty = itemView.findViewById(R.id.txtCurrentQty);
                txtRequestedQty = itemView.findViewById(R.id.txtRequestedQty);
                txtExtraQty = itemView.findViewById(R.id.txtExtraQty);
                txtRequestDate = itemView.findViewById(R.id.txtRequestDate);
                txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
                txtNote = itemView.findViewById(R.id.txtNote);
                cardNote = itemView.findViewById(R.id.cardNote);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}

