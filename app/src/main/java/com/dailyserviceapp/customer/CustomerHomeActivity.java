package com.dailyserviceapp.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.PopupMenu;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.dailyserviceapp.databinding.ActivityCustomerHomeBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Customer home screen.
 *
 * Supports:
 * - Joining a provider via QR/manual provider code
 * - Viewing currently linked provider
 * - Unlinking provider
 */
public class CustomerHomeActivity extends BaseActivity {

    private ActivityCustomerHomeBinding binding;

    private FirebaseFirestore firestore;
    private String customerId;
    private GoogleSignInClient googleSignInClient;
    private ListenerRegistration linkListener;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }

        if (!isCustomer()) {
            // Defensive routing: if a provider somehow lands here, send them back.
            startActivity(new Intent(this, DashboardActivity.class));
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
        setupGoogleSignInClient();

        setupToolbar();
        setupContent();
        loadLinkedProvider();
    }

    private void setupGoogleSignInClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.customer_home_title);
        }
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

    private boolean handleToolbarMenuClick(MenuItem item) {
        if (item.getItemId() == R.id.action_support) {
            startActivity(new Intent(this, ComplaintSupportActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return false;
    }

    private void setupContent() {
        String name = preferenceManager.getUserName();
        if (name == null || name.trim().isEmpty()) {
            name = getString(R.string.default_user_name);
        }
        binding.txtWelcome.setText(getString(R.string.customer_home_welcome, name));

        binding.providerCodeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.providerCodeLayout.setError(null);
                updateJoinActionEnabled();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        binding.providerCodeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                joinProviderFromInput();
                return true;
            }
            return false;
        });

        binding.btnScanProviderQr.setOnClickListener(v -> startQrScan());
        binding.btnJoinProvider.setOnClickListener(v -> joinProviderFromInput());
        binding.btnUnlinkProvider.setOnClickListener(v -> confirmUnlinkProvider());
        binding.btnOpenDashboard.setOnClickListener(v ->
            startActivity(new Intent(this, CustomerServiceDashboardActivity.class))
        );
        updateJoinActionEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (linkListener == null) {
            loadLinkedProvider();
        }
    }

    private void startQrScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan service provider QR code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String contents = result.getContents();
            if (contents == null || contents.trim().isEmpty()) {
                showToast("Scan cancelled");
            } else {
                binding.providerCodeInput.setText(contents.trim());
                resolveAndLinkProvider(contents);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void joinProviderFromInput() {
        String rawCode = binding.providerCodeInput.getText() != null
            ? binding.providerCodeInput.getText().toString().trim()
            : "";

        if (TextUtils.isEmpty(rawCode)) {
            binding.providerCodeLayout.setError("Enter provider code");
            return;
        }

        binding.providerCodeLayout.setError(null);
        resolveAndLinkProvider(rawCode);
    }

    private void resolveAndLinkProvider(String rawCode) {
        String providerCode = extractProviderId(rawCode);
        if (providerCode.isEmpty()) {
            binding.providerCodeLayout.setError("Invalid provider code");
            return;
        }

        if (providerCode.equals(customerId)) {
            binding.providerCodeLayout.setError("You cannot link to your own account");
            return;
        }

        setLoading(true);
        if (isLikelyFullProviderId(providerCode)) {
            fetchProviderAndLinkById(providerCode);
            return;
        }

        resolveProviderFromShortCode(providerCode.toUpperCase(Locale.US));
    }

    private boolean isLikelyFullProviderId(String value) {
        return value != null && value.trim().length() >= 20;
    }

    private void fetchProviderAndLinkById(String providerId) {
        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(providerId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isUiActive()) return;
                handleResolvedProviderDocument(documentSnapshot);
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                Log.e("CustomerHomeActivity", "Failed to verify provider by id", e);
                showToast("Failed to verify provider");
            });
    }

    private void resolveProviderFromShortCode(String shortCode) {
        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .whereEqualTo("providerCode", shortCode)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!isUiActive()) return;
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    handleResolvedProviderDocument(querySnapshot.getDocuments().get(0));
                    return;
                }
                resolveProviderFromShortCodeFallback();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                Log.e("CustomerHomeActivity", "Failed to verify provider code", e);
                showToast("Failed to verify provider code");
            });
    }

    private void resolveProviderFromShortCodeFallback() {
        setLoading(false);
        binding.providerCodeLayout.setError("Provider not found. Ask provider to share QR again.");
    }

    private void handleResolvedProviderDocument(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) {
            setLoading(false);
            binding.providerCodeLayout.setError("Provider not found. Ask provider to share QR again.");
            return;
        }

        String providerId = documentSnapshot.getId();
        if (providerId.equals(customerId)) {
            setLoading(false);
            binding.providerCodeLayout.setError("You cannot link to your own account");
            return;
        }

        String providerName = resolveProviderName(documentSnapshot, providerId);
        saveCustomerProviderLink(providerId, providerName);
    }

    private String extractProviderId(String rawCode) {
        if (rawCode == null) return "";

        String value = rawCode.trim();
        if (value.isEmpty()) return "";

        String marker = "provider id:";
        String lower = value.toLowerCase(Locale.US);
        int markerPos = lower.indexOf(marker);
        if (markerPos >= 0) {
            value = value.substring(markerPos + marker.length()).trim();
        }

        String uriPrefix = "dd://provider/";
        if (value.startsWith(uriPrefix)) {
            value = value.substring(uriPrefix.length()).trim();
        }

        int newLine = value.indexOf('\n');
        if (newLine >= 0) {
            value = value.substring(0, newLine).trim();
        }

        // Firebase UIDs are URL-safe alpha-numeric strings. Keep only expected chars.
        value = value.replaceAll("[^A-Za-z0-9_-]", "");

        return value;
    }

    private String resolveProviderName(DocumentSnapshot providerDoc, String providerId) {
        String businessName = providerDoc.getString("businessName");
        String ownerName = providerDoc.getString("name");

        if (businessName != null && !businessName.trim().isEmpty()) {
            return businessName.trim();
        }
        if (ownerName != null && !ownerName.trim().isEmpty()) {
            return ownerName.trim();
        }

        return "Provider " + shortProviderId(providerId);
    }

    private void saveCustomerProviderLink(String providerId, String providerName) {
        firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .document(customerId)
            .get()
            .addOnSuccessListener(existingLink -> {
                if (!isUiActive()) return;

                if (existingLink != null && existingLink.exists()) {
                    String existingProviderId = safeTrim(existingLink.getString("providerId"));
                    String existingProviderName = safeTrim(existingLink.getString("providerName"));
                    String existingStatus = safeTrim(existingLink.getString("status")).toUpperCase(Locale.US);

                    boolean hasDifferentActiveLink = "ACTIVE".equals(existingStatus)
                        && !existingProviderId.isEmpty()
                        && !existingProviderId.equals(providerId);
                    if (hasDifferentActiveLink) {
                        String displayExisting = existingProviderName.isEmpty()
                            ? shortProviderId(existingProviderId)
                            : existingProviderName;
                        new MaterialAlertDialogBuilder(this)
                            .setTitle("Replace linked provider?")
                            .setMessage("You are currently linked with " + displayExisting
                                + ". Do you want to request connection with " + providerName + "?")
                            .setNegativeButton("Cancel", (dialog, which) -> setLoading(false))
                            .setPositiveButton("Replace", (dialog, which) ->
                                performSaveCustomerProviderLink(providerId, providerName))
                            .show();
                        return;
                    }
                }

                performSaveCustomerProviderLink(providerId, providerName);
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                Log.e("CustomerHomeActivity", "Failed to check existing provider link", e);
                showToast("Failed to check existing link");
            });
    }

    private void performSaveCustomerProviderLink(String providerId, String providerName) {
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("customerId", customerId);
        linkData.put("providerId", providerId);
        linkData.put("providerName", providerName);
        linkData.put("customerName", safeTrim(preferenceManager.getUserName()));
        linkData.put("status", "PENDING");
        linkData.put("requestedAt", FieldValue.serverTimestamp());
        linkData.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .document(customerId)
            .set(linkData)
            .addOnSuccessListener(unused -> {
                if (!isUiActive()) return;
                setLoading(false);
                binding.providerCodeLayout.setError(null);
                renderLinkedProvider(providerName, providerId, "PENDING");
                showToast("Join request sent to " + providerName);
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                Log.e("CustomerHomeActivity", "Failed to save provider link", e);
                showToast("Failed to save provider link");
            });
    }

    private void loadLinkedProvider() {
        setLoading(true);
        if (linkListener != null) {
            linkListener.remove();
        }
        linkListener = firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .document(customerId)
            .addSnapshotListener((documentSnapshot, error) -> {
                if (!isUiActive()) return;
                setLoading(false);
                if (error != null) {
                    Log.e("CustomerHomeActivity", "Failed to watch provider link", error);
                    showUnlinkedState();
                    showToast("Could not load provider link");
                    return;
                }
                if (documentSnapshot == null || !documentSnapshot.exists()) {
                    showUnlinkedState();
                    return;
                }

                String providerId = safeTrim(documentSnapshot.getString("providerId"));
                String providerName = safeTrim(documentSnapshot.getString("providerName"));
                String status = safeTrim(documentSnapshot.getString("status"));

                if (providerId.isEmpty()) {
                    showUnlinkedState();
                    return;
                }

                if (providerName.isEmpty()) {
                    providerName = "Provider " + shortProviderId(providerId);
                }
                if (status.isEmpty()) {
                    status = "PENDING";
                }
                renderLinkedProvider(providerName, providerId, status);
            });
    }

    private void confirmUnlinkProvider() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Remove linked provider?")
            .setMessage("You can link again anytime using QR or provider code.")
            .setPositiveButton("Remove", (dialog, which) -> unlinkProvider())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void unlinkProvider() {
        setLoading(true);
        firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .document(customerId)
            .delete()
            .addOnSuccessListener(unused -> {
                if (!isUiActive()) return;
                setLoading(false);
                showUnlinkedState();
                showToast("Provider link removed");
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                Log.e("CustomerHomeActivity", "Failed to remove provider link", e);
                showToast("Failed to remove provider link");
            });
    }

    private void logout() {
        preferenceManager.clearAllData();
        FirebaseAuth.getInstance().signOut();

        if (googleSignInClient != null) {
            googleSignInClient.signOut().addOnCompleteListener(this, task -> navigateToLogin());
        } else {
            navigateToLogin();
        }
    }

    private void renderLinkedProvider(String providerName, String providerId, String status) {
        binding.linkedProviderCard.setVisibility(View.VISIBLE);
        binding.txtLinkedProviderName.setText(providerName);
        binding.txtLinkedProviderId.setText(getString(R.string.customer_home_provider_id, shortProviderId(providerId)));
        String normalizedStatus = safeTrim(status).toUpperCase(Locale.US);
        if (normalizedStatus.isEmpty()) {
            normalizedStatus = "PENDING";
        }
        binding.txtLinkStatus.setText("Status: " + normalizedStatus);
        binding.btnUnlinkProvider.setText("PENDING".equals(normalizedStatus) ? "Cancel Request" : "Remove Link");
        binding.btnOpenDashboard.setVisibility("ACTIVE".equals(normalizedStatus) ? View.VISIBLE : View.GONE);
    }

    private void showUnlinkedState() {
        binding.linkedProviderCard.setVisibility(View.GONE);
        binding.btnOpenDashboard.setVisibility(View.GONE);
        updateJoinActionEnabled();
    }

    private String shortProviderId(String providerId) {
        if (providerId == null) return "-";
        String trimmed = providerId.trim();
        if (trimmed.length() <= 8) return trimmed.toUpperCase(Locale.US);
        return trimmed.substring(0, 8).toUpperCase(Locale.US);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void setLoading(boolean loading) {
        if (!isUiActive()) return;
        this.loading = loading;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnScanProviderQr.setEnabled(!loading);
        binding.btnUnlinkProvider.setEnabled(!loading);
        updateJoinActionEnabled();
    }

    private void updateJoinActionEnabled() {
        if (binding == null) return;
        String code = binding.providerCodeInput.getText() != null
            ? binding.providerCodeInput.getText().toString().trim()
            : "";
        binding.btnJoinProvider.setEnabled(!loading && !code.isEmpty());
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        if (linkListener != null) {
            linkListener.remove();
            linkListener = null;
        }
        super.onDestroy();
        binding = null;
    }
}
