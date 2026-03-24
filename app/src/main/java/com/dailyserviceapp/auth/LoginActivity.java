package com.dailyserviceapp.auth;

import dagger.hilt.android.AndroidEntryPoint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.dailyserviceapp.R;
import com.dailyserviceapp.SplashActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.ValidationUtils;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.dailyserviceapp.databinding.ActivityLoginBinding;
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
import java.util.Locale;
import java.util.Map;

/**
 * Login Activity for DailyDrop application.
 * Provides email/password authentication and Google Sign-In functionality.
 * Integrates with Firebase Authentication and Firestore for user management.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Email/password login with validation</li>
 *   <li>Google Sign-In integration</li>
 *   <li>Password recovery link</li>
 *   <li>Registration link to SignupActivity</li>
 *   <li>Automatic navigation to dashboard after successful login</li>
 * </ul>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
@AndroidEntryPoint
public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    
    /** Email input field */
    private EditText emailInput, passwordInput;
    
    /** Login and Google Sign-In buttons */
    private Button loginButton, googleSignInButton;
    
    /** Text links for signup and forgot password */
    private TextView signupLink, forgotPasswordLink;
    
    /** Progress indicator for async operations */
    private ProgressBar progressBar;
    
    /** Firebase Authentication instance */
    private FirebaseAuth firebaseAuth;
    
    /** Firestore database instance */
    private FirebaseFirestore firestore;
    
    /** Google Sign-In client */
    private GoogleSignInClient googleSignInClient;
    
    /** Activity result launcher for Google Sign-In flow */
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        initializeViews();
        
        // Check if already logged in
        if (isLoggedIn()) {
            String currentUserId = getCurrentUserId();
            if (currentUserId == null || currentUserId.trim().isEmpty()) {
                preferenceManager.clearAllData();
                firebaseAuth.signOut();
            } else {
                routeAfterLogin(currentUserId, getCurrentUserRole());
                return;
            }
        }
        
        initializeGoogleSignIn();
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
            android.util.Log.e("LoginActivity", "Google Sign-In is not configured: invalid default_web_client_id");
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Register result launcher
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) {
                    hideLoading();
                    showToast("Google Sign-In canceled (no data)");
                    android.util.Log.w("LoginActivity", "Google Sign-In result data is null. resultCode=" + result.getResultCode());
                    return;
                }

                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleGoogleSignInResult(task);
            }
        );
    }
    
    private void initializeViews() {
        emailInput = binding.emailInput;
        passwordInput = binding.passwordInput;
        loginButton = binding.loginButton;
        googleSignInButton = binding.googleSignInButton;
        signupLink = binding.signupLink;
        forgotPasswordLink = binding.forgotPasswordLink;
        progressBar = binding.progressBar;
    }
    
    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> performLogin());
        
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        
        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });
        
        forgotPasswordLink.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }
    
    private void performLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        // Validation
        if (!ValidationUtils.isValidEmail(email)) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }
        
        if (!isNetworkAvailable()) {
            showNetworkError();
            return;
        }
        
        showLoading();
        
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        android.util.Log.d("LoginActivity", "Firebase Auth successful for user: " + user.getUid());
                        loadUserData(user.getUid());
                    } else {
                        hideLoading();
                        showToast("Authentication error. Please try again.");
                    }
                } else {
                    hideLoading();
                    Exception exception = task.getException();
                    String error = exception != null ? exception.getMessage() : "Login failed";
                    
                    android.util.Log.e("LoginActivity", "Login failed: " + error, exception);
                    
                    // Provide user-friendly error messages
                    if (error.contains("password")) {
                        showToast("Incorrect password. Please try again.");
                    } else if (error.contains("no user record")) {
                        showToast("No account found with this email. Please sign up.");
                    } else if (error.contains("network")) {
                        showToast("Network error. Please check your connection.");
                    } else if (error.contains("too many requests")) {
                        showToast("Too many failed attempts. Please try again later.");
                    } else {
                        showToast(error);
                    }
                }
            });
    }
    
    private void loadUserData(String userId) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                hideLoading();
                
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String email = document.getString("email");
                        String name = document.getString("name");
                        String role = document.getString("role");
                        
                        // Save user data to preferences
                        preferenceManager.saveUserData(userId, email, name, role);
                        
                        showToast("Login successful!");
                        routeAfterLogin(userId, role);
                    } else {
                        // User document doesn't exist - create it from Firebase Auth data
                        android.util.Log.w("LoginActivity", "User document not found for userId: " + userId + ". Creating document...");
                        createUserDocument(userId);
                    }
                } else {
                    // Network or permissions error
                    Exception exception = task.getException();
                    String errorMsg = exception != null ? exception.getMessage() : "Unknown error";
                    
                    android.util.Log.e("LoginActivity", "Failed to load user data: " + errorMsg, exception);
                    
                    // Check if it's a permissions error
                    if (errorMsg != null && errorMsg.toLowerCase(Locale.ROOT).contains("permission")) {
                        // Permissions error - try to create document anyway
                        android.util.Log.w("LoginActivity", "Permissions error detected. Attempting to create user document...");
                        createUserDocument(userId);
                    } else if (errorMsg != null && errorMsg.toLowerCase(Locale.ROOT).contains("network")) {
                        showToast("Network error. Please check your connection and try again.");
                    } else {
                        // Unknown error - try to create document as fallback
                        android.util.Log.w("LoginActivity", "Unknown error. Attempting to create user document as fallback...");
                        createUserDocument(userId);
                    }
                }
            });
    }
    
    /**
     * Creates a user document in Firestore from Firebase Auth data.
     * This handles cases where signup was interrupted or the document wasn't created.
     */
    private void createUserDocument(String userId) {
        showLoading();
        
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            hideLoading();
            showToast("Authentication error. Please try again.");
            return;
        }
        
        // Get user data from Firebase Auth
        String email = currentUser.getEmail();
        String name = currentUser.getDisplayName();
        
        // Use least-privilege default role when legacy records need recovery.
        String role = Constants.ROLE_CUSTOMER;
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", userId);
        userData.put("name", name != null ? name : email);
        userData.put("email", email);
        userData.put("phone", "");
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .set(userData)
            .addOnCompleteListener(task -> {
                hideLoading();
                
                if (task.isSuccessful()) {
                    // Save to preferences
                    preferenceManager.saveUserData(userId, email, 
                        name != null ? name : email, role);
                    
                    showToast("Login successful!");
                    routeAfterLogin(userId, role);
                } else {
                    showToast("Failed to create user profile. Please try again.");
                }
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
            android.util.Log.e("LoginActivity", "Google sign in failed. code=" + code, e);
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
                        checkAndCreateUserProfile(firebaseUser);
                    }
                } else {
                    hideLoading();
                    showToast("Authentication failed: " + 
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                }
            });
    }
    
    private void checkAndCreateUserProfile(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // User exists, save and navigate
                        String email = document.getString("email");
                        String name = document.getString("name");
                        String role = document.getString("role");
                        preferenceManager.saveUserData(userId, email, name, role);
                        routeAfterLogin(userId, role);
                    } else {
                        // New Google user, create profile with default role
                        createGoogleUserProfile(firebaseUser);
                    }
                } else {
                    hideLoading();
                    showToast("Failed to check user profile");
                }
            });
    }
    
    private void createGoogleUserProfile(FirebaseUser firebaseUser) {
        java.util.Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("id", firebaseUser.getUid());
        userData.put("name", firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User");
        userData.put("email", firebaseUser.getEmail());
        userData.put("phone", firebaseUser.getPhoneNumber() != null ? firebaseUser.getPhoneNumber() : "");
        userData.put("role", Constants.ROLE_CUSTOMER); // Default role for Google sign-in
        userData.put("createdAt", System.currentTimeMillis());
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(firebaseUser.getUid())
            .set(userData)
            .addOnCompleteListener(task -> {
                hideLoading();
                if (task.isSuccessful()) {
                    preferenceManager.saveUserData(
                        firebaseUser.getUid(),
                        firebaseUser.getEmail(),
                        firebaseUser.getDisplayName(),
                        Constants.ROLE_CUSTOMER
                    );
                    routeAfterLogin(firebaseUser.getUid(), Constants.ROLE_CUSTOMER);
                } else {
                    showToast("Failed to create user profile");
                }
            });
    }

    private void routeAfterLogin(String userId, String role) {
        if (userId == null || userId.trim().isEmpty()) {
            navigateToLogin();
            return;
        }

        if (role == null || role.trim().isEmpty()) {
            resolveRoleAndRoute(userId);
            return;
        }

        if (Constants.ROLE_PROVIDER.equals(role)) {
            checkProviderProfileAndRoute(userId);
            return;
        }

        hideLoading();
        navigateToCustomerHome();
    }

    private void resolveRoleAndRoute(String userId) {
        showLoading();
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String role = documentSnapshot != null && documentSnapshot.exists()
                    ? documentSnapshot.getString("role")
                    : null;
                if (role != null && !role.trim().isEmpty()) {
                    preferenceManager.setUserRole(role);
                    routeAfterLogin(userId, role);
                    return;
                }
                hideLoading();
                showToast("Account role is missing. Please sign in again.");
                preferenceManager.clearAllData();
                firebaseAuth.signOut();
                navigateToLogin();
            })
            .addOnFailureListener(e -> {
                hideLoading();
                showToast("Failed to load account role. Please try again.");
                navigateToLogin();
            });
    }

    private void checkProviderProfileAndRoute(String userId) {
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

        java.util.List<String> services = new java.util.ArrayList<>();
        Object rawServices = documentSnapshot.get("services");
        if (rawServices instanceof java.util.List) {
            java.util.List<?> casted = (java.util.List<?>) rawServices;
            for (Object item : casted) {
                if (item instanceof String) {
                    String value = safeTrim((String) item);
                    if (!value.isEmpty()) {
                        services.add(value);
                    }
                }
            }
        }
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
    
    private void navigateToProviderHome() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToCustomerHome() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra(Constants.EXTRA_SKIP_SPLASH_DELAY, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        googleSignInButton.setEnabled(false);
    }
    
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);
        googleSignInButton.setEnabled(isGoogleSignInConfigured());
    }

    private boolean isGoogleSignInConfigured() {
        String webClientId = getString(R.string.default_web_client_id);
        return webClientId != null
            && webClientId.endsWith(".apps.googleusercontent.com")
            && !webClientId.contains("PLACEHOLDER");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
