package com.dailyserviceapp.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.ValidationUtils;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.dailyserviceapp.databinding.ActivityProfileBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;

    private TextInputEditText businessNameInput;
    private TextInputEditText ownerNameInput;
    private TextInputEditText phoneInput;
    private TextInputEditText emailInput;
    private TextInputEditText addressInput;
    private TextInputEditText areaInput;
    private TextInputEditText cityInput;
    private TextInputEditText gstInput;
    private TextInputEditText upiInput;
    private TextInputEditText notesInput;
    private TextInputEditText otherServiceInput;
    private TextInputLayout otherServiceLayout;

    private ChipGroup serviceChipGroup;
    private Chip chipMilk;
    private Chip chipNewspaper;
    private Chip chipWater;
    private Chip chipTiffin;
    private Chip chipLaundry;
    private Chip chipMaid;
    private Chip chipOther;

    private MaterialButton btnSaveProfile;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String providerId;
    private long createdAt = 0L;

    private boolean forceProfileSetup = false;
    private boolean hasExistingProfile = false;
    private boolean isEditMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }

        if (!isProvider()) {
            showToast("Profile is available for service providers only");
            finish();
            return;
        }

        providerId = getCurrentUserId();
        if (providerId == null || providerId.isEmpty()) {
            showToast("Session expired. Please login again.");
            navigateToLogin();
            return;
        }

        forceProfileSetup = getIntent().getBooleanExtra(Constants.EXTRA_FORCE_PROFILE_SETUP, false);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = binding.toolbar;
        setupToolbar(toolbar, forceProfileSetup ? "Setup Profile" : "Profile", true);

        initViews();
        setupListeners();
        setEditMode(true);
        loadProfile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (hasExistingProfile && !isEditMode) {
            getMenuInflater().inflate(R.menu.profile_menu, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit_profile) {
            setEditMode(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (forceProfileSetup && !hasExistingProfile) {
            showToast("Please complete profile setup first");
            return;
        }
        super.onBackPressed();
    }

    private void initViews() {
        businessNameInput = binding.businessNameInput;
        ownerNameInput = binding.ownerNameInput;
        phoneInput = binding.phoneInput;
        emailInput = binding.emailInput;
        addressInput = binding.addressInput;
        areaInput = binding.areaInput;
        cityInput = binding.cityInput;
        gstInput = binding.gstInput;
        upiInput = binding.upiInput;
        notesInput = binding.notesInput;
        otherServiceInput = binding.otherServiceInput;
        otherServiceLayout = binding.otherServiceLayout;

        serviceChipGroup = binding.serviceChipGroup;
        chipMilk = binding.chipMilk;
        chipNewspaper = binding.chipNewspaper;
        chipWater = binding.chipWater;
        chipTiffin = binding.chipTiffin;
        chipLaundry = binding.chipLaundry;
        chipMaid = binding.chipMaid;
        chipOther = binding.chipOther;

        btnSaveProfile = binding.btnSaveProfile;
        progressBar = binding.progressBar;

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            setTextIfPresent(emailInput, user.getEmail());
            if (ownerNameInput != null
                && (ownerNameInput.getText() == null || ownerNameInput.getText().toString().trim().isEmpty())) {
                setTextIfPresent(ownerNameInput, user.getDisplayName());
            }
        }
    }

    private void setupListeners() {
        if (chipOther != null) {
            chipOther.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (otherServiceLayout != null) {
                    otherServiceLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        showLoading(true);
        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(providerId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                showLoading(false);
                if (documentSnapshot == null || !documentSnapshot.exists()) {
                    hasExistingProfile = false;
                    setEditMode(true);
                    if (forceProfileSetup) {
                        showToast("Please setup your profile to continue");
                    }
                    return;
                }

                createdAt = documentSnapshot.getLong("createdAt") != null
                    ? documentSnapshot.getLong("createdAt") : 0L;

                setTextIfPresent(businessNameInput, documentSnapshot.getString("businessName"));
                setTextIfPresent(ownerNameInput, documentSnapshot.getString("name"));
                setTextIfPresent(phoneInput, documentSnapshot.getString("phone"));
                setTextIfPresent(emailInput, documentSnapshot.getString("email"));
                setTextIfPresent(addressInput, documentSnapshot.getString("address"));
                setTextIfPresent(areaInput, documentSnapshot.getString("area"));
                setTextIfPresent(cityInput, documentSnapshot.getString("city"));
                setTextIfPresent(gstInput, documentSnapshot.getString("gstNumber"));
                setTextIfPresent(upiInput, documentSnapshot.getString("upiId"));
                setTextIfPresent(notesInput, documentSnapshot.getString("notes"));

                @SuppressWarnings("unchecked")
                List<String> services = (List<String>) documentSnapshot.get("services");
                if (services == null || services.isEmpty()) {
                    String singleService = documentSnapshot.getString("serviceType");
                    if (singleService != null && !singleService.trim().isEmpty()) {
                        services = new ArrayList<>();
                        services.add(singleService);
                    }
                }

                applyServiceSelection(services, documentSnapshot.getString("otherService"));

                hasExistingProfile = isProfileComplete(documentSnapshot);
                if (hasExistingProfile) {
                    setEditMode(false);
                } else {
                    setEditMode(true);
                    if (forceProfileSetup) {
                        showToast("Please complete profile details");
                    }
                }
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                setEditMode(true);
                showToast("Failed to load profile: " + e.getMessage());
            });
    }

    private void saveProfile() {
        String businessName = getValue(businessNameInput);
        String ownerName = getValue(ownerNameInput);
        String phoneRaw = getValue(phoneInput);
        String email = getValue(emailInput);
        String address = getValue(addressInput);
        String area = getValue(areaInput);
        String city = getValue(cityInput);
        String gstNumber = getValue(gstInput);
        String upiId = getValue(upiInput);
        String notes = getValue(notesInput);
        String otherService = getValue(otherServiceInput);

        if (businessName.isEmpty()) {
            businessNameInput.setError("Business name is required");
            businessNameInput.requestFocus();
            return;
        }

        if (ownerName.isEmpty()) {
            ownerNameInput.setError("Owner name is required");
            ownerNameInput.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidPhone(phoneRaw)) {
            phoneInput.setError("Please enter a valid phone number");
            phoneInput.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            addressInput.setError("Address is required");
            addressInput.requestFocus();
            return;
        }

        List<String> services = getSelectedServices();
        if (services.isEmpty()) {
            showToast("Please select at least one service");
            return;
        }

        if (chipOther != null && chipOther.isChecked()) {
            if (otherService.isEmpty()) {
                otherServiceInput.setError("Please enter the other service");
                otherServiceInput.requestFocus();
                return;
            }
            services.add(otherService);
        }

        String phone = ValidationUtils.normalizePhoneNumber(phoneRaw);

        Map<String, Object> data = new HashMap<>();
        data.put("id", providerId);
        data.put("userId", providerId);
        data.put("name", ownerName);
        data.put("businessName", businessName);
        data.put("phone", phone);
        data.put("email", email);
        data.put("address", address);
        data.put("area", area);
        data.put("city", city);
        data.put("gstNumber", gstNumber);
        data.put("upiId", upiId);
        data.put("services", services);
        data.put("serviceType", services.get(0));
        data.put("otherService", otherService);
        data.put("notes", notes);
        data.put("providerCode", shortProviderCode(providerId));
        data.put("updatedAt", System.currentTimeMillis());
        data.put("createdAt", createdAt == 0L ? System.currentTimeMillis() : createdAt);

        showLoading(true);
        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(providerId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(unused -> {
                showLoading(false);
                hasExistingProfile = true;
                setEditMode(false);
                showToast("Profile saved successfully");
                if (forceProfileSetup) {
                    forceProfileSetup = false;
                    navigateToDashboard();
                }
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                showToast("Failed to save profile: " + e.getMessage());
            });
    }

    private List<String> getSelectedServices() {
        List<String> services = new ArrayList<>();
        if (chipMilk != null && chipMilk.isChecked()) services.add(chipMilk.getText().toString());
        if (chipNewspaper != null && chipNewspaper.isChecked()) services.add(chipNewspaper.getText().toString());
        if (chipWater != null && chipWater.isChecked()) services.add(chipWater.getText().toString());
        if (chipTiffin != null && chipTiffin.isChecked()) services.add(chipTiffin.getText().toString());
        if (chipLaundry != null && chipLaundry.isChecked()) services.add(chipLaundry.getText().toString());
        if (chipMaid != null && chipMaid.isChecked()) services.add(chipMaid.getText().toString());
        return services;
    }

    private void applyServiceSelection(List<String> services, String otherServiceValue) {
        if (services == null) services = new ArrayList<>();

        Set<String> known = new HashSet<>();
        known.add("Milk");
        known.add("Newspaper");
        known.add("Water");
        known.add("Tiffin");
        known.add("Laundry");
        known.add("Maid");

        for (String service : services) {
            if ("Milk".equalsIgnoreCase(service) && chipMilk != null) chipMilk.setChecked(true);
            if ("Newspaper".equalsIgnoreCase(service) && chipNewspaper != null) chipNewspaper.setChecked(true);
            if ("Water".equalsIgnoreCase(service) && chipWater != null) chipWater.setChecked(true);
            if ("Tiffin".equalsIgnoreCase(service) && chipTiffin != null) chipTiffin.setChecked(true);
            if ("Laundry".equalsIgnoreCase(service) && chipLaundry != null) chipLaundry.setChecked(true);
            if ("Maid".equalsIgnoreCase(service) && chipMaid != null) chipMaid.setChecked(true);
        }

        String otherValue = otherServiceValue;
        if ((otherValue == null || otherValue.trim().isEmpty())) {
            for (String service : services) {
                if (!known.contains(service)) {
                    otherValue = service;
                    break;
                }
            }
        }

        if (otherValue != null && !otherValue.trim().isEmpty() && chipOther != null) {
            chipOther.setChecked(true);
            if (otherServiceLayout != null) {
                otherServiceLayout.setVisibility(View.VISIBLE);
            }
            if (otherServiceInput != null) {
                otherServiceInput.setText(otherValue);
            }
        }
    }

    private void setEditMode(boolean editable) {
        isEditMode = editable;

        setInputEnabled(businessNameInput, editable);
        setInputEnabled(ownerNameInput, editable);
        setInputEnabled(phoneInput, editable);
        setInputEnabled(addressInput, editable);
        setInputEnabled(areaInput, editable);
        setInputEnabled(cityInput, editable);
        setInputEnabled(gstInput, editable);
        setInputEnabled(upiInput, editable);
        setInputEnabled(notesInput, editable);
        setInputEnabled(otherServiceInput, editable);
        setInputEnabled(emailInput, false);

        setChipEnabled(chipMilk, editable);
        setChipEnabled(chipNewspaper, editable);
        setChipEnabled(chipWater, editable);
        setChipEnabled(chipTiffin, editable);
        setChipEnabled(chipLaundry, editable);
        setChipEnabled(chipMaid, editable);
        setChipEnabled(chipOther, editable);
        if (serviceChipGroup != null) {
            serviceChipGroup.setEnabled(editable);
        }

        if (otherServiceLayout != null) {
            boolean shouldShowOther = chipOther != null && chipOther.isChecked();
            otherServiceLayout.setVisibility(shouldShowOther ? View.VISIBLE : View.GONE);
        }

        if (btnSaveProfile != null) {
            btnSaveProfile.setVisibility(editable ? View.VISIBLE : View.GONE);
        }

        updateToolbarTitle();
        invalidateOptionsMenu();
    }

    private void updateToolbarTitle() {
        if (getSupportActionBar() == null) return;

        if (!hasExistingProfile) {
            getSupportActionBar().setTitle("Setup Profile");
        } else if (isEditMode) {
            getSupportActionBar().setTitle("Edit Profile");
        } else {
            getSupportActionBar().setTitle("Profile");
        }
    }

    private void setInputEnabled(TextInputEditText input, boolean enabled) {
        if (input == null) return;
        input.setEnabled(enabled);
        input.setFocusable(enabled);
        input.setFocusableInTouchMode(enabled);
        input.setClickable(enabled);
        input.setLongClickable(enabled);
    }

    private void setChipEnabled(Chip chip, boolean enabled) {
        if (chip != null) {
            chip.setEnabled(enabled);
        }
    }

    private boolean isProfileComplete(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) return false;

        String businessName = safeTrim(documentSnapshot.getString("businessName"));
        String ownerName = safeTrim(documentSnapshot.getString("name"));
        String phone = safeTrim(documentSnapshot.getString("phone"));
        String address = safeTrim(documentSnapshot.getString("address"));

        @SuppressWarnings("unchecked")
        List<String> services = (List<String>) documentSnapshot.get("services");
        String serviceType = safeTrim(documentSnapshot.getString("serviceType"));
        boolean hasService = (services != null && !services.isEmpty()) || !serviceType.isEmpty();

        return !businessName.isEmpty()
            && !ownerName.isEmpty()
            && !phone.isEmpty()
            && !address.isEmpty()
            && hasService;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void setTextIfPresent(TextInputEditText editText, String value) {
        if (editText != null && value != null) {
            editText.setText(value);
        }
    }

    private String getValue(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    private String shortProviderCode(String id) {
        if (id == null || id.trim().isEmpty()) return "";
        String trimmed = id.trim();
        if (trimmed.length() <= 8) return trimmed.toUpperCase(Locale.US);
        return trimmed.substring(0, 8).toUpperCase(Locale.US);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnSaveProfile != null) {
            btnSaveProfile.setEnabled(!show && isEditMode);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
