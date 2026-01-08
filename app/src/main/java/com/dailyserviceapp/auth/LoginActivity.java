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

public class LoginActivity extends BaseActivity {
    
    private EditText emailInput, passwordInput;
    private Button loginButton, googleSignInButton;
    private TextView signupLink, forgotPasswordLink;
    private ProgressBar progressBar;
    
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private GoogleSignInClient googleSignInClient;
    
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
                        loadUserData(user.getUid());
                    }
                } else {
                    hideLoading();
                    String error = task.getException() != null ? 
                        task.getException().getMessage() : "Login failed";
                    showToast(error);
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
                        navigateToDashboard();
                    } else {
                        showToast("User data not found");
                    }
                } else {
                    showToast("Failed to load user data");
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
