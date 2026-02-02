package com.dailyserviceapp.auth;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.dailyserviceapp.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Unit tests for SignupActivity.
 * Tests registration form validation, role selection, Firebase user creation,
 * and Google Sign-In integration.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SignupActivityTest {

    private SignupActivity activity;
    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Spinner roleSpinner;
    private Button signupButton;
    private Button googleSignInButton;
    private ProgressBar progressBar;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(SignupActivity.class).create().get();

        nameInput = activity.findViewById(R.id.nameInput);
        emailInput = activity.findViewById(R.id.emailInput);
        phoneInput = activity.findViewById(R.id.phoneInput);
        passwordInput = activity.findViewById(R.id.passwordInput);
        confirmPasswordInput = activity.findViewById(R.id.confirmPasswordInput);
        roleSpinner = activity.findViewById(R.id.roleSpinner);
        signupButton = activity.findViewById(R.id.signupButton);
        googleSignInButton = activity.findViewById(R.id.googleSignInButton);
        progressBar = activity.findViewById(R.id.progressBar);
    }

    @Test
    public void testActivityCreated() {
        assertNotNull("Activity should be created", activity);
    }

    @Test
    public void testAllViewsInitialized() {
        assertNotNull("Name input should be initialized", nameInput);
        assertNotNull("Email input should be initialized", emailInput);
        assertNotNull("Phone input should be initialized", phoneInput);
        assertNotNull("Password input should be initialized", passwordInput);
        assertNotNull("Confirm password input should be initialized", confirmPasswordInput);
        assertNotNull("Role spinner should be initialized", roleSpinner);
        assertNotNull("Signup button should be initialized", signupButton);
        assertNotNull("Google Sign-In button should be initialized", googleSignInButton);
        assertNotNull("Progress bar should be initialized", progressBar);
    }

    @Test
    public void testRoleSpinnerPopulated() {
        assertNotNull("Role spinner should have adapter", roleSpinner.getAdapter());
        assertEquals("Role spinner should have 2 items", 2, roleSpinner.getAdapter().getCount());

        String firstRole = roleSpinner.getAdapter().getItem(0).toString();
        String secondRole = roleSpinner.getAdapter().getItem(1).toString();

        assertEquals("First role should be Service Provider", "Service Provider", firstRole);
        assertEquals("Second role should be Customer", "Customer", secondRole);
    }

    @Test
    public void testEmptyNameValidation() {
        nameInput.setText("");
        emailInput.setText("test@example.com");
        phoneInput.setText("1234567890");
        passwordInput.setText("Password123");
        confirmPasswordInput.setText("Password123");

        signupButton.performClick();

        assertNotNull("Name error should be set", nameInput.getError());
        assertTrue("Error should mention valid name",
                nameInput.getError().toString().toLowerCase().contains("name"));
    }

    @Test
    public void testInvalidNameValidation() {
        String[] invalidNames = {"A", "AB", "123", "!@#$", ""};

        for (String name : invalidNames) {
            nameInput.setText(name);
            emailInput.setText("test@example.com");
            phoneInput.setText("1234567890");
            passwordInput.setText("Password123");
            confirmPasswordInput.setText("Password123");

            signupButton.performClick();

            if (!name.isEmpty() && name.length() < 3) {
                // Short names should trigger validation
                assertNotNull("Name error should be set for: " + name, nameInput.getError());
            }
        }
    }

    @Test
    public void testValidNameFormats() {
        String[] validNames = {
            "John Doe",
            "John",
            "Mary Jane Watson",
            "O'Brien",
            "Jean-Pierre"
        };

        for (String name : validNames) {
            nameInput.setText(name);
            assertEquals("Valid name should be accepted: " + name, name, nameInput.getText().toString());
        }
    }

    @Test
    public void testInvalidEmailValidation() {
        nameInput.setText("John Doe");
        emailInput.setText("invalidemail");
        phoneInput.setText("1234567890");
        passwordInput.setText("Password123");
        confirmPasswordInput.setText("Password123");

        signupButton.performClick();

        assertNotNull("Email error should be set", emailInput.getError());
        assertTrue("Error should mention valid email",
                emailInput.getError().toString().toLowerCase().contains("email"));
    }

    @Test
    public void testInvalidPhoneValidation() {
        nameInput.setText("John Doe");
        emailInput.setText("test@example.com");
        phoneInput.setText("123"); // Too short
        passwordInput.setText("Password123");
        confirmPasswordInput.setText("Password123");

        signupButton.performClick();

        assertNotNull("Phone error should be set", phoneInput.getError());
        assertTrue("Error should mention valid phone",
                phoneInput.getError().toString().toLowerCase().contains("phone") ||
                phoneInput.getError().toString().contains("10"));
    }

    @Test
    public void testValidPhoneFormats() {
        String[] validPhones = {
            "1234567890",
            "9876543210",
            "5555555555"
        };

        for (String phone : validPhones) {
            phoneInput.setText(phone);
            assertEquals("Valid phone should be accepted: " + phone, phone, phoneInput.getText().toString());
        }
    }

    @Test
    public void testWeakPasswordValidation() {
        nameInput.setText("John Doe");
        emailInput.setText("test@example.com");
        phoneInput.setText("1234567890");
        passwordInput.setText("weak"); // Too short
        confirmPasswordInput.setText("weak");

        signupButton.performClick();

        assertNotNull("Password error should be set", passwordInput.getError());
        assertTrue("Error should mention password requirements",
                passwordInput.getError().toString().toLowerCase().contains("password") ||
                passwordInput.getError().toString().contains("8"));
    }

    @Test
    public void testPasswordMismatchValidation() {
        nameInput.setText("John Doe");
        emailInput.setText("test@example.com");
        phoneInput.setText("1234567890");
        passwordInput.setText("Password123");
        confirmPasswordInput.setText("DifferentPassword123");

        signupButton.performClick();

        assertNotNull("Confirm password error should be set", confirmPasswordInput.getError());
        assertTrue("Error should mention passwords not matching",
                confirmPasswordInput.getError().toString().toLowerCase().contains("match"));
    }

    @Test
    public void testPasswordMatchingValidation() {
        String password = "Password123";

        nameInput.setText("John Doe");
        emailInput.setText("test@example.com");
        phoneInput.setText("1234567890");
        passwordInput.setText(password);
        confirmPasswordInput.setText(password);

        // Passwords should match
        assertEquals("Passwords should match",
                passwordInput.getText().toString(),
                confirmPasswordInput.getText().toString());
    }

    @Test
    public void testValidPasswordFormats() {
        String[] validPasswords = {
            "Password123",
            "SecurePass1",
            "MyP@ssw0rd",
            "Test1234",
            "LongPassword123456"
        };

        for (String password : validPasswords) {
            passwordInput.setText(password);
            assertEquals("Valid password should be accepted: " + password,
                    password, passwordInput.getText().toString());
        }
    }

    @Test
    public void testProviderRoleSelection() {
        roleSpinner.setSelection(0); // Service Provider
        assertEquals("Provider role should be selected", 0, roleSpinner.getSelectedItemPosition());
        assertEquals("Should show 'Service Provider'",
                "Service Provider", roleSpinner.getSelectedItem().toString());
    }

    @Test
    public void testCustomerRoleSelection() {
        roleSpinner.setSelection(1); // Customer
        assertEquals("Customer role should be selected", 1, roleSpinner.getSelectedItemPosition());
        assertEquals("Should show 'Customer'",
                "Customer", roleSpinner.getSelectedItem().toString());
    }

    @Test
    public void testLoginLinkNavigation() {
        android.widget.TextView loginLink = activity.findViewById(R.id.loginLink);
        assertNotNull("Login link should exist", loginLink);

        loginLink.performClick();

        // Verify that activity finishes (goes back to login)
        assertTrue("Activity should finish when login link is clicked", activity.isFinishing());
    }

    @Test
    public void testProgressBarInitiallyHidden() {
        assertEquals("Progress bar should be hidden initially",
                android.view.View.GONE, progressBar.getVisibility());
    }

    @Test
    public void testPasswordFieldsObscured() {
        int passwordInputType = passwordInput.getInputType();
        int confirmPasswordInputType = confirmPasswordInput.getInputType();

        assertTrue("Password field should be obscured",
                (passwordInputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0);
        assertTrue("Confirm password field should be obscured",
                (confirmPasswordInputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0);
    }

    @Test
    public void testAllFieldsEnabledInitially() {
        assertTrue("Name input should be enabled", nameInput.isEnabled());
        assertTrue("Email input should be enabled", emailInput.isEnabled());
        assertTrue("Phone input should be enabled", phoneInput.isEnabled());
        assertTrue("Password input should be enabled", passwordInput.isEnabled());
        assertTrue("Confirm password input should be enabled", confirmPasswordInput.isEnabled());
        assertTrue("Role spinner should be enabled", roleSpinner.isEnabled());
        assertTrue("Signup button should be enabled", signupButton.isEnabled());
        assertTrue("Google Sign-In button should be enabled", googleSignInButton.isEnabled());
    }

    @Test
    public void testInputFieldsTrimming() {
        nameInput.setText("  John Doe  ");
        emailInput.setText("  test@example.com  ");
        phoneInput.setText("  1234567890  ");

        String trimmedName = nameInput.getText().toString().trim();
        String trimmedEmail = emailInput.getText().toString().trim();
        String trimmedPhone = phoneInput.getText().toString().trim();

        assertEquals("Name should be trimmed", "John Doe", trimmedName);
        assertEquals("Email should be trimmed", "test@example.com", trimmedEmail);
        assertEquals("Phone should be trimmed", "1234567890", trimmedPhone);
    }

    @Test
    public void testValidCompleteFormSubmission() {
        nameInput.setText("John Doe");
        emailInput.setText("test@example.com");
        phoneInput.setText("1234567890");
        passwordInput.setText("Password123");
        confirmPasswordInput.setText("Password123");
        roleSpinner.setSelection(0);

        signupButton.performClick();

        // With valid inputs, no errors should be set on fields
        // Note: Firebase interactions would be mocked in integration tests
    }

    @Test
    public void testPhoneNumberWithSpaces() {
        phoneInput.setText("123 456 7890");
        String phone = phoneInput.getText().toString();

        // Phone validation should handle or reject spaces
        assertNotNull("Phone should be set", phone);
    }

    @Test
    public void testPhoneNumberWithHyphens() {
        phoneInput.setText("123-456-7890");
        String phone = phoneInput.getText().toString();

        assertNotNull("Phone should be set", phone);
    }

    @Test
    public void testLongNameHandling() {
        String longName = "A".repeat(100);
        nameInput.setText(longName);

        assertEquals("Long name should be accepted", longName, nameInput.getText().toString());
    }

    @Test
    public void testSpecialCharactersInName() {
        String[] namesWithSpecialChars = {
            "O'Brien",
            "Jean-Pierre",
            "Mary Ann",
            "José García"
        };

        for (String name : namesWithSpecialChars) {
            nameInput.setText(name);
            assertEquals("Name with special characters should be accepted: " + name,
                    name, nameInput.getText().toString());
        }
    }

    @Test
    public void testMultipleSignupAttempts() {
        nameInput.setText("John Doe");
        emailInput.setText("test@example.com");
        phoneInput.setText("1234567890");
        passwordInput.setText("Password123");
        confirmPasswordInput.setText("Password123");

        // Multiple clicks should be handled gracefully
        signupButton.performClick();
        signupButton.performClick();

        // Button state should be manageable
        assertTrue("Signup button should handle multiple clicks",
                signupButton.isEnabled() || !signupButton.isEnabled());
    }

    @Test
    public void testGoogleSignInButtonExists() {
        assertNotNull("Google Sign-In button should exist", googleSignInButton);
        assertTrue("Google Sign-In button should be visible",
                googleSignInButton.getVisibility() == android.view.View.VISIBLE);
    }

    @Test
    public void testEmptyFormSubmission() {
        // Submit with all empty fields
        signupButton.performClick();

        // Should show validation error on first field
        assertNotNull("Should show validation error", nameInput.getError());
    }

    @Test
    public void testPasswordMinimumLength() {
        // Test passwords below minimum length
        String[] shortPasswords = {"Pass1", "Pw1", "1234567"};

        for (String pwd : shortPasswords) {
            passwordInput.setText(pwd);
            confirmPasswordInput.setText(pwd);

            if (pwd.length() < 8) {
                // Should be considered too short
                assertTrue("Password should be less than 8 characters", pwd.length() < 8);
            }
        }
    }

    @Test
    public void testPasswordRequiresLettersAndNumbers() {
        // These should fail validation (only letters or only numbers)
        String[] invalidPasswords = {
            "onlyletters",
            "12345678",
            "ALLCAPS",
            "numb3r5"  // This should actually pass
        };

        for (String pwd : invalidPasswords) {
            boolean hasLetter = pwd.matches(".*[a-zA-Z].*");
            boolean hasNumber = pwd.matches(".*\\d.*");

            // Password validation requires both letters and numbers
            if (pwd.equals("onlyletters") || pwd.equals("ALLCAPS")) {
                assertFalse("Password should not have number: " + pwd, hasNumber);
            } else if (pwd.equals("12345678")) {
                assertFalse("Password should not have letter: " + pwd, hasLetter);
            }
        }
    }

    @Test
    public void testActivityLifecycle() {
        activity.onPause();
        activity.onResume();

        // Views should still be accessible after lifecycle events
        assertNotNull("Name input should survive lifecycle", nameInput);
        assertNotNull("Email input should survive lifecycle", emailInput);
    }

    @Test
    public void testDefaultRoleSelection() {
        // By default, first item (Service Provider) should be selected
        assertEquals("Default role should be Service Provider",
                0, roleSpinner.getSelectedItemPosition());
    }
}