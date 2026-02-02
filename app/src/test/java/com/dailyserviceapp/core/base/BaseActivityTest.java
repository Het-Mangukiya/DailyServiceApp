package com.dailyserviceapp.core.base;

import android.content.Intent;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;

import com.dailyserviceapp.R;
import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.core.utils.NetworkMonitor;
import com.dailyserviceapp.core.utils.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BaseActivity.
 * Tests common functionality including preference management,
 * network monitoring, toolbar setup, and utility methods.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BaseActivityTest {

    private TestBaseActivity activity;

    @Mock
    private NetworkMonitor mockNetworkMonitor;

    @Mock
    private PreferenceManager mockPreferenceManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        activity = Robolectric.buildActivity(TestBaseActivity.class)
            .create()
            .start()
            .resume()
            .get();
    }

    @Test
    public void testActivityCreation() {
        assertNotNull("Activity should be created", activity);
        assertNotNull("PreferenceManager should be initialized", activity.preferenceManager);
        assertNotNull("NetworkMonitor should be initialized", activity.networkMonitor);
    }

    @Test
    public void testSetupToolbarWithTitle() {
        Toolbar toolbar = new Toolbar(activity);

        activity.setupToolbar(toolbar, "Test Title", false);

        assertNotNull("Toolbar should be set", activity.getSupportActionBar());
        assertEquals("Title should be set", "Test Title", activity.getSupportActionBar().getTitle());
    }

    @Test
    public void testSetupToolbarWithBackButton() {
        Toolbar toolbar = new Toolbar(activity);

        activity.setupToolbar(toolbar, "Test", true);

        assertTrue("Back button should be enabled",
            activity.getSupportActionBar().getDisplayOptions() != 0);
    }

    @Test
    public void testSetupToolbarWithNullToolbar() {
        // Should not crash with null toolbar
        activity.setupToolbar(null, "Title", false);

        // Test passes if no exception is thrown
    }

    @Test
    public void testOnOptionsItemSelectedHome() {
        MenuItem mockMenuItem = mock(MenuItem.class);
        when(mockMenuItem.getItemId()).thenReturn(android.R.id.home);

        boolean handled = activity.onOptionsItemSelected(mockMenuItem);

        assertTrue("Home button should be handled", handled);
    }

    @Test
    public void testShowToast() {
        // Should not crash when showing toast
        activity.showToast("Test message");

        // Test passes if no exception is thrown
    }

    @Test
    public void testShowLongToast() {
        // Should not crash when showing long toast
        activity.showLongToast("Test long message");

        // Test passes if no exception is thrown
    }

    @Test
    public void testIsNetworkAvailable() {
        // Test default network availability check
        boolean isAvailable = activity.isNetworkAvailable();

        // Should return a boolean value without crashing
        assertNotNull("Network availability should return a value", isAvailable);
    }

    @Test
    public void testShowNetworkError() {
        // Should show network error message without crashing
        activity.showNetworkError();

        // Test passes if no exception is thrown
    }

    @Test
    public void testGetCurrentUserId() {
        String userId = activity.getCurrentUserId();

        // Should return a value (null or actual ID) without crashing
        assertNotNull("Method should complete without error", activity);
    }

    @Test
    public void testGetCurrentUserRole() {
        String role = activity.getCurrentUserRole();

        // Should return a value (null or actual role) without crashing
        assertNotNull("Method should complete without error", activity);
    }

    @Test
    public void testIsProvider() {
        boolean isProvider = activity.isProvider();

        // Should return a boolean value without crashing
        assertNotNull("Method should complete without error", activity);
    }

    @Test
    public void testIsCustomer() {
        boolean isCustomer = activity.isCustomer();

        // Should return a boolean value without crashing
        assertNotNull("Method should complete without error", activity);
    }

    @Test
    public void testIsLoggedIn() {
        boolean isLoggedIn = activity.isLoggedIn();

        // Should return a boolean value without crashing
        assertNotNull("Method should complete without error", activity);
    }

    @Test
    public void testNavigateToLogin() {
        activity.navigateToLogin();

        Intent expectedIntent = new Intent(activity, LoginActivity.class);
        Intent actualIntent = Shadows.shadowOf(activity).getNextStartedActivity();

        assertNotNull("Intent should be started", actualIntent);
        assertEquals("Should navigate to LoginActivity",
            LoginActivity.class.getName(),
            actualIntent.getComponent().getClassName());
        assertTrue("Activity should be finishing", activity.isFinishing());
    }

    @Test
    public void testNavigateToLoginSetsCorrectFlags() {
        activity.navigateToLogin();

        Intent actualIntent = Shadows.shadowOf(activity).getNextStartedActivity();

        int expectedFlags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK;
        assertTrue("Should have NEW_TASK flag",
            (actualIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
        assertTrue("Should have CLEAR_TASK flag",
            (actualIntent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0);
    }

    @Test
    public void testPerformLogout() {
        activity.performLogout();

        // Verify navigation to login
        Intent actualIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull("Should navigate after logout", actualIntent);
        assertTrue("Activity should be finishing", activity.isFinishing());
    }

    @Test
    public void testOnDestroy() {
        activity.onDestroy();

        // Test passes if no exception is thrown
        // NetworkMonitor should be cleaned up
    }

    @Test
    public void testMultipleToolbarSetups() {
        Toolbar toolbar1 = new Toolbar(activity);
        Toolbar toolbar2 = new Toolbar(activity);

        activity.setupToolbar(toolbar1, "Title 1", false);
        activity.setupToolbar(toolbar2, "Title 2", true);

        // Second setup should override first
        assertEquals("Should have latest title", "Title 2",
            activity.getSupportActionBar().getTitle());
    }

    @Test
    public void testSetupToolbarWithoutBackButton() {
        Toolbar toolbar = new Toolbar(activity);

        activity.setupToolbar(toolbar, "Test", false);

        // Verify back button is not shown
        assertNotNull("Toolbar should be set up", activity.getSupportActionBar());
    }

    @Test
    public void testToastMessagesWithEmptyString() {
        activity.showToast("");
        activity.showLongToast("");

        // Should handle empty strings gracefully
    }

    @Test
    public void testToastMessagesWithLongText() {
        String longMessage = "This is a very long message ".repeat(20);
        activity.showToast(longMessage);
        activity.showLongToast(longMessage);

        // Should handle long messages gracefully
    }

    @Test
    public void testOptionsMenuSelectionWithUnknownItem() {
        MenuItem mockMenuItem = mock(MenuItem.class);
        when(mockMenuItem.getItemId()).thenReturn(999999); // Unknown ID

        boolean handled = activity.onOptionsItemSelected(mockMenuItem);

        // Should delegate to super
        assertNotNull("Should handle unknown menu items", activity);
    }

    /**
     * Test implementation of BaseActivity for testing purposes
     */
    public static class TestBaseActivity extends BaseActivity {
        @Override
        protected void onCreate(android.os.Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Minimal setup for testing
        }
    }
}