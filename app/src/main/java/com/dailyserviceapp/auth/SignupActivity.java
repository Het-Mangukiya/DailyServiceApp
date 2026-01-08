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

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.ValidationUtils;
import com.dailyserviceapp.data.models.User;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends BaseActivity {
    
    private EditText nameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    private Spinner roleSpinner;
    private Button signupButton, googleSignInButton;
    private TextView loginLink;
    private ProgressBar progressBar;
    
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private GoogleSignInClient googleSignInClient;
    
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        
        initializeFirebase();
        initializeGoogleSignIn();
        initializeViews();
        setupRoleSpinner();
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
        
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleGoogleSignInResult(task);
                } else {
                    hideLoading();
                }
            }
        );
    }
    
    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        roleSpinner = findViewById(R.id.roleSpinner);
        signupButton = findViewById(R.id.signupButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        loginLink = findViewById(R.id.loginLink);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setupRoleSpinner() {
        String[] roles = {"Service Provider", "Customer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            android.R.layout.simple_spinner_item, 
            roles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
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
        String phone = phoneInput.getText().toString().trim();
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
        
        if (!ValidationUtils.isValidPhone(phone)) {
            phoneInput.setError("Please enter a valid 10-digit phone number");
            phoneInput.requestFocus();
            return;
        }
        
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
                        createUserDocument(firebaseUser.getUid(), name, email, phone, selectedRole);
                    }
                } else {
                    hideLoading();
                    String error = "Signup failed";
                    if (task.getException() != null) {
                        error = task.getException().getMessage();
                        // Log the full error for debugging
                        android.util.Log.e("SignupActivity", "Signup error: " + error, task.getException());
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
                    
                    // Navigate to login
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    showToast("Failed to create user profile");
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
            android.util.Log.e("SignupActivity", "Google sign in failed", e);
            showToast("Google sign-in failed");
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                hideLoading();
                if (task.isSuccessful()) {
                    showToast("Account created successfully!");
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    showToast("Authentication failed");
                }
            });
    }
    
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);
        googleSignInButton.setEnabled(false);
    }
    
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        signupButton.setEnabled(true);
        googleSignInButton.setEnabled(true);
    }
}
