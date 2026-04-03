package com.dailyserviceapp.provider;

import dagger.hilt.android.AndroidEntryPoint;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.databinding.ActivityJoinRequestsBinding;
import com.dailyserviceapp.notifications.NotificationHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provider-facing inbox for customer join requests.
 */
@AndroidEntryPoint
public class JoinRequestsActivity extends BaseActivity {

    private ActivityJoinRequestsBinding binding;
    private FirebaseFirestore firestore;
    private String providerId;
    private JoinRequestAdapter adapter;
    private ListenerRegistration requestsListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJoinRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }

        if (!isProvider()) {
            showToast("Join requests are for service providers only");
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

        setupToolbar();
        setupRecycler();
        listenForRequests();
    }

    private void setupToolbar() {
        setupToolbar(binding.toolbar, "Join Requests", true);
    }

    private void setupRecycler() {
        adapter = new JoinRequestAdapter(new JoinRequestAdapter.OnActionListener() {
            @Override
            public void onApprove(JoinRequestItem item) {
                approveRequest(item);
            }

            @Override
            public void onReject(JoinRequestItem item) {
                confirmReject(item);
            }
        });

        binding.recyclerJoinRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerJoinRequests.setAdapter(adapter);
    }

    private void listenForRequests() {
        showLoading(true);

        if (requestsListener != null) {
            requestsListener.remove();
        }

        requestsListener = firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .whereEqualTo("providerId", providerId)
            .addSnapshotListener((snapshot, error) -> {
                if (!isUiActive()) return;
                showLoading(false);

                if (error != null) {
                    showToast("Failed to load join requests: " + error.getMessage());
                    renderRequests(new ArrayList<>());
                    return;
                }

                List<JoinRequestItem> pending = new ArrayList<>();
                if (snapshot != null) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = safeTrim(doc.getString("status"));
                        if (!"PENDING".equalsIgnoreCase(status)) {
                            continue;
                        }

                        String customerId = safeTrim(doc.getString("customerId"));
                        if (customerId.isEmpty()) {
                            customerId = doc.getId();
                        }

                        pending.add(new JoinRequestItem(
                            doc.getId(),
                            customerId,
                            doc.getString("customerName"),
                            doc.getString("customerEmail"),
                            doc.getString("customerPhone"),
                            doc.getTimestamp("requestedAt")
                        ));
                    }
                }

                pending.sort((a, b) -> {
                    long aTime = a.getRequestedAt() == null ? 0L : a.getRequestedAt().toDate().getTime();
                    long bTime = b.getRequestedAt() == null ? 0L : b.getRequestedAt().toDate().getTime();
                    return Long.compare(bTime, aTime);
                });

                renderRequests(pending);
            });
    }

    private void renderRequests(List<JoinRequestItem> requests) {
        adapter.submit(requests);

        boolean empty = requests == null || requests.isEmpty();
        binding.recyclerJoinRequests.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.emptyStateLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void approveRequest(JoinRequestItem item) {
        if (item == null) return;

        showLoading(true);
        fetchDefaultServiceAndApprove(item);
    }

    private void fetchDefaultServiceAndApprove(JoinRequestItem item) {
        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(providerId)
            .get()
            .addOnSuccessListener(providerDoc -> {
                if (!isUiActive()) return;
                String defaultService = resolveDefaultService(providerDoc);
                createOrUpdateCustomerFromJoinRequest(item, defaultService);
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                createOrUpdateCustomerFromJoinRequest(item, "Service");
            });
    }

    private String resolveDefaultService(DocumentSnapshot providerDoc) {
        if (providerDoc == null || !providerDoc.exists()) {
            return "Service";
        }

        String serviceType = safeTrim(providerDoc.getString("serviceType"));
        if (!serviceType.isEmpty()) {
            return serviceType;
        }

        Object servicesObj = providerDoc.get("services");
        if (servicesObj instanceof List) {
            List<?> services = (List<?>) servicesObj;
            if (!services.isEmpty() && services.get(0) instanceof String) {
                String first = ((String) services.get(0)).trim();
                if (!first.isEmpty()) {
                    return first;
                }
            }
        }

        return "Service";
    }

    private void createOrUpdateCustomerFromJoinRequest(JoinRequestItem item, String defaultService) {
        if (!isUiActive()) return;
        String customerDocId = safeTrim(item.getCustomerId());
        if (customerDocId.isEmpty()) {
            if (isUiActive()) {
                showLoading(false);
                showToast("Invalid request data: missing customer id");
            }
            return;
        }
        String linkId = safeTrim(item.getLinkId());
        if (linkId.isEmpty()) {
            if (isUiActive()) {
                showLoading(false);
                showToast("Invalid request data: missing link id");
            }
            return;
        }

        DocumentReference customerRef = firestore.collection(Constants.COLLECTION_CUSTOMERS)
            .document(customerDocId);
        DocumentReference linkRef = firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .document(linkId);

        customerRef.get()
            .addOnSuccessListener(existingCustomerDoc -> {
                if (!isUiActive()) return;
                Map<String, Object> customerData = buildCustomerData(
                    item,
                    defaultService,
                    existingCustomerDoc == null || !existingCustomerDoc.exists()
                );

                commitApproval(customerRef, linkRef, customerData, item.getCustomerId());
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;

                // If customer doc does not exist yet, rules can reject get(). Proceed as a new customer.
                if (isPermissionDenied(e)) {
                    Map<String, Object> customerData = buildCustomerData(item, defaultService, true);
                    commitApproval(customerRef, linkRef, customerData, item.getCustomerId());
                    return;
                }

                showLoading(false);
                showToast("Failed to read customer record: " + errorMessage(e));
            });
    }

    private void commitApproval(DocumentReference customerRef,
                                DocumentReference linkRef,
                                Map<String, Object> customerData,
                                String customerId) {
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("customerId", safeTrim(customerRef.getId()));
        linkData.put("providerId", providerId);
        linkData.put("status", "ACTIVE");
        linkData.put("updatedAt", FieldValue.serverTimestamp());
        linkData.put("respondedAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(customerRef, customerData, SetOptions.merge());
        batch.set(linkRef, linkData, SetOptions.merge());

        batch.commit()
            .addOnSuccessListener(unused -> {
                if (!isUiActive()) return;
                NotificationHelper.saveNotification(
                    customerId,
                    "Join request approved",
                    "Your request was approved. You can now open your service dashboard.",
                    Constants.NOTIF_JOIN_REQUEST_STATUS,
                    providerId
                );
                showLoading(false);
                showToast("Request approved. Customer added to your list.");
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                showLoading(false);

                if (isPermissionDenied(e)) {
                    showToast("Approval failed: customer is linked to another provider.");
                } else if (isUnavailable(e)) {
                    showToast("Approval failed: network unavailable. Please check internet and try again.");
                } else {
                    showToast("Failed to approve request: " + errorMessage(e));
                }
            });
    }

    private boolean isPermissionDenied(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
            return firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
        }
        return false;
    }

    private boolean isUnavailable(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
            return firestoreException.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE;
        }
        return false;
    }

    private String errorMessage(Exception e) {
        return e != null && e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    private Map<String, Object> buildCustomerData(JoinRequestItem item, String defaultService, boolean isNewCustomer) {
        Map<String, Object> data = new HashMap<>();

        String name = safeTrim(item.getCustomerName());
        if (name.isEmpty()) {
            name = "Customer " + shortId(item.getCustomerId());
        }

        data.put("name", name);
        data.put("providerId", providerId);
        data.put("status", "ACTIVE");
        data.put("updatedAt", FieldValue.serverTimestamp());

        String phone = safeTrim(item.getCustomerPhone());
        if (!phone.isEmpty()) {
            data.put("phone", phone);
        }

        String email = safeTrim(item.getCustomerEmail());
        if (!email.isEmpty()) {
            data.put("email", email);
        }

        data.put("serviceType", safeTrim(defaultService).isEmpty() ? "Service" : defaultService);
        if (isNewCustomer) {
            data.put("address", "");
            data.put("ratePerUnit", 0.0);
            data.put("defaultQuantity", 1.0);
            data.put("lentAmount", 0.0);
            data.put("notes", "Joined via QR request");
            data.put("onVacation", false);
            data.put("startDate", FieldValue.serverTimestamp());
            data.put("createdAt", FieldValue.serverTimestamp());
        }
        return data;
    }

    private void confirmReject(JoinRequestItem item) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Reject join request?")
            .setMessage("The customer can send request again later.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reject", (dialog, which) -> rejectRequest(item))
            .show();
    }

    private void rejectRequest(JoinRequestItem item) {
        if (item == null) return;
        String linkId = safeTrim(item.getLinkId());
        if (linkId.isEmpty()) {
            showToast("Invalid request data: missing link id");
            return;
        }

        showLoading(true);
        firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .document(linkId)
            .set(buildRejectUpdate(), SetOptions.merge())
            .addOnSuccessListener(unused -> {
                if (!isUiActive()) return;
                NotificationHelper.saveNotification(
                    item.getCustomerId(),
                    "Join request rejected",
                    "Your request was rejected. You can try again later.",
                    Constants.NOTIF_JOIN_REQUEST_STATUS,
                    providerId
                );
                showLoading(false);
                showToast("Request rejected");
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                showLoading(false);
                showToast("Failed to reject request: " + e.getMessage());
            });
    }

    private Map<String, Object> buildRejectUpdate() {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "REJECTED");
        update.put("updatedAt", FieldValue.serverTimestamp());
        update.put("respondedAt", FieldValue.serverTimestamp());
        return update;
    }

    private String shortId(String id) {
        if (id == null || id.trim().isEmpty()) return "-";
        String trimmed = id.trim();
        if (trimmed.length() <= 8) return trimmed.toUpperCase(Locale.US);
        return trimmed.substring(0, 8).toUpperCase(Locale.US);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        super.onDestroy();
        binding = null;
    }
}
