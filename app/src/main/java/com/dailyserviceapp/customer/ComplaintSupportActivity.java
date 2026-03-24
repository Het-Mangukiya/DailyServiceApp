package com.dailyserviceapp.customer;

import dagger.hilt.android.AndroidEntryPoint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.databinding.ActivityComplaintSupportBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Customer complaint and support ticket screen.
 */
@AndroidEntryPoint
public class ComplaintSupportActivity extends BaseActivity {

    private ActivityComplaintSupportBinding binding;
    private FirebaseFirestore firestore;

    private String customerId;
    private String providerId;
    private String providerName;
    private String providerEmail;
    private String customerEmail;

    private Spinner spinnerCategory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComplaintSupportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }

        if (!isCustomer()) {
            showToast("This screen is for customers only");
            finish();
            return;
        }

        customerId = getCurrentUserId();
        if (customerId == null || customerId.trim().isEmpty()) {
            showToast("Session expired. Please login again.");
            navigateToLogin();
            return;
        }

        firestore = FirebaseFirestore.getInstance();

        setupToolbar(binding.toolbar, "Complaint & Support", true);
        spinnerCategory = binding.spinnerCategory;
        setupCategorySpinner();

        binding.btnSubmitTicket.setOnClickListener(v -> submitTicket());
        binding.btnEmailProvider.setOnClickListener(v -> emailProvider(null, null, null));
        updateSupportAvailability(false);

        loadActiveLinkAndTickets();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.customer_home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_more) {
            showMoreMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMoreMenu() {
        PopupMenu popupMenu = new PopupMenu(this, binding.toolbar, Gravity.END);
        popupMenu.getMenuInflater().inflate(R.menu.customer_home_more_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this::handleToolbarMenuClick);
        popupMenu.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActiveLinkAndTickets();
    }

    private boolean handleToolbarMenuClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_support) {
            return true;
        }
        if (itemId == R.id.action_logout) {
            performLogout();
            return true;
        }
        return false;
    }

    private void setupCategorySpinner() {
        List<String> items = new ArrayList<>();
        items.add("Delivery Issue");
        items.add("Billing Issue");
        items.add("Payment Issue");
        items.add("App Issue");
        items.add("Other");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            items
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void loadActiveLinkAndTickets() {
        setLoading(true);
        customerEmail = safe(preferenceManager.getUserEmail());
        firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .document(customerId)
            .get()
            .addOnSuccessListener(doc -> {
                if (!isUiActive()) return;

                if (doc == null || !doc.exists()) {
                    setLoading(false);
                    providerId = "";
                    providerName = "";
                    providerEmail = "";
                    binding.txtProviderInfo.setText("No active provider link found.");
                    updateSupportAvailability(false);
                    showEmptyState(true, "Connect a provider first to raise support tickets.");
                    return;
                }

                String status = safe(doc.getString("status")).toUpperCase(Locale.US);
                if (!"ACTIVE".equals(status)) {
                    setLoading(false);
                    providerId = "";
                    providerName = "";
                    providerEmail = "";
                    binding.txtProviderInfo.setText("Link status: " + (status.isEmpty() ? "PENDING" : status));
                    updateSupportAvailability(false);
                    showEmptyState(true, "Your provider link must be ACTIVE to submit tickets.");
                    return;
                }

                providerId = safe(doc.getString("providerId"));
                providerName = safe(doc.getString("providerName"));
                if (providerName.isEmpty()) {
                    providerName = "Service Provider";
                }

                loadProviderContactAndTickets();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                updateSupportAvailability(false);
                showToast("Failed to load link: " + e.getMessage());
                showEmptyState(true, "Unable to load support tickets.");
            });
    }

    private void loadProviderContactAndTickets() {
        if (providerId == null || providerId.isEmpty()) {
            providerEmail = "";
            binding.txtProviderInfo.setText("Linked to: " + providerName);
            updateProviderEmailButton();
            updateSupportAvailability(false);
            loadTickets();
            return;
        }

        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(providerId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isUiActive()) return;
                providerEmail = documentSnapshot != null ? safe(documentSnapshot.getString("email")) : "";
                if (!providerEmail.isEmpty()) {
                    renderProviderInfoAndLoadTickets();
                    return;
                }

                firestore.collection(Constants.COLLECTION_USERS)
                    .document(providerId)
                    .get()
                    .addOnSuccessListener(userSnapshot -> {
                        if (!isUiActive()) return;
                        providerEmail = userSnapshot != null ? safe(userSnapshot.getString("email")) : "";
                        renderProviderInfoAndLoadTickets();
                    })
                    .addOnFailureListener(e -> {
                        if (!isUiActive()) return;
                        renderProviderInfoAndLoadTickets();
                    });
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                providerEmail = "";
                renderProviderInfoAndLoadTickets();
            });
    }

    private void renderProviderInfoAndLoadTickets() {
        String info = "Linked to: " + providerName;
        if (!providerEmail.isEmpty()) {
            info += "\nEmail: " + providerEmail;
        }
        binding.txtProviderInfo.setText(info);
        updateProviderEmailButton();
        updateSupportAvailability(true);
        loadTickets();
    }

    private void loadTickets() {
        firestore.collection(Constants.COLLECTION_SUPPORT_TICKETS)
            .whereEqualTo("customerId", customerId)
            .limit(100)
            .get()
            .addOnSuccessListener(query -> {
                if (!isUiActive()) return;
                setLoading(false);

                List<DocumentSnapshot> docs = query != null ? query.getDocuments() : new ArrayList<>();
                docs.sort((d1, d2) -> {
                    Timestamp t1 = d1.getTimestamp("createdAt");
                    Timestamp t2 = d2.getTimestamp("createdAt");
                    Date v1 = t1 != null ? t1.toDate() : new Date(0);
                    Date v2 = t2 != null ? t2.toDate() : new Date(0);
                    return v2.compareTo(v1);
                });

                renderTicketRows(docs);
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                showToast("Failed to load tickets: " + e.getMessage());
                showEmptyState(true, "No support tickets found.");
            });
    }

    private void renderTicketRows(List<DocumentSnapshot> docs) {
        binding.ticketListContainer.removeAllViews();

        if (docs == null || docs.isEmpty()) {
            showEmptyState(true, "No complaints yet. Submit your first ticket below.");
            return;
        }

        showEmptyState(false, "");
        int limit = Math.min(20, docs.size());
        for (int i = 0; i < limit; i++) {
            DocumentSnapshot doc = docs.get(i);
            View row = getLayoutInflater().inflate(R.layout.row_customer_history, binding.ticketListContainer, false);

            String category = safe(doc.getString("category"));
            String status = safe(doc.getString("status"));
            String subject = safe(doc.getString("subject"));
            String message = safe(doc.getString("message"));
            Timestamp created = doc.getTimestamp("createdAt");
            String date = created != null ? DateUtils.formatShortDate(created.toDate()) : "-";

            TextView title = row.findViewById(R.id.txtHistoryTitle);
            TextView subtitle = row.findViewById(R.id.txtHistorySubtitle);
            TextView value = row.findViewById(R.id.txtHistoryValue);

            title.setText((subject.isEmpty() ? "Support Ticket" : subject) + " • " + date);
            subtitle.setText((category.isEmpty() ? "General" : category) + " • " + (message.isEmpty() ? "No details" : message));
            value.setText(status.isEmpty() ? "OPEN" : status.toUpperCase(Locale.US));

            binding.ticketListContainer.addView(row);
        }
    }

    private void submitTicket() {
        if (providerId == null || providerId.trim().isEmpty()) {
            showToast("Connect to an active provider first");
            return;
        }

        String subject = safe(binding.etSubject.getText() != null ? binding.etSubject.getText().toString() : "");
        String message = safe(binding.etMessage.getText() != null ? binding.etMessage.getText().toString() : "");
        String category = spinnerCategory.getSelectedItem() != null
            ? spinnerCategory.getSelectedItem().toString() : "Other";

        if (subject.isEmpty()) {
            binding.etSubject.setError("Subject is required");
            binding.etSubject.requestFocus();
            return;
        }
        if (message.isEmpty()) {
            binding.etMessage.setError("Please describe the issue");
            binding.etMessage.requestFocus();
            return;
        }

        setSubmitBusy(true);

        Map<String, Object> ticket = new HashMap<>();
        ticket.put("customerId", customerId);
        ticket.put("providerId", providerId);
        ticket.put("customerName", safe(preferenceManager.getUserName()));
        ticket.put("customerEmail", customerEmail);
        ticket.put("providerName", providerName);
        ticket.put("providerEmail", providerEmail);
        ticket.put("category", category);
        ticket.put("subject", subject);
        ticket.put("message", message);
        ticket.put("status", "OPEN");
        ticket.put("createdAt", Timestamp.now());
        ticket.put("updatedAt", Timestamp.now());

        firestore.collection(Constants.COLLECTION_SUPPORT_TICKETS)
            .add(ticket)
            .addOnSuccessListener(ref -> {
                if (!isUiActive()) return;
                setSubmitBusy(false);
                showToast("Support ticket submitted");
                binding.etSubject.setText("");
                binding.etMessage.setText("");
                emailProvider(subject, category, message);
                loadTickets();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setSubmitBusy(false);
                showToast("Failed to submit ticket: " + e.getMessage());
            });
    }

    private void showEmptyState(boolean show, String message) {
        binding.txtNoTickets.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            binding.txtNoTickets.setText(message);
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void setSubmitBusy(boolean busy) {
        binding.btnSubmitTicket.setEnabled(!busy);
        binding.btnSubmitTicket.setText(busy ? "Submitting..." : "Submit Ticket");
        binding.btnEmailProvider.setEnabled(!busy && providerEmail != null && !providerEmail.isEmpty());
    }

    private void updateSupportAvailability(boolean canSubmit) {
        if (binding == null) return;
        binding.btnSubmitTicket.setEnabled(canSubmit);
        binding.spinnerCategory.setEnabled(canSubmit);
        binding.etSubject.setEnabled(canSubmit);
        binding.etMessage.setEnabled(canSubmit);
        updateProviderEmailButton();
    }

    private void updateProviderEmailButton() {
        if (binding == null) return;
        binding.btnEmailProvider.setEnabled(providerEmail != null && !providerEmail.isEmpty());
    }

    private void emailProvider(String subject, String category, String message) {
        if (providerEmail == null || providerEmail.isEmpty()) {
            showToast(getString(R.string.customer_support_provider_unavailable));
            return;
        }

        String safeSubject = subject == null || subject.trim().isEmpty() ? "Support Request" : subject.trim();
        String safeCategory = category == null || category.trim().isEmpty() ? "General" : category.trim();
        String safeMessage = message == null || message.trim().isEmpty() ? "Please review my issue." : message.trim();
        String customerName = safe(preferenceManager.getUserName());
        if (customerName.isEmpty()) {
            customerName = "Customer";
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + Uri.encode(providerEmail)));
        intent.putExtra(Intent.EXTRA_SUBJECT,
            getString(R.string.customer_support_ticket_mail_subject, safeSubject));
        intent.putExtra(Intent.EXTRA_TEXT,
            getString(
                R.string.customer_support_ticket_mail_body,
                providerName.isEmpty() ? "Service Provider" : providerName,
                safeCategory,
                safeSubject,
                safeMessage,
                customerName,
                customerEmail == null ? "" : customerEmail
            )
        );

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.customer_support_email_provider)));
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.customer_support_no_email_client));
        }
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
