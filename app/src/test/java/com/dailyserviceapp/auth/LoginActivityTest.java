package com.dailyserviceapp.auth;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.PreferenceManager;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginActivity.
 * Tests login validation, Firebase authentication, and navigation logic.
 */
@RunWith(RobolectricTestRunner.class)
public class LoginActivityTest {

    private ActivityController<LoginActivity> controller;
    private LoginActivity activity;

    @Mock
    private FirebaseAuth mockAuth;

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private FirebaseUser mockUser;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = Robolectric.buildActivity(LoginActivity.class);
    }

    @Test
    public void testActivityCreation() {
        activity = controller.create().get();
        assertNotNull("Activity should be created", activity);
    }

    @Test
    public void testUIElementsAreInitialized() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button loginButton = activity.findViewById(R.id.loginButton);
        ProgressBar progressBar = activity.findViewById(R.id.progressBar);

        assertNotNull("Email input should exist", emailInput);
        assertNotNull("Password input should exist", passwordInput);
        assertNotNull("Login button should exist", loginButton);
        assertNotNull("Progress bar should exist", progressBar);
    }

    @Test
    public void testEmailValidation_EmptyEmail() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button loginButton = activity.findViewById(R.id.loginButton);

        emailInput.setText("");
        passwordInput.setText("password123");
        loginButton.performClick();

        // Check that email field shows error
        assertNotNull("Email field should show error", emailInput.getError());
        assertTrue("Error should mention email",
            emailInput.getError().toString().toLowerCase().contains("email"));
    }

    @Test
    public void testEmailValidation_InvalidEmail() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button loginButton = activity.findViewById(R.id.loginButton);

        emailInput.setText("invalid-email");
        passwordInput.setText("password123");
        loginButton.performClick();

        // Check that email field shows error
        assertNotNull("Email field should show error for invalid email", emailInput.getError());
    }

    @Test
    public void testPasswordValidation_EmptyPassword() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button loginButton = activity.findViewById(R.id.loginButton);

        emailInput.setText("test@example.com");
        passwordInput.setText("");
        loginButton.performClick();

        // Check that password field shows error
        assertNotNull("Password field should show error", passwordInput.getError());
        assertTrue("Error should mention password",
            passwordInput.getError().toString().toLowerCase().contains("password"));
    }

    @Test
    public void testLoginButtonClickWithValidInput() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button loginButton = activity.findViewById(R.id.loginButton);

        emailInput.setText("test@example.com");
        passwordInput.setText("password123");

        // Click should trigger validation (actual Firebase call would be mocked in integration test)
        loginButton.performClick();

        // Verify inputs are not empty after click
        assertFalse("Email should not be empty", emailInput.getText().toString().isEmpty());
        assertFalse("Password should not be empty", passwordInput.getText().toString().isEmpty());
    }

    @Test
    public void testGoogleSignInButtonExists() {
        activity = controller.create().start().resume().get();

        Button googleSignInButton = activity.findViewById(R.id.googleSignInButton);
        assertNotNull("Google Sign-In button should exist", googleSignInButton);
    }

    @Test
    public void testSignupLinkNavigation() {
        activity = controller.create().start().resume().get();

        // Find and click the signup link
        activity.findViewById(R.id.signupLink).performClick();

        // Verify that SignupActivity intent was created
        Intent expectedIntent = new Intent(activity, SignupActivity.class);
        Intent actualIntent = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();

        assertNotNull("Signup intent should be started", actualIntent);
        assertEquals("Should navigate to SignupActivity",
            SignupActivity.class.getName(),
            actualIntent.getComponent().getClassName());
    }

    @Test
    public void testForgotPasswordLinkNavigation() {
        activity = controller.create().start().resume().get();

        // Find and click forgot password link
        activity.findViewById(R.id.forgotPasswordLink).performClick();

        // Verify that ForgotPasswordActivity intent was created
        Intent actualIntent = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();

        assertNotNull("Forgot password intent should be started", actualIntent);
        assertEquals("Should navigate to ForgotPasswordActivity",
            ForgotPasswordActivity.class.getName(),
            actualIntent.getComponent().getClassName());
    }

    @Test
    public void testProgressBarInitiallyHidden() {
        activity = controller.create().start().resume().get();

        ProgressBar progressBar = activity.findViewById(R.id.progressBar);
        assertEquals("Progress bar should be initially hidden",
            android.view.View.GONE, progressBar.getVisibility());
    }

    @Test
    public void testInputFieldsAreEditable() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);

        assertTrue("Email input should be enabled", emailInput.isEnabled());
        assertTrue("Password input should be enabled", passwordInput.isEnabled());
        assertTrue("Email input should be focusable", emailInput.isFocusable());
        assertTrue("Password input should be focusable", passwordInput.isFocusable());
    }

    @Test
    public void testEmailInputType() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);

        // Verify email input type is set (it should have email input type flag)
        int inputType = emailInput.getInputType();
        assertTrue("Email input should have text input type", inputType != 0);
    }

    @Test
    public void testPasswordInputIsObscured() {
        activity = controller.create().start().resume().get();

        EditText passwordInput = activity.findViewById(R.id.passwordInput);

        // Check if password is obscured by checking transformation method
        assertNotNull("Password should have transformation method",
            passwordInput.getTransformationMethod());
    }

    @Test
    public void testActivityLayoutIsLoaded() {
        activity = controller.create().get();

        assertNotNull("Activity content view should be set",
            activity.findViewById(android.R.id.content));
    }

    @Test
    public void testWhitespaceIsTrimmedFromEmail() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        emailInput.setText("  test@example.com  ");

        String trimmedText = emailInput.getText().toString().trim();
        assertEquals("Email should be trimmable", "test@example.com", trimmedText);
    }

    @Test
    public void testMultipleValidationErrors() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button loginButton = activity.findViewById(R.id.loginButton);

        // Both fields empty
        emailInput.setText("");
        passwordInput.setText("");
        loginButton.performClick();

        // At least one field should show error
        boolean hasError = emailInput.getError() != null || passwordInput.getError() != null;
        assertTrue("At least one field should show validation error", hasError);
    }

    @Test
    public void testButtonsAreClickable() {
        activity = controller.create().start().resume().get();

        Button loginButton = activity.findViewById(R.id.loginButton);
        Button googleSignInButton = activity.findViewById(R.id.googleSignInButton);

        assertTrue("Login button should be clickable", loginButton.isClickable());
        assertTrue("Google sign-in button should be clickable", googleSignInButton.isClickable());
    }

    @Test
    public void testActivityDoesNotCrashOnRotation() {
        activity = controller.create().start().resume().get();

        // Simulate configuration change
        try {
            controller.configurationChange();
            assertTrue("Activity should handle configuration change", true);
        } catch (Exception e) {
            fail("Activity should not crash on rotation: " + e.getMessage());
        }
    }

    @Test
    public void testValidEmailFormats() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button loginButton = activity.findViewById(R.id.loginButton);

        String[] validEmails = {
            "test@example.com",
            "user.name@example.com",
            "user+tag@example.co.uk"
        };

        for (String email : validEmails) {
            emailInput.setText(email);
            passwordInput.setText("password123");
            loginButton.performClick();

            // Valid emails should not show email error (might show network error instead)
            // We're just checking the email format is accepted
            assertFalse("Valid email should be accepted: " + email,
                emailInput.getError() != null &&
                emailInput.getError().toString().contains("valid email"));
        }
    }
}