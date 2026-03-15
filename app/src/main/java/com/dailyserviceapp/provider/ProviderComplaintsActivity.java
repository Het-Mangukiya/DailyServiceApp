package com.dailyserviceapp.provider;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.databinding.ActivityProviderComplaintsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provider-facing complaints inbox and detail viewer.
 */
public class ProviderComplaintsActivity extends BaseActivity {

    private ActivityProviderComplaintsBinding binding;
    private FirebaseFirestore firestore;
    private String providerId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProviderComplaintsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }

        if (!isProvider()) {
            showToast("Complaints are available for providers only");
            finish();
            return;
        }

        providerId = getCurrentUserId();
        if (providerId == null || providerId.trim().isEmpty()) {
            showToast("Session expired. Please login again.");
            navigateToLogin();
            return;
        }

        firestore = FirebaseFirestore.getInstance();
        setupToolbar(binding.toolbar, getString(R.string.provider_complaints_title), true);

        binding.btnRefresh.setOnClickListener(v -> loadComplaints());
        loadComplaints();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadComplaints();
    }

    private void loadComplaints() {
        setLoading(true);
        firestore.collection(Constants.COLLECTION_SUPPORT_TICKETS)
            .whereEqualTo("providerId", providerId)
            .limit(250)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (!isUiActive()) return;
                setLoading(false);

                List<DocumentSnapshot> docs = snapshot != null ? snapshot.getDocuments() : new ArrayList<>();
                docs.sort((d1, d2) -> {
                    Timestamp t1 = d1.getTimestamp("createdAt");
                    Timestamp t2 = d2.getTimestamp("createdAt");
                    Date v1 = t1 != null ? t1.toDate() : new Date(0);
                    Date v2 = t2 != null ? t2.toDate() : new Date(0);
                    return v2.compareTo(v1);
                });

                renderComplaints(docs);
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                showToast("Failed to load complaints: " + e.getMessage());
                renderComplaints(new ArrayList<>());
            });
    }

    private void renderComplaints(List<DocumentSnapshot> docs) {
        binding.complaintsContainer.removeAllViews();

        int openCount = 0;
        int inProgressCount = 0;
        int resolvedCount = 0;

        if (docs == null || docs.isEmpty()) {
            binding.txtNoComplaints.setVisibility(View.VISIBLE);
            updateSummary(openCount, inProgressCount, resolvedCount);
            return;
        }

        binding.txtNoComplaints.setVisibility(View.GONE);
        int limit = Math.min(50, docs.size());

        for (int i = 0; i < limit; i++) {
            DocumentSnapshot doc = docs.get(i);

            String status = safe(doc.getString("status")).toUpperCase(Locale.US);
            if (status.isEmpty()) status = "OPEN";

            if ("RESOLVED".equals(status)) {
                resolvedCount++;
            } else if ("IN_PROGRESS".equals(status)) {
                inProgressCount++;
            } else {
                openCount++;
            }

            View row = getLayoutInflater().inflate(R.layout.row_customer_history, binding.complaintsContainer, false);

            String customerName = safe(doc.getString("customerName"));
            if (customerName.isEmpty()) {
                customerName = "Customer " + shortId(doc.getString("customerId"));
            }
            String category = safe(doc.getString("category"));
            if (category.isEmpty()) category = "General";

            String subject = safe(doc.getString("subject"));
            if (subject.isEmpty()) subject = "Support Ticket";
            String message = safe(doc.getString("message"));

            Timestamp createdAt = doc.getTimestamp("createdAt");
            String createdText = createdAt != null ? DateUtils.formatShortDate(createdAt.toDate()) : "-";

            TextView title = row.findViewById(R.id.txtHistoryTitle);
            TextView subtitle = row.findViewById(R.id.txtHistorySubtitle);
            TextView value = row.findViewById(R.id.txtHistoryValue);

            title.setText(subject + " • " + customerName);

            String subtitleText = category + " • " + createdText;
            if (!message.isEmpty()) {
                subtitleText += "\n" + message;
            }
            subtitle.setText(subtitleText);

            value.setText(status.replace("_", " "));
            value.setTextColor(resolveStatusColor(status));

            DocumentSnapshot ticketDoc = doc;
            row.setOnClickListener(v -> showComplaintDetail(ticketDoc));

            binding.complaintsContainer.addView(row);
        }

        updateSummary(openCount, inProgressCount, resolvedCount);
    }

    private void showComplaintDetail(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return;

        String subject = safe(doc.getString("subject"));
        if (subject.isEmpty()) subject = "Support Ticket";

        String customerName = safe(doc.getString("customerName"));
        if (customerName.isEmpty()) customerName = "-";

        String customerId = safe(doc.getString("customerId"));
        String category = safe(doc.getString("category"));
        if (category.isEmpty()) category = "General";

        String message = safe(doc.getString("message"));
        if (message.isEmpty()) message = "No details provided";

        String status = safe(doc.getString("status")).toUpperCase(Locale.US);
        if (status.isEmpty()) status = "OPEN";

        Timestamp createdAt = doc.getTimestamp("createdAt");
        Timestamp updatedAt = doc.getTimestamp("updatedAt");

        String createdText = createdAt != null ? DateUtils.formatFullDate(createdAt.toDate()) : "-";
        String updatedText = updatedAt != null ? DateUtils.formatFullDate(updatedAt.toDate()) : "-";

        String detail = "Customer: " + customerName + "\n"
            + "Customer ID: " + (customerId.isEmpty() ? "-" : customerId) + "\n"
            + "Reason: " + category + "\n"
            + "Status: " + status.replace("_", " ") + "\n"
            + "Registered At: " + createdText + "\n"
            + "Last Updated: " + updatedText + "\n\n"
            + "Complaint Description:\n" + message;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle(subject)
            .setMessage(detail)
            .setNegativeButton("Close", null);

        String ticketId = doc.getId();
        if (!"IN_PROGRESS".equals(status) && !"RESOLVED".equals(status)) {
            builder.setNeutralButton(getString(R.string.provider_complaints_mark_in_progress),
                (dialog, which) -> updateTicketStatus(ticketId, "IN_PROGRESS"));
        }

        if (!"RESOLVED".equals(status)) {
            builder.setPositiveButton(getString(R.string.provider_complaints_mark_resolved),
                (dialog, which) -> updateTicketStatus(ticketId, "RESOLVED"));
        }

        builder.show();
    }

    private void updateTicketStatus(String ticketId, String newStatus) {
        if (ticketId == null || ticketId.trim().isEmpty()) return;

        setLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", Timestamp.now());
        if ("RESOLVED".equals(newStatus)) {
            updates.put("resolvedAt", Timestamp.now());
        }

        firestore.collection(Constants.COLLECTION_SUPPORT_TICKETS)
            .document(ticketId)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener(unused -> {
                if (!isUiActive()) return;
                showToast("Complaint updated: " + newStatus.replace("_", " "));
                loadComplaints();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                showToast("Failed to update complaint: " + e.getMessage());
            });
    }

    private int resolveStatusColor(String status) {
        if ("RESOLVED".equals(status)) {
            return ContextCompat.getColor(this, R.color.success);
        }
        if ("IN_PROGRESS".equals(status)) {
            return ContextCompat.getColor(this, R.color.md_theme_primary_dark);
        }
        return ContextCompat.getColor(this, R.color.md_theme_error);
    }

    private void updateSummary(int openCount, int inProgressCount, int resolvedCount) {
        binding.txtOpenCount.setText(String.valueOf(openCount));
        binding.txtInProgressCount.setText(String.valueOf(inProgressCount));
        binding.txtResolvedCount.setText(String.valueOf(resolvedCount));
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRefresh.setEnabled(!loading);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String shortId(String value) {
        String trimmed = safe(value);
        if (trimmed.isEmpty()) return "-";
        if (trimmed.length() <= 6) return trimmed.toUpperCase(Locale.US);
        return trimmed.substring(0, 6).toUpperCase(Locale.US);
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
