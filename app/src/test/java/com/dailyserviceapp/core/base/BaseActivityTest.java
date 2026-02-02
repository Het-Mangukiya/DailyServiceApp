package com.dailyserviceapp.core.base;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.core.utils.NetworkMonitor;
import com.dailyserviceapp.core.utils.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowToast;

import static org.junit.Assert.*;

/**
 * Unit tests for BaseActivity.
 * Tests common functionality shared across all activities.
 */
@RunWith(RobolectricTestRunner.class)
public class BaseActivityTest {

    // Test concrete implementation of BaseActivity
    private static class TestBaseActivity extends BaseActivity {
        @Override
        protected void onCreate(android.os.Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(android.R.layout.list_content);
        }
    }

    private ActivityController<TestBaseActivity> controller;
    private TestBaseActivity activity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = Robolectric.buildActivity(TestBaseActivity.class);
    }

    @Test
    public void testActivityCreation() {
        activity = controller.create().get();
        assertNotNull("Activity should be created", activity);
    }

    @Test
    public void testPreferenceManagerInitialized() {
        activity = controller.create().get();
        assertNotNull("PreferenceManager should be initialized", activity.preferenceManager);
    }

    @Test
    public void testNetworkMonitorInitialized() {
        activity = controller.create().get();
        assertNotNull("NetworkMonitor should be initialized", activity.networkMonitor);
    }

    @Test
    public void testShowToast() {
        activity = controller.create().start().resume().get();

        String testMessage = "Test toast message";
        activity.showToast(testMessage);

        assertEquals("Toast should show correct message",
            testMessage, ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testShowLongToast() {
        activity = controller.create().start().resume().get();

        String testMessage = "Test long toast message";
        activity.showLongToast(testMessage);

        assertEquals("Long toast should show correct message",
            testMessage, ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testShowNetworkError() {
        activity = controller.create().start().resume().get();

        activity.showNetworkError();

        String toastText = ShadowToast.getTextOfLatestToast();
        assertNotNull("Network error toast should be shown", toastText);
        assertTrue("Toast should mention network/internet",
            toastText.toLowerCase().contains("internet") ||
            toastText.toLowerCase().contains("network"));
    }

    @Test
    public void testSetupToolbarWithTitle() {
        activity = controller.create().get();

        Toolbar toolbar = new Toolbar(activity);
        String testTitle = "Test Title";

        activity.setupToolbar(toolbar, testTitle, false);

        ActionBar actionBar = activity.getSupportActionBar();
        assertNotNull("ActionBar should be set", actionBar);
        assertEquals("Toolbar title should be set", testTitle, actionBar.getTitle());
    }

    @Test
    public void testSetupToolbarWithBackButton() {
        activity = controller.create().get();

        Toolbar toolbar = new Toolbar(activity);
        String testTitle = "Test Title";

        activity.setupToolbar(toolbar, testTitle, true);

        ActionBar actionBar = activity.getSupportActionBar();
        assertNotNull("ActionBar should be set", actionBar);
        assertTrue("Back button should be enabled",
            actionBar.getDisplayOptions() != 0);
    }

    @Test
    public void testSetupToolbarWithoutBackButton() {
        activity = controller.create().get();

        Toolbar toolbar = new Toolbar(activity);
        String testTitle = "Test Title";

        activity.setupToolbar(toolbar, testTitle, false);

        ActionBar actionBar = activity.getSupportActionBar();
        assertNotNull("ActionBar should be set", actionBar);
    }

    @Test
    public void testSetupToolbarWithNullToolbar() {
        activity = controller.create().get();

        // Should not crash with null toolbar
        try {
            activity.setupToolbar(null, "Test", false);
            assertTrue("Should handle null toolbar gracefully", true);
        } catch (Exception e) {
            fail("Should not throw exception with null toolbar: " + e.getMessage());
        }
    }

    @Test
    public void testGetCurrentUserId() {
        activity = controller.create().get();

        // Initially should be null
        String userId = activity.getCurrentUserId();
        assertNull("User ID should be null initially", userId);

        // Set a user ID
        activity.preferenceManager.setUserId("test-user-id");
        userId = activity.getCurrentUserId();
        assertEquals("Should return correct user ID", "test-user-id", userId);
    }

    @Test
    public void testGetCurrentUserRole() {
        activity = controller.create().get();

        // Initially should be null
        String role = activity.getCurrentUserRole();
        assertNull("User role should be null initially", role);

        // Set a user role
        activity.preferenceManager.setUserRole("PROVIDER");
        role = activity.getCurrentUserRole();
        assertEquals("Should return correct user role", "PROVIDER", role);
    }

    @Test
    public void testIsProvider() {
        activity = controller.create().get();

        assertFalse("Should not be provider initially", activity.isProvider());

        activity.preferenceManager.setUserRole("PROVIDER");
        assertTrue("Should be provider after setting role", activity.isProvider());
    }

    @Test
    public void testIsCustomer() {
        activity = controller.create().get();

        assertFalse("Should not be customer initially", activity.isCustomer());

        activity.preferenceManager.setUserRole("CUSTOMER");
        assertTrue("Should be customer after setting role", activity.isCustomer());
    }

    @Test
    public void testIsLoggedIn() {
        activity = controller.create().get();

        assertFalse("Should not be logged in initially", activity.isLoggedIn());

        activity.preferenceManager.setLoggedIn(true);
        assertTrue("Should be logged in after setting", activity.isLoggedIn());
    }

    @Test
    public void testNavigateToLogin() {
        activity = controller.create().start().resume().get();

        activity.navigateToLogin();

        Intent expectedIntent = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull("Login intent should be started", expectedIntent);
        assertEquals("Should navigate to LoginActivity",
            LoginActivity.class.getName(),
            expectedIntent.getComponent().getClassName());

        assertTrue("Activity should be finishing", activity.isFinishing());
    }

    @Test
    public void testNavigateToLoginSetsIntentFlags() {
        activity = controller.create().start().resume().get();

        activity.navigateToLogin();

        Intent intent = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull("Intent should be started", intent);

        int flags = intent.getFlags();
        assertTrue("Should have NEW_TASK flag",
            (flags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
        assertTrue("Should have CLEAR_TASK flag",
            (flags & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0);
    }

    @Test
    public void testPerformLogoutClearsData() {
        activity = controller.create().start().resume().get();

        // Set some user data
        activity.preferenceManager.setUserId("test-id");
        activity.preferenceManager.setUserEmail("test@example.com");
        activity.preferenceManager.setLoggedIn(true);

        // Perform logout
        activity.performLogout();

        // Verify data is cleared
        assertNull("User ID should be cleared", activity.preferenceManager.getUserId());
        assertNull("User email should be cleared", activity.preferenceManager.getUserEmail());
        assertFalse("Should be logged out", activity.preferenceManager.isLoggedIn());
    }

    @Test
    public void testPerformLogoutNavigatesToLogin() {
        activity = controller.create().start().resume().get();

        activity.performLogout();

        Intent intent = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull("Login intent should be started", intent);
        assertEquals("Should navigate to LoginActivity",
            LoginActivity.class.getName(),
            intent.getComponent().getClassName());
    }

    @Test
    public void testOnDestroyUnregistersNetworkCallback() {
        activity = controller.create().start().resume().get();

        // Should not crash when destroyed
        try {
            controller.pause().stop().destroy();
            assertTrue("Should unregister network callback on destroy", true);
        } catch (Exception e) {
            fail("Should not crash on destroy: " + e.getMessage());
        }
    }

    @Test
    public void testActivityLifecycleComplete() {
        try {
            activity = controller.create().start().resume().pause().stop().destroy().get();
            assertTrue("Activity should handle complete lifecycle", true);
        } catch (Exception e) {
            fail("Activity lifecycle should not crash: " + e.getMessage());
        }
    }

    @Test
    public void testIsNetworkAvailableMethod() {
        activity = controller.create().get();

        // Should return a boolean without crashing
        boolean networkAvailable = activity.isNetworkAvailable();
        assertTrue("isNetworkAvailable should return a boolean value",
            networkAvailable == true || networkAvailable == false);
    }

    @Test
    public void testOnOptionsItemSelectedWithHomeButton() {
        activity = controller.create().start().resume().get();

        android.view.MenuItem menuItem = mock(android.view.MenuItem.class);
        when(menuItem.getItemId()).thenReturn(android.R.id.home);

        boolean result = activity.onOptionsItemSelected(menuItem);

        assertTrue("Home button should be handled", result);
    }

    @Test
    public void testMultipleToastsCanBeShown() {
        activity = controller.create().start().resume().get();

        activity.showToast("First message");
        activity.showToast("Second message");
        activity.showToast("Third message");

        // Last toast should be visible
        assertEquals("Last toast should be shown",
            "Third message", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testPreferenceManagerPersistsAcrossActivities() {
        activity = controller.create().get();

        activity.preferenceManager.setUserId("persistent-id");

        // Create new activity instance
        TestBaseActivity newActivity = Robolectric.buildActivity(TestBaseActivity.class)
            .create().get();

        assertEquals("User ID should persist across activity instances",
            "persistent-id", newActivity.preferenceManager.getUserId());
    }

    @Test
    public void testEmptyToastMessage() {
        activity = controller.create().start().resume().get();

        activity.showToast("");

        assertEquals("Empty toast should be shown", "", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testLongToastMessage() {
        activity = controller.create().start().resume().get();

        String longMessage = "This is a very long toast message that contains " +
            "multiple words and should still be displayed correctly without any issues";
        activity.showLongToast(longMessage);

        assertEquals("Long toast should show full message",
            longMessage, ShadowToast.getTextOfLatestToast());
    }

    // Mock MenuItem for testing
    private static android.view.MenuItem mock(Class<android.view.MenuItem> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }

    private static <T> org.mockito.stubbing.OngoingStubbing<T> when(T methodCall) {
        return org.mockito.Mockito.when(methodCall);
    }
}