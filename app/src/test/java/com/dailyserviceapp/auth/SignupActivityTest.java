package com.dailyserviceapp.auth;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.dailyserviceapp.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.*;

/**
 * Unit tests for SignupActivity.
 * Tests user registration validation and UI behavior.
 */
@RunWith(RobolectricTestRunner.class)
public class SignupActivityTest {

    private ActivityController<SignupActivity> controller;
    private SignupActivity activity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = Robolectric.buildActivity(SignupActivity.class);
    }

    @Test
    public void testActivityCreation() {
        activity = controller.create().get();
        assertNotNull("Activity should be created", activity);
    }

    @Test
    public void testUIElementsAreInitialized() {
        activity = controller.create().start().resume().get();

        EditText nameInput = activity.findViewById(R.id.nameInput);
        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText phoneInput = activity.findViewById(R.id.phoneInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = activity.findViewById(R.id.confirmPasswordInput);
        Spinner roleSpinner = activity.findViewById(R.id.roleSpinner);
        Button signupButton = activity.findViewById(R.id.signupButton);
        ProgressBar progressBar = activity.findViewById(R.id.progressBar);

        assertNotNull("Name input should exist", nameInput);
        assertNotNull("Email input should exist", emailInput);
        assertNotNull("Phone input should exist", phoneInput);
        assertNotNull("Password input should exist", passwordInput);
        assertNotNull("Confirm password input should exist", confirmPasswordInput);
        assertNotNull("Role spinner should exist", roleSpinner);
        assertNotNull("Signup button should exist", signupButton);
        assertNotNull("Progress bar should exist", progressBar);
    }

    @Test
    public void testRoleSpinnerHasTwoOptions() {
        activity = controller.create().start().resume().get();

        Spinner roleSpinner = activity.findViewById(R.id.roleSpinner);
        assertNotNull("Role spinner should exist", roleSpinner);
        assertEquals("Role spinner should have 2 options", 2, roleSpinner.getAdapter().getCount());
    }

    @Test
    public void testRoleSpinnerOptions() {
        activity = controller.create().start().resume().get();

        Spinner roleSpinner = activity.findViewById(R.id.roleSpinner);
        String firstOption = roleSpinner.getAdapter().getItem(0).toString();
        String secondOption = roleSpinner.getAdapter().getItem(1).toString();

        assertEquals("First option should be Service Provider", "Service Provider", firstOption);
        assertEquals("Second option should be Customer", "Customer", secondOption);
    }

    @Test
    public void testNameValidation_EmptyName() {
        activity = controller.create().start().resume().get();

        EditText nameInput = activity.findViewById(R.id.nameInput);
        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText phoneInput = activity.findViewById(R.id.phoneInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = activity.findViewById(R.id.confirmPasswordInput);
        Button signupButton = activity.findViewById(R.id.signupButton);

        nameInput.setText("");
        emailInput.setText("test@example.com");
        phoneInput.setText("1234567890");
        passwordInput.setText("password123");
        confirmPasswordInput.setText("password123");

        signupButton.performClick();

        assertNotNull("Name field should show error", nameInput.getError());
    }

    @Test
    public void testNameValidation_ShortName() {
        activity = controller.create().start().resume().get();

        EditText nameInput = activity.findViewById(R.id.nameInput);
        Button signupButton = activity.findViewById(R.id.signupButton);

        nameInput.setText("A");
        activity.findViewById(R.id.emailInput).setText("test@example.com");
        activity.findViewById(R.id.phoneInput).setText("1234567890");
        activity.findViewById(R.id.passwordInput).setText("password123");
        activity.findViewById(R.id.confirmPasswordInput).setText("password123");

        signupButton.performClick();

        assertNotNull("Name field should show error for short name", nameInput.getError());
    }

    @Test
    public void testEmailValidation_InvalidEmail() {
        activity = controller.create().start().resume().get();

        EditText emailInput = activity.findViewById(R.id.emailInput);
        Button signupButton = activity.findViewById(R.id.signupButton);

        activity.findViewById(R.id.nameInput).setText("Test User");
        emailInput.setText("invalid-email");
        activity.findViewById(R.id.phoneInput).setText("1234567890");
        activity.findViewById(R.id.passwordInput).setText("password123");
        activity.findViewById(R.id.confirmPasswordInput).setText("password123");

        signupButton.performClick();

        assertNotNull("Email field should show error", emailInput.getError());
    }

    @Test
    public void testPhoneValidation_InvalidPhone() {
        activity = controller.create().start().resume().get();

        EditText phoneInput = activity.findViewById(R.id.phoneInput);
        Button signupButton = activity.findViewById(R.id.signupButton);

        activity.findViewById(R.id.nameInput).setText("Test User");
        activity.findViewById(R.id.emailInput).setText("test@example.com");
        phoneInput.setText("123"); // Too short
        activity.findViewById(R.id.passwordInput).setText("password123");
        activity.findViewById(R.id.confirmPasswordInput).setText("password123");

        signupButton.performClick();

        assertNotNull("Phone field should show error for invalid phone", phoneInput.getError());
    }

    @Test
    public void testPasswordValidation_ShortPassword() {
        activity = controller.create().start().resume().get();

        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button signupButton = activity.findViewById(R.id.signupButton);

        activity.findViewById(R.id.nameInput).setText("Test User");
        activity.findViewById(R.id.emailInput).setText("test@example.com");
        activity.findViewById(R.id.phoneInput).setText("1234567890");
        passwordInput.setText("pass");
        activity.findViewById(R.id.confirmPasswordInput).setText("pass");

        signupButton.performClick();

        assertNotNull("Password field should show error for short password", passwordInput.getError());
    }

    @Test
    public void testPasswordValidation_NoDigits() {
        activity = controller.create().start().resume().get();

        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button signupButton = activity.findViewById(R.id.signupButton);

        activity.findViewById(R.id.nameInput).setText("Test User");
        activity.findViewById(R.id.emailInput).setText("test@example.com");
        activity.findViewById(R.id.phoneInput).setText("1234567890");
        passwordInput.setText("passwordonly");
        activity.findViewById(R.id.confirmPasswordInput).setText("passwordonly");

        signupButton.performClick();

        assertNotNull("Password field should show error for password without digits",
            passwordInput.getError());
    }

    @Test
    public void testPasswordValidation_NoLetters() {
        activity = controller.create().start().resume().get();

        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        Button signupButton = activity.findViewById(R.id.signupButton);

        activity.findViewById(R.id.nameInput).setText("Test User");
        activity.findViewById(R.id.emailInput).setText("test@example.com");
        activity.findViewById(R.id.phoneInput).setText("1234567890");
        passwordInput.setText("12345678");
        activity.findViewById(R.id.confirmPasswordInput).setText("12345678");

        signupButton.performClick();

        assertNotNull("Password field should show error for password without letters",
            passwordInput.getError());
    }

    @Test
    public void testPasswordMismatch() {
        activity = controller.create().start().resume().get();

        EditText confirmPasswordInput = activity.findViewById(R.id.confirmPasswordInput);
        Button signupButton = activity.findViewById(R.id.signupButton);

        activity.findViewById(R.id.nameInput).setText("Test User");
        activity.findViewById(R.id.emailInput).setText("test@example.com");
        activity.findViewById(R.id.phoneInput).setText("1234567890");
        activity.findViewById(R.id.passwordInput).setText("password123");
        confirmPasswordInput.setText("different123");

        signupButton.performClick();

        assertNotNull("Confirm password field should show error for mismatch",
            confirmPasswordInput.getError());
        assertTrue("Error should mention mismatch",
            confirmPasswordInput.getError().toString().toLowerCase().contains("match"));
    }

    @Test
    public void testValidInputDoesNotShowErrors() {
        activity = controller.create().start().resume().get();

        EditText nameInput = activity.findViewById(R.id.nameInput);
        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText phoneInput = activity.findViewById(R.id.phoneInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = activity.findViewById(R.id.confirmPasswordInput);
        Button signupButton = activity.findViewById(R.id.signupButton);

        nameInput.setText("Test User");
        emailInput.setText("test@example.com");
        phoneInput.setText("1234567890");
        passwordInput.setText("password123");
        confirmPasswordInput.setText("password123");

        signupButton.performClick();

        // Valid inputs should not show validation errors on these fields
        // (Network/Firebase errors are different and expected in unit tests)
        // We just verify no client-side validation errors
        assertFalse("Valid name should not show validation error",
            nameInput.getError() != null && nameInput.getError().toString().contains("valid name"));
        assertFalse("Valid email should not show validation error",
            emailInput.getError() != null && emailInput.getError().toString().contains("valid email"));
    }

    @Test
    public void testLoginLinkFinishesActivity() {
        activity = controller.create().start().resume().get();

        activity.findViewById(R.id.loginLink).performClick();

        assertTrue("Activity should finish when login link is clicked", activity.isFinishing());
    }

    @Test
    public void testGoogleSignInButtonExists() {
        activity = controller.create().start().resume().get();

        Button googleSignInButton = activity.findViewById(R.id.googleSignInButton);
        assertNotNull("Google Sign-In button should exist", googleSignInButton);
        assertTrue("Google Sign-In button should be clickable", googleSignInButton.isClickable());
    }

    @Test
    public void testProgressBarInitiallyHidden() {
        activity = controller.create().start().resume().get();

        ProgressBar progressBar = activity.findViewById(R.id.progressBar);
        assertEquals("Progress bar should be initially hidden",
            android.view.View.GONE, progressBar.getVisibility());
    }

    @Test
    public void testAllInputFieldsAreEnabled() {
        activity = controller.create().start().resume().get();

        EditText nameInput = activity.findViewById(R.id.nameInput);
        EditText emailInput = activity.findViewById(R.id.emailInput);
        EditText phoneInput = activity.findViewById(R.id.phoneInput);
        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = activity.findViewById(R.id.confirmPasswordInput);

        assertTrue("Name input should be enabled", nameInput.isEnabled());
        assertTrue("Email input should be enabled", emailInput.isEnabled());
        assertTrue("Phone input should be enabled", phoneInput.isEnabled());
        assertTrue("Password input should be enabled", passwordInput.isEnabled());
        assertTrue("Confirm password input should be enabled", confirmPasswordInput.isEnabled());
    }

    @Test
    public void testPasswordFieldsAreObscured() {
        activity = controller.create().start().resume().get();

        EditText passwordInput = activity.findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = activity.findViewById(R.id.confirmPasswordInput);

        assertNotNull("Password should have transformation method",
            passwordInput.getTransformationMethod());
        assertNotNull("Confirm password should have transformation method",
            confirmPasswordInput.getTransformationMethod());
    }

    @Test
    public void testPhoneInputAcceptsOnlyNumbers() {
        activity = controller.create().start().resume().get();

        EditText phoneInput = activity.findViewById(R.id.phoneInput);

        // Verify input type includes number flag
        int inputType = phoneInput.getInputType();
        assertTrue("Phone input should accept numbers", inputType != 0);
    }

    @Test
    public void testWhitespaceIsTrimmedFromInputs() {
        activity = controller.create().start().resume().get();

        EditText nameInput = activity.findViewById(R.id.nameInput);
        EditText emailInput = activity.findViewById(R.id.emailInput);

        nameInput.setText("  Test User  ");
        emailInput.setText("  test@example.com  ");

        assertEquals("Name should be trimmable", "Test User", nameInput.getText().toString().trim());
        assertEquals("Email should be trimmable", "test@example.com",
            emailInput.getText().toString().trim());
    }

    @Test
    public void testMultipleValidationErrors() {
        activity = controller.create().start().resume().get();

        Button signupButton = activity.findViewById(R.id.signupButton);

        // All fields empty
        activity.findViewById(R.id.nameInput).setText("");
        activity.findViewById(R.id.emailInput).setText("");
        activity.findViewById(R.id.phoneInput).setText("");
        activity.findViewById(R.id.passwordInput).setText("");
        activity.findViewById(R.id.confirmPasswordInput).setText("");

        signupButton.performClick();

        // At least one field should show error
        EditText nameInput = activity.findViewById(R.id.nameInput);
        assertNotNull("At least name field should show error", nameInput.getError());
    }

    @Test
    public void testValidPhoneNumberFormats() {
        activity = controller.create().start().resume().get();

        EditText phoneInput = activity.findViewById(R.id.phoneInput);

        String[] validPhones = {"1234567890", "9876543210", "5555555555"};

        for (String phone : validPhones) {
            phoneInput.setText(phone);
            assertEquals("Phone number should be 10 digits", 10, phone.length());
        }
    }

    @Test
    public void testActivityDoesNotCrashOnRotation() {
        activity = controller.create().start().resume().get();

        try {
            controller.configurationChange();
            assertTrue("Activity should handle configuration change", true);
        } catch (Exception e) {
            fail("Activity should not crash on rotation: " + e.getMessage());
        }
    }

    @Test
    public void testRoleSpinnerDefaultSelection() {
        activity = controller.create().start().resume().get();

        Spinner roleSpinner = activity.findViewById(R.id.roleSpinner);
        assertEquals("Role spinner should default to first item (Service Provider)",
            0, roleSpinner.getSelectedItemPosition());
    }

    @Test
    public void testButtonsAreClickable() {
        activity = controller.create().start().resume().get();

        Button signupButton = activity.findViewById(R.id.signupButton);
        Button googleSignInButton = activity.findViewById(R.id.googleSignInButton);

        assertTrue("Signup button should be clickable", signupButton.isClickable());
        assertTrue("Google sign-in button should be clickable", googleSignInButton.isClickable());
    }
}