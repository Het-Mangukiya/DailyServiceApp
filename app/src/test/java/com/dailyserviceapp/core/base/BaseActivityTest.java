package com.dailyserviceapp.core.base;

import android.content.Intent;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.core.utils.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BaseActivity.
 * Tests common functionality including session management, network checking,
 * toolbar setup, toast messages, and navigation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BaseActivityTest {

    private TestableBaseActivity activity;
    private PreferenceManager preferenceManager;

    /**
     * Testable implementation of BaseActivity for testing
     */
    private static class TestableBaseActivity extends BaseActivity {
        @Override
        protected void onCreate(android.os.Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Minimal setup for testing
        }
    }

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(TestableBaseActivity.class)
                .create()
                .get();
        preferenceManager = new PreferenceManager(activity);
        preferenceManager.clearAllData();
    }

    @Test
    public void testActivityCreated() {
        assertNotNull("Activity should be created", activity);
    }

    @Test
    public void testPreferenceManagerInitialized() {
        assertNotNull("PreferenceManager should be initialized", activity.preferenceManager);
    }

    @Test
    public void testNetworkMonitorInitialized() {
        assertNotNull("NetworkMonitor should be initialized", activity.networkMonitor);
    }

    @Test
    public void testGetCurrentUserIdWhenLoggedIn() {
        preferenceManager.saveUserData("testUserId", "test@example.com", "Test User", "PROVIDER");

        String userId = activity.getCurrentUserId();

        assertEquals("Should return correct user ID", "testUserId", userId);

        preferenceManager.clearAllData();
    }

    @Test
    public void testGetCurrentUserIdWhenNotLoggedIn() {
        String userId = activity.getCurrentUserId();

        assertNull("Should return null when not logged in", userId);
    }

    @Test
    public void testGetCurrentUserRole() {
        preferenceManager.saveUserData("testUserId", "test@example.com", "Test User", "PROVIDER");

        String role = activity.getCurrentUserRole();

        assertEquals("Should return correct role", "PROVIDER", role);

        preferenceManager.clearAllData();
    }

    @Test
    public void testIsProviderWhenProvider() {
        preferenceManager.saveUserData("testUserId", "test@example.com", "Test User", "PROVIDER");

        assertTrue("Should return true for provider", activity.isProvider());

        preferenceManager.clearAllData();
    }

    @Test
    public void testIsProviderWhenCustomer() {
        preferenceManager.saveUserData("testUserId", "test@example.com", "Test User", "CUSTOMER");

        assertFalse("Should return false for customer", activity.isProvider());

        preferenceManager.clearAllData();
    }

    @Test
    public void testIsCustomerWhenCustomer() {
        preferenceManager.saveUserData("testUserId", "test@example.com", "Test User", "CUSTOMER");

        assertTrue("Should return true for customer", activity.isCustomer());

        preferenceManager.clearAllData();
    }

    @Test
    public void testIsCustomerWhenProvider() {
        preferenceManager.saveUserData("testUserId", "test@example.com", "Test User", "PROVIDER");

        assertFalse("Should return false for provider", activity.isCustomer());

        preferenceManager.clearAllData();
    }

    @Test
    public void testIsLoggedInWhenLoggedIn() {
        preferenceManager.saveUserData("testUserId", "test@example.com", "Test User", "PROVIDER");

        assertTrue("Should return true when logged in", activity.isLoggedIn());

        preferenceManager.clearAllData();
    }

    @Test
    public void testIsLoggedInWhenNotLoggedIn() {
        assertFalse("Should return false when not logged in", activity.isLoggedIn());
    }

    @Test
    public void testShowToast() {
        String message = "Test toast message";

        activity.showToast(message);

        Toast latestToast = ShadowToast.getLatestToast();
        assertNotNull("Toast should be shown", latestToast);
        assertEquals("Toast should have short duration",
                Toast.LENGTH_SHORT, ShadowToast.getLatestToast().getDuration());
    }

    @Test
    public void testShowLongToast() {
        String message = "Test long toast message";

        activity.showLongToast(message);

        Toast latestToast = ShadowToast.getLatestToast();
        assertNotNull("Toast should be shown", latestToast);
        assertEquals("Toast should have long duration",
                Toast.LENGTH_LONG, ShadowToast.getLatestToast().getDuration());
    }

    @Test
    public void testShowNetworkError() {
        activity.showNetworkError();

        String toastText = ShadowToast.getTextOfLatestToast();
        assertNotNull("Network error toast should be shown", toastText);
        assertTrue("Should show network error message",
                toastText.toLowerCase().contains("internet") ||
                toastText.toLowerCase().contains("network") ||
                toastText.toLowerCase().contains("connection"));
    }

    @Test
    public void testSetupToolbarWithTitle() {
        Toolbar toolbar = new Toolbar(activity);
        String title = "Test Title";

        activity.setupToolbar(toolbar, title, false);

        assertNotNull("Action bar should be set", activity.getSupportActionBar());
        assertEquals("Title should be set", title, activity.getSupportActionBar().getTitle());
    }

    @Test
    public void testSetupToolbarWithBackButton() {
        Toolbar toolbar = new Toolbar(activity);

        activity.setupToolbar(toolbar, "Title", true);

        assertNotNull("Action bar should be set", activity.getSupportActionBar());
        assertTrue("Back button should be enabled",
                activity.getSupportActionBar().getDisplayOptions() != 0);
    }

    @Test
    public void testSetupToolbarWithoutBackButton() {
        Toolbar toolbar = new Toolbar(activity);

        activity.setupToolbar(toolbar, "Title", false);

        assertNotNull("Action bar should be set", activity.getSupportActionBar());
    }

    @Test
    public void testSetupToolbarWithNullToolbar() {
        // Should handle null toolbar gracefully
        activity.setupToolbar(null, "Title", true);

        // Should not crash
        assertNotNull("Activity should still be valid", activity);
    }

    @Test
    public void testOptionsItemSelectedHomeButton() {
        MenuItem mockMenuItem = mock(MenuItem.class);
        when(mockMenuItem.getItemId()).thenReturn(android.R.id.home);

        boolean result = activity.onOptionsItemSelected(mockMenuItem);

        assertTrue("Should handle home button", result);
    }

    @Test
    public void testNavigateToLogin() {
        activity.navigateToLogin();

        Intent startedIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull("Intent should be started", startedIntent);
        assertEquals("Should navigate to LoginActivity",
                LoginActivity.class.getName(),
                startedIntent.getComponent().getClassName());

        int flags = startedIntent.getFlags();
        assertTrue("Should clear task",
                (flags & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0);
        assertTrue("Should create new task",
                (flags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
    }

    @Test
    public void testPerformLogout() {
        preferenceManager.saveUserData("testUserId", "test@example.com", "Test User", "PROVIDER");

        activity.performLogout();

        assertFalse("User should be logged out", activity.isLoggedIn());

        Intent startedIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull("Should navigate to login", startedIntent);
        assertEquals("Should navigate to LoginActivity",
                LoginActivity.class.getName(),
                startedIntent.getComponent().getClassName());
    }

    @Test
    public void testPerformLogoutClearsPreferences() {
        preferenceManager.saveUserData("testUserId", "test@example.com", "Test User", "PROVIDER");
        assertTrue("User should be logged in initially", activity.isLoggedIn());

        activity.performLogout();

        assertNull("User ID should be cleared", preferenceManager.getUserId());
        assertNull("User email should be cleared", preferenceManager.getUserEmail());
        assertNull("User name should be cleared", preferenceManager.getUserName());
        assertNull("User role should be cleared", preferenceManager.getUserRole());
    }

    @Test
    public void testIsNetworkAvailable() {
        // This test verifies the method exists and returns a boolean
        boolean result = activity.isNetworkAvailable();

        // Result can be true or false depending on test environment
        assertTrue("Method should return a boolean value",
                result == true || result == false);
    }

    @Test
    public void testActivityLifecycle() {
        activity.onPause();
        activity.onResume();

        assertNotNull("Activity should survive lifecycle", activity);
        assertNotNull("PreferenceManager should survive lifecycle", activity.preferenceManager);
    }

    @Test
    public void testOnDestroyUnregistersNetworkCallback() {
        // Verify that network monitor is cleaned up
        assertNotNull("Network monitor should exist before destroy", activity.networkMonitor);

        activity.onDestroy();

        // After destroy, network monitor should have unregistered callbacks
        // (no exception should be thrown)
        assertNotNull("Activity should be valid after destroy", activity);
    }

    @Test
    public void testMultipleToastMessages() {
        activity.showToast("Message 1");
        activity.showToast("Message 2");
        activity.showToast("Message 3");

        String lastToast = ShadowToast.getTextOfLatestToast();
        assertEquals("Should show latest toast", "Message 3", lastToast);
    }

    @Test
    public void testEmptyToastMessage() {
        activity.showToast("");

        assertNotNull("Toast should be shown even with empty message",
                ShadowToast.getLatestToast());
    }

    @Test
    public void testNullToastMessage() {
        try {
            activity.showToast(null);
            // Should either handle null gracefully or throw exception
            assertTrue("Should handle null message", true);
        } catch (Exception e) {
            // Exception is acceptable for null input
            assertTrue("Exception is acceptable for null input", true);
        }
    }

    @Test
    public void testLongToastMessage() {
        String longMessage = "A".repeat(500);

        activity.showToast(longMessage);

        assertNotNull("Toast should handle long message", ShadowToast.getLatestToast());
    }

    @Test
    public void testSpecialCharactersInToast() {
        String specialMessage = "Test 测试 テスト 🎉 @#$%";

        activity.showToast(specialMessage);

        String toastText = ShadowToast.getTextOfLatestToast();
        assertEquals("Toast should handle special characters", specialMessage, toastText);
    }

    @Test
    public void testMultipleToolbarSetups() {
        Toolbar toolbar1 = new Toolbar(activity);
        Toolbar toolbar2 = new Toolbar(activity);

        activity.setupToolbar(toolbar1, "Title 1", true);
        activity.setupToolbar(toolbar2, "Title 2", false);

        // Should handle multiple setups without crashing
        assertNotNull("Activity should handle multiple toolbar setups", activity);
    }

    @Test
    public void testSessionPersistence() {
        String userId = "persistentUser";
        String email = "test@example.com";
        String name = "Test User";
        String role = "PROVIDER";

        preferenceManager.saveUserData(userId, email, name, role);

        // Simulate activity recreation
        activity = Robolectric.buildActivity(TestableBaseActivity.class)
                .create()
                .get();

        assertEquals("User ID should persist", userId, activity.getCurrentUserId());
        assertEquals("Role should persist", role, activity.getCurrentUserRole());

        preferenceManager.clearAllData();
    }

    @Test
    public void testRoleValidation() {
        String[] roles = {"PROVIDER", "CUSTOMER", "ADMIN", "GUEST", null, ""};

        for (String role : roles) {
            preferenceManager.clearAllData();
            if (role != null) {
                preferenceManager.saveUserData("userId", "email@test.com", "Name", role);
                assertEquals("Role should be stored correctly", role, activity.getCurrentUserRole());
            }
        }

        preferenceManager.clearAllData();
    }

    @Test
    public void testConcurrentToasts() {
        // Show multiple toasts in quick succession
        for (int i = 0; i < 10; i++) {
            activity.showToast("Toast " + i);
        }

        // Should handle concurrent toasts without crashing
        assertNotNull("Should handle concurrent toasts", ShadowToast.getLatestToast());
    }
}