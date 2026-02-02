package com.dailyserviceapp.auth;

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
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.ValidationUtils;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
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
public class LoginActivity extends BaseActivity {
    
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
        setContentView(R.layout.activity_login);
        
        // Check if already logged in
        if (isLoggedIn()) {
            navigateToDashboard();
            return;
        }
        
        initializeFirebase();
        initializeGoogleSignIn();
        initializeViews();
        setupClickListeners();
    }
    
    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }
    
    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Register result launcher
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleGoogleSignInResult(task);
                } else {
                    hideLoading();
                    showToast("Google Sign-In cancelled");
                }
            }
        );
    }
    
    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        signupLink = findViewById(R.id.signupLink);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        progressBar = findViewById(R.id.progressBar);
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
    
    /**
     * Validates credentials and signs the user in with Firebase using the entered email and password.
     *
     * <p>Performs client-side validation of email and password, checks network availability, shows a loading
     * indicator, attempts Firebase email/password authentication, and on successful authentication
     * proceeds to load the user's profile. On failure, hides the loading indicator, logs the error, and
     * displays an appropriate user-facing message (e.g., incorrect password, no account found, network
     * issues, too many attempts).</p>
     */
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
    
    /**
     * Loads the user's profile document from Firestore and advances the app flow based on the result.
     *
     * If the document exists, the method persists the user's id, email, name, and role to preferences,
     * shows a success toast, and navigates to the dashboard. If the document does not exist or Firestore
     * access is not permitted, the method attempts to create a user document from Firebase Auth data.
     * If a network error occurs, a network error toast is shown.
     *
     * @param userId the Firebase UID of the user whose profile should be loaded
     */
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
                        navigateToDashboard();
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
                    if (errorMsg != null && errorMsg.toLowerCase().contains("permission")) {
                        // Permissions error - try to create document anyway
                        android.util.Log.w("LoginActivity", "Permissions error detected. Attempting to create user document...");
                        createUserDocument(userId);
                    } else if (errorMsg != null && errorMsg.toLowerCase().contains("network")) {
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
     * Create a Firestore user document from the current Firebase Auth account and persist the profile on success.
     *
     * Shows a loading indicator while constructing a user document (id, name, email, phone, role, createdAt) using Firebase Auth values with sensible defaults.
     * If no authenticated Firebase user exists, the method hides loading and shows an authentication error toast.
     * On successful write the method saves the user data to preferences, shows a success toast, and navigates to the dashboard.
     * On failure the method hides loading and shows a failure toast.
     *
     * @param userId the Firebase UID to use as the Firestore document ID
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
        
        // Default to PROVIDER role if not specified
        String role = "PROVIDER";
        
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
                    navigateToDashboard();
                } else {
                    showToast("Failed to create user profile. Please try again.");
                }
            });
    }
    
    private void signInWithGoogle() {
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
            android.util.Log.e("LoginActivity", "Google sign in failed", e);
            showToast("Google sign-in failed: " + e.getMessage());
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
                        navigateToDashboard();
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
                    navigateToDashboard();
                } else {
                    showToast("Failed to create user profile");
                }
            });
    }
    
    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
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
        googleSignInButton.setEnabled(true);
    }
}