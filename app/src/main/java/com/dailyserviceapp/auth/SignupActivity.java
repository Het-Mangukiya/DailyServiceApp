package com.dailyserviceapp.auth;

import dagger.hilt.android.AndroidEntryPoint;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.dailyserviceapp.R;
import com.dailyserviceapp.SplashActivity;
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
@AndroidEntryPoint
public class SignupActivity extends BaseActivity {

    private ActivitySignupBinding binding;
    
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
        initializeToolbar();
        initializeGoogleSignIn();
        setupRoleSpinner();
        setupClickListeners();
    }
    
    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeToolbar() {
        // Assume binding.topHeader is available, but setupToolbar uses the actual toolbar view
        // The layout doesn't have a Toolbar with id 'toolbar', it uses binding.topHeader
        // Looking at activity_signup.xml, there is no Toolbar tag.
        // Let's check if there is a toolbar in the layout.
    }
    
    private void initializeGoogleSignIn() {
        if (!isGoogleSignInConfigured()) {
            binding.googleSignInButton.setEnabled(false);
            binding.googleSignInButton.setAlpha(0.5f);
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
                    return;
                }

                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleGoogleSignInResult(task);
            }
        );
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
        binding.roleSpinner.setAdapter(adapter);
        binding.roleSpinner.setSelection(0, false);
    }
    
    private void setupClickListeners() {
        binding.signupButton.setOnClickListener(v -> performSignup());
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        binding.loginLink.setOnClickListener(v -> finish());
    }
    
    private void performSignup() {
        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String phoneRaw = binding.phoneInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordInput.getText().toString().trim();
        String selectedRole = binding.roleSpinner.getSelectedItemPosition() == 0 ? 
            Constants.ROLE_PROVIDER : Constants.ROLE_CUSTOMER;
        
        // Validation
        if (!ValidationUtils.isValidName(name)) {
            binding.nameInput.setError("Please enter a valid name");
            binding.nameInput.requestFocus();
            return;
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            binding.emailInput.setError("Please enter a valid email");
            binding.emailInput.requestFocus();
            return;
        }
        
        if (!ValidationUtils.isValidPhone(phoneRaw)) {
            binding.phoneInput.setError("Please enter a valid phone number");
            binding.phoneInput.requestFocus();
            return;
        }
        
        String phone = ValidationUtils.normalizePhoneNumber(phoneRaw);
        
        if (!ValidationUtils.isValidPassword(password)) {
            binding.passwordInput.setError("Password must be at least 8 characters with letters and numbers");
            binding.passwordInput.requestFocus();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordInput.setError("Passwords do not match");
            binding.confirmPasswordInput.requestFocus();
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
                        preferenceManager.saveUserData(firebaseUser.getUid(), email, name, selectedRole);
                        createUserDocument(firebaseUser.getUid(), name, email, phone, selectedRole);
                    } else {
                        hideLoading();
                        showToast("Signup failed: user not available. Please try again.");
                    }
                } else {
                    hideLoading();
                    String error = getAuthErrorMessage(task.getException());
                    showToast(error);
                }
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
                    showToast("Account created, but profile sync failed.");
                    navigateAfterSignup(userId, role, true);
                }
            });
    }
    
    private void signInWithGoogle() {
        if (!isGoogleSignInConfigured() || googleSignInClient == null) {
            showToast("Google Sign-In is not configured yet.");
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
            if (code == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                showToast("Google Sign-In canceled");
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
                        checkAndCreateGoogleUserProfile(firebaseUser);
                    }
                } else {
                    hideLoading();
                    showToast("Authentication failed");
                }
            });
    }
    
    private void checkAndCreateGoogleUserProfile(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    if (task.getResult().exists()) {
                        String existingEmail = task.getResult().getString("email");
                        String existingName = task.getResult().getString("name");
                        String existingRole = task.getResult().getString("role");
                        preferenceManager.saveUserData(userId, existingEmail, existingName, existingRole);
                        navigateAfterSignup(userId, existingRole, false);
                    } else {
                        String selectedRole = binding.roleSpinner.getSelectedItemPosition() == 0 ? 
                            Constants.ROLE_PROVIDER : Constants.ROLE_CUSTOMER;
                        createGoogleUserDocument(firebaseUser, selectedRole);
                    }
                } else {
                    hideLoading();
                    preferenceManager.saveUserData(userId, firebaseUser.getEmail(), firebaseUser.getDisplayName(), Constants.ROLE_CUSTOMER);
                    navigateToCustomerHome();
                }
            });
    }
    
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
                    preferenceManager.saveUserData(firebaseUser.getUid(), firebaseUser.getEmail(), firebaseUser.getDisplayName(), role);
                    showToast("Account created successfully!");
                    navigateAfterSignup(firebaseUser.getUid(), role, true);
                } else {
                    preferenceManager.saveUserData(firebaseUser.getUid(), firebaseUser.getEmail(), firebaseUser.getDisplayName(), role);
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
        Intent intent = new Intent(this, com.dailyserviceapp.dashboard.DashboardActivity.class);
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
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.signupButton.setEnabled(false);
        binding.googleSignInButton.setEnabled(false);
    }
    
    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
        binding.signupButton.setEnabled(true);
        binding.googleSignInButton.setEnabled(isGoogleSignInConfigured());
    }

    private boolean isGoogleSignInConfigured() {
        String webClientId = getString(R.string.default_web_client_id);
        return webClientId != null && webClientId.endsWith(".apps.googleusercontent.com");
    }

    private String getAuthErrorMessage(Exception exception) {
        if (exception == null) return "Signup failed.";
        String message = exception.getMessage();
        if (exception instanceof com.google.firebase.auth.FirebaseAuthException) {
            String code = ((com.google.firebase.auth.FirebaseAuthException) exception).getErrorCode();
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) return "Email already registered.";
        }
        return message != null ? message : "Signup failed.";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
