package com.dailyserviceapp.auth;

import dagger.hilt.android.AndroidEntryPoint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.ValidationUtils;
import com.google.firebase.auth.FirebaseAuth;

@AndroidEntryPoint
public class ForgotPasswordActivity extends BaseActivity {
    
    private EditText emailInput;
    private Button resetPasswordButton;
    private ProgressBar progressBar;
    
    private FirebaseAuth firebaseAuth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        
        firebaseAuth = FirebaseAuth.getInstance();
        
        initializeViews();
        setupClickListeners();
    }
    
    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setupClickListeners() {
        resetPasswordButton.setOnClickListener(v -> sendPasswordResetEmail());
    }
    
    private void sendPasswordResetEmail() {
        String email = emailInput.getText().toString().trim();
        
        if (!ValidationUtils.isValidEmail(email)) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return;
        }
        
        if (!isNetworkAvailable()) {
            showNetworkError();
            return;
        }
        
        showLoading();
        
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                hideLoading();
                
                if (task.isSuccessful()) {
                    showToast("Password reset email sent! Check your inbox.");
                    finish();
                } else {
                    String error = task.getException() != null ? 
                        task.getException().getMessage() : "Failed to send reset email";
                    showToast(error);
                }
            });
    }
    
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        resetPasswordButton.setEnabled(false);
    }
    
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        resetPasswordButton.setEnabled(true);
    }
}
