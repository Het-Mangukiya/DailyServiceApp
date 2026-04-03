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
import com.dailyserviceapp.notifications.FCMService;
import com.dailyserviceapp.customer.CustomerHomeActivity;
import com.dailyserviceapp.ui.ThemePreviewActivity;
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
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Login Activity for DailyDrop application.
 * Provides email/password authentication and Google Sign-In functionality.
 */
@AndroidEntryPoint
public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    
    private EditText emailInput, passwordInput;
    private Button loginButton, googleSignInButton;
    private TextView signupLink, forgotPasswordLink;
    private ProgressBar progressBar;
    private View themePreviewButton;
    
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private GoogleSignInClient googleSignInClient;
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
                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleGoogleSignInResult(task);
            }
        );
    }
    
    private void initializeViews() {
        emailInput = binding.edtEmail;
        passwordInput = binding.edtPassword;
        loginButton = binding.btnLogin;
        googleSignInButton = binding.googleSignInButton;
        signupLink = binding.txtSignUp;
        forgotPasswordLink = binding.txtForgotPassword;
        progressBar = binding.progressBar;
        themePreviewButton = binding.btnThemePreview;
    }
    
    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> performLogin());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        signupLink.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));
        forgotPasswordLink.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        themePreviewButton.setOnClickListener(v -> startActivity(ThemePreviewActivity.createIntent(this)));
    }
    
    private void performLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
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
                    } else {
                        hideLoading();
                        showToast("Authentication error. Please try again.");
                    }
                } else {
                    hideLoading();
                    showToast(task.getException() != null ? task.getException().getMessage() : "Login failed");
                }
            });
    }
    
    private void loadUserData(String userId) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String email = document.getString("email");
                        String name = document.getString("name");
                        String role = document.getString("role");
                        
                        preferenceManager.saveUserData(userId, email, name, role);
                        routeAfterLogin(userId, role);
                    } else {
                        createUserDocument(userId);
                    }
                } else {
                    hideLoading();
                    showToast("Failed to load user data. Please try again.");
                }
            });
    }
    
    private void createUserDocument(String userId) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            hideLoading();
            showToast("Authentication error. Please try again.");
            return;
        }
        
        String email = currentUser.getEmail();
        String name = currentUser.getDisplayName();
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
                if (task.isSuccessful()) {
                    preferenceManager.saveUserData(userId, email, name != null ? name : email, role);
                    routeAfterLogin(userId, role);
                } else {
                    hideLoading();
                    showToast("Failed to create user profile.");
                }
            });
    }
    
    private void signInWithGoogle() {
        if (!isGoogleSignInConfigured() || googleSignInClient == null) {
            showToast("Google Sign-In is not configured.");
            return;
        }
        if (!isNetworkAvailable()) {
            showNetworkError();
            return;
        }
        showLoading();
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }
    
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            hideLoading();
            showToast("Google sign in failed.");
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
                    showToast("Authentication failed.");
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
                        String email = document.getString("email");
                        String name = document.getString("name");
                        String role = document.getString("role");
                        preferenceManager.saveUserData(userId, email, name, role);
                        routeAfterLogin(userId, role);
                    } else {
                        createGoogleUserProfile(firebaseUser);
                    }
                } else {
                    hideLoading();
                    showToast("Failed to check user profile");
                }
            });
    }
    
    private void createGoogleUserProfile(FirebaseUser firebaseUser) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", firebaseUser.getUid());
        userData.put("name", firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User");
        userData.put("email", firebaseUser.getEmail());
        userData.put("role", Constants.ROLE_CUSTOMER);
        userData.put("createdAt", System.currentTimeMillis());
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(firebaseUser.getUid())
            .set(userData)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    preferenceManager.saveUserData(firebaseUser.getUid(), firebaseUser.getEmail(), firebaseUser.getDisplayName(), Constants.ROLE_CUSTOMER);
                    routeAfterLogin(firebaseUser.getUid(), Constants.ROLE_CUSTOMER);
                } else {
                    hideLoading();
                    showToast("Failed to create user profile");
                }
            });
    }

    private void routeAfterLogin(String userId, String role) {
        saveFcmTokenIfAvailable();
        
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

    private void saveFcmTokenIfAvailable() {
        FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> {
                if (token != null && !token.trim().isEmpty()) {
                    FCMService.saveTokenToFirestore(token);
                }
            });
    }

    private void resolveRoleAndRoute(String userId) {
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
                } else {
                    hideLoading();
                    preferenceManager.clearAllData();
                    firebaseAuth.signOut();
                    showToast("Account role is missing.");
                }
            })
            .addOnFailureListener(e -> {
                hideLoading();
                showToast("Failed to resolve role.");
            });
    }

    private void checkProviderProfileAndRoute(String userId) {
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
        return !businessName.isEmpty() && !ownerName.isEmpty() && !phone.isEmpty() && !address.isEmpty();
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
        Intent intent = new Intent(this, CustomerHomeActivity.class);
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
        return webClientId != null && webClientId.endsWith(".apps.googleusercontent.com");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
