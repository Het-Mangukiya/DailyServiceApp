package com.dailyserviceapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.dailyserviceapp.R;
import com.dailyserviceapp.customer.CustomerHomeActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.ValidationUtils;
import com.dailyserviceapp.data.models.User;
import com.dailyserviceapp.databinding.ActivitySignupBinding;
import com.dailyserviceapp.profile.ProfileActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Signup Activity for new user registration in DailyDrop.
 * Provides email/password registration and Google Sign-In options.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Name, email, phone, password input with validation</li>
 *   <li>Role selection (Provider or Customer)</li>
 *   <li>Email/password signup with Firebase Authentication</li>
 *   <li>Google Sign-In integration</li>
 *   <li>Automatic Firestore user record creation</li>
 *   <li>Link to login page for existing users</li>
 * </ul>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class SignupActivity extends BaseActivity {

    private ActivitySignupBinding binding;
    
    /** Input fields for user registration */
    private EditText nameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    
    /** Role selection spinner */
    private Spinner roleSpinner;
    
    /** Signup and Google Sign-In buttons */
    private Button signupButton, googleSignInButton;
    
    /** Login link for existing users */
    private TextView loginLink;
    
    /** Progress indicator */
    private ProgressBar progressBar;
    
    /** Firebase Authentication instance */
    private FirebaseAuth firebaseAuth;
    
    /** Firestore database instance */
    private FirebaseFirestore firestore;
    
    /** Google Sign-In client */
    private GoogleSignInClient googleSignInClient;
    
    /** Activity result launcher for Google Sign-In */
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        initializeFirebase();
        initializeViews();
        initializeGoogleSignIn();
        setupRoleSpinner();
        setupClickListeners();
    }
    
    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }
    
    private void initializeGoogleSignIn() {
        if (!isGoogleSignInConfigured()) {
            if (googleSignInButton != null) {
                googleSignInButton.setEnabled(false);
                googleSignInButton.setAlpha(0.5f);
            }
            android.util.Log.e("SignupActivity", "Google Sign-In is not configured: invalid default_web_client_id");
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) {
                    hideLoading();
                    showToast("Google Sign-In canceled (no data)");
                    android.util.Log.w("SignupActivity", "Google Sign-In result data is null. resultCode=" + result.getResultCode());
                    return;
                }

                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleGoogleSignInResult(task);
            }
        );
    }
    
    private void initializeViews() {
        nameInput = binding.nameInput;
        emailInput = binding.emailInput;
        phoneInput = binding.phoneInput;
        passwordInput = binding.passwordInput;
        confirmPasswordInput = binding.confirmPasswordInput;
        roleSpinner = binding.roleSpinner;
        signupButton = binding.signupButton;
        googleSignInButton = binding.googleSignInButton;
        loginLink = binding.loginLink;
        progressBar = binding.progressBar;
    }
    
    private void setupRoleSpinner() {
        String[] roles = getResources().getStringArray(R.array.role_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            roles
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(ContextCompat.getColor(SignupActivity.this, R.color.text_primary));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(ContextCompat.getColor(SignupActivity.this, R.color.text_primary));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
        roleSpinner.setSelection(0, false);
    }
    
    private void setupClickListeners() {
        signupButton.setOnClickListener(v -> performSignup());
        
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        
        loginLink.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void performSignup() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phoneRaw = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String selectedRole = roleSpinner.getSelectedItemPosition() == 0 ? 
            Constants.ROLE_PROVIDER : Constants.ROLE_CUSTOMER;
        
        // Validation
        if (!ValidationUtils.isValidName(name)) {
            nameInput.setError("Please enter a valid name");
            nameInput.requestFocus();
            return;
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return;
        }
        
        if (!ValidationUtils.isValidPhone(phoneRaw)) {
            phoneInput.setError("Please enter a valid phone number");
            phoneInput.requestFocus();
            return;
        }
        
        String phone = ValidationUtils.normalizePhoneNumber(phoneRaw);
        
        if (!ValidationUtils.isValidPassword(password)) {
            passwordInput.setError("Password must be at least 8 characters with letters and numbers");
            passwordInput.requestFocus();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }
        
        if (!isNetworkAvailable()) {
            showNetworkError();
            return;
        }
        
        showLoading();
        
        // Create Firebase Auth user
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // Save session immediately so login can proceed even if Firestore fails
                        preferenceManager.saveUserData(firebaseUser.getUid(), email, name, selectedRole);
                        createUserDocument(firebaseUser.getUid(), name, email, phone, selectedRole);
                    } else {
                        hideLoading();
                        showToast("Signup failed: user not available. Please try again.");
                    }
                } else {
                    hideLoading();
                    String error = getAuthErrorMessage(task.getException());
                    if (task.getException() != null) {
                        // Log the full error for debugging
                        android.util.Log.e("SignupActivity", "Signup error: " + task.getException().getMessage(), task.getException());
                    }
                    showToast(error);
                }
            })
            .addOnFailureListener(e -> {
                hideLoading();
                android.util.Log.e("SignupActivity", "Signup failure: " + e.getMessage(), e);
                showToast("Error: " + e.getMessage());
            });
    }
    
    private void createUserDocument(String userId, String name, String email, String phone, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .set(userData)
            .addOnCompleteListener(task -> {
                hideLoading();
                
                if (task.isSuccessful()) {
                    showToast("Account created successfully!");
                    navigateAfterSignup(userId, role, true);
                } else {
                    Exception exception = task.getException();
                    android.util.Log.e("SignupActivity", "Firestore user creation failed", exception);
                    showToast("Account created, but profile sync failed. You can continue.");
                    navigateAfterSignup(userId, role, true);
                }
            })
            .addOnFailureListener(e -> {
                hideLoading();
                android.util.Log.e("SignupActivity", "Failed to create Firestore document", e);
                showToast("Account created, but profile sync failed. You can continue.");
                navigateAfterSignup(userId, role, true);
            });
    }
    
    private void signInWithGoogle() {
        if (!isGoogleSignInConfigured() || googleSignInClient == null) {
            showToast("Google Sign-In is not configured yet. Please update Firebase config.");
            return;
        }

        if (!isNetworkAvailable()) {
            showNetworkError();
            return;
        }
        
        showLoading();
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }
    
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            hideLoading();
            int code = e.getStatusCode();
            android.util.Log.e("SignupActivity", "Google sign in failed. code=" + code, e);
            if (code == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                showToast("Google Sign-In canceled");
            } else if (code == GoogleSignInStatusCodes.SIGN_IN_FAILED) {
                showToast("Google Sign-In failed. Check internet and try again.");
            } else if (code == 10) {
                showToast("Google config mismatch (SHA/package).");
            } else {
                showToast("Google Sign-In error code: " + code);
            }
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // Check if user already exists in Firestore
                        checkAndCreateGoogleUserProfile(firebaseUser);
                    } else {
                        hideLoading();
                        showToast("Authentication failed");
                    }
                } else {
                    hideLoading();
                    showToast("Authentication failed");
                }
            });
    }
    
    /**
     * Check if Google user exists in Firestore, create if not
     */
    private void checkAndCreateGoogleUserProfile(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    if (task.getResult().exists()) {
                        // User already exists, just save session
                        String existingEmail = task.getResult().getString("email");
                        String existingName = task.getResult().getString("name");
                        String existingRole = task.getResult().getString("role");
                        
                        preferenceManager.saveUserData(userId, existingEmail, existingName, existingRole);
                        navigateAfterSignup(userId, existingRole, false);
                    } else {
                        // New Google user, create profile with selected role
                        String selectedRole = roleSpinner.getSelectedItemPosition() == 0 ? 
                            Constants.ROLE_PROVIDER : Constants.ROLE_CUSTOMER;
                        createGoogleUserDocument(firebaseUser, selectedRole);
                    }
                } else {
                    hideLoading();
                    // Continue with local session if Firestore is unavailable
                    preferenceManager.saveUserData(
                        userId,
                        firebaseUser.getEmail(),
                        firebaseUser.getDisplayName(),
                        Constants.ROLE_CUSTOMER
                    );
                    showToast("Signed in, but profile sync failed. You can continue.");
                    navigateToCustomerHome();
                }
            });
    }
    
    /**
     * Create Firestore document for new Google user
     */
    private void createGoogleUserDocument(FirebaseUser firebaseUser, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", firebaseUser.getUid());
        userData.put("name", firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User");
        userData.put("email", firebaseUser.getEmail());
        userData.put("phone", firebaseUser.getPhoneNumber() != null ? firebaseUser.getPhoneNumber() : "");
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(firebaseUser.getUid())
            .set(userData)
            .addOnCompleteListener(task -> {
                hideLoading();
                if (task.isSuccessful()) {
                    // Save session
                    preferenceManager.saveUserData(
                        firebaseUser.getUid(),
                        firebaseUser.getEmail(),
                        firebaseUser.getDisplayName(),
                        role
                    );
                    
                    showToast("Account created successfully!");
                    navigateAfterSignup(firebaseUser.getUid(), role, true);
                } else {
                    preferenceManager.saveUserData(
                        firebaseUser.getUid(),
                        firebaseUser.getEmail(),
                        firebaseUser.getDisplayName(),
                        role
                    );
                    showToast("Account created, but profile sync failed. You can continue.");
                    navigateAfterSignup(firebaseUser.getUid(), role, true);
                }
            });
    }

    private void navigateAfterSignup(String userId, String role, boolean isNewAccount) {
        if (!Constants.ROLE_PROVIDER.equals(role)) {
            navigateToCustomerHome();
            return;
        }

        if (isNewAccount) {
            navigateToProfileSetup();
            return;
        }

        checkProviderProfileAndRoute(userId);
    }

    private void checkProviderProfileAndRoute(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            navigateToProviderHome();
            return;
        }

        showLoading();
        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                hideLoading();
                if (isProviderProfileComplete(documentSnapshot)) {
                    navigateToProviderHome();
                } else {
                    navigateToProfileSetup();
                }
            })
            .addOnFailureListener(e -> {
                hideLoading();
                navigateToProfileSetup();
            });
    }

    private boolean isProviderProfileComplete(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) return false;

        String businessName = safeTrim(documentSnapshot.getString("businessName"));
        String ownerName = safeTrim(documentSnapshot.getString("name"));
        String phone = safeTrim(documentSnapshot.getString("phone"));
        String address = safeTrim(documentSnapshot.getString("address"));

        @SuppressWarnings("unchecked")
        java.util.List<String> services = (java.util.List<String>) documentSnapshot.get("services");
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

    private void navigateToProfileSetup() {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(Constants.EXTRA_FORCE_PROFILE_SETUP, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Navigate to Dashboard with proper flags
     */
    private void navigateToProviderHome() {
        Intent intent = new Intent(this, com.dailyserviceapp.dashboard.DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToCustomerHome() {
        Intent intent = new Intent(this, CustomerHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);
        googleSignInButton.setEnabled(false);
    }
    
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        signupButton.setEnabled(true);
        googleSignInButton.setEnabled(isGoogleSignInConfigured());
    }

    private boolean isGoogleSignInConfigured() {
        String webClientId = getString(R.string.default_web_client_id);
        return webClientId != null
            && webClientId.endsWith(".apps.googleusercontent.com")
            && !webClientId.contains("PLACEHOLDER");
    }

    private String getAuthErrorMessage(Exception exception) {
        if (exception == null) return "Signup failed. Please try again.";
        String message = exception.getMessage() != null ? exception.getMessage() : "Signup failed.";
        if (exception instanceof com.google.firebase.auth.FirebaseAuthException) {
            String code = ((com.google.firebase.auth.FirebaseAuthException) exception).getErrorCode();
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) {
                return "Email already registered. Please login.";
            }
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "Invalid email address.";
            }
            if ("ERROR_WEAK_PASSWORD".equals(code)) {
                return "Password is too weak.";
            }
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(code)) {
                return "Email/password sign-up is disabled in Firebase.";
            }
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
                return "Network error. Please check your connection.";
            }
        }
        return message;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
