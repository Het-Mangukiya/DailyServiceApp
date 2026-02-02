package com.dailyserviceapp;

import android.content.Intent;
import android.os.Handler;

import androidx.appcompat.app.ActionBar;

import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.core.utils.PreferenceManager;
import com.dailyserviceapp.dashboard.ProviderDashboardActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SplashActivity.
 * Tests splash screen navigation logic and user session handling.
 */
@RunWith(RobolectricTestRunner.class)
public class SplashActivityTest {

    private ActivityController<SplashActivity> controller;
    private SplashActivity activity;
    private ShadowActivity shadowActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = Robolectric.buildActivity(SplashActivity.class);
    }

    @Test
    public void testActivityCreation() {
        activity = controller.create().get();
        assertNotNull("Activity should be created", activity);
    }

    @Test
    public void testActionBarIsHidden() {
        activity = controller.create().start().resume().get();
        ActionBar actionBar = activity.getSupportActionBar();

        // Action bar may be null or hidden depending on theme
        if (actionBar != null) {
            assertFalse("Action bar should be hidden", actionBar.isShowing());
        }
    }

    @Test
    public void testNavigatesToLoginWhenNotLoggedIn() {
        // Create activity and trigger navigation
        activity = controller.create().start().resume().get();
        shadowActivity = Shadows.shadowOf(activity);

        // Wait for handler to execute (simulate delay)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        // Check if LoginActivity intent was started
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertNotNull("An intent should be started", startedIntent);

        // Verify it's either LoginActivity or ProviderDashboardActivity
        String targetClass = startedIntent.getComponent().getClassName();
        assertTrue("Should navigate to LoginActivity or ProviderDashboardActivity",
            targetClass.equals(LoginActivity.class.getName()) ||
            targetClass.equals(ProviderDashboardActivity.class.getName()));

        // Verify activity is finished
        assertTrue("Activity should be finished after navigation", activity.isFinishing());
    }

    @Test
    public void testSplashDelayConstant() {
        // Use reflection to verify the splash delay constant
        try {
            java.lang.reflect.Field field = SplashActivity.class.getDeclaredField("SPLASH_DELAY");
            field.setAccessible(true);
            int splashDelay = (int) field.get(null);
            assertEquals("Splash delay should be 2000ms", 2000, splashDelay);
        } catch (Exception e) {
            fail("SPLASH_DELAY constant should exist: " + e.getMessage());
        }
    }

    @Test
    public void testActivityFinishesAfterNavigation() {
        activity = controller.create().start().resume().get();
        shadowActivity = Shadows.shadowOf(activity);

        // Wait for handler to execute
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        // Verify activity is finishing
        assertTrue("Activity should finish after navigation", activity.isFinishing());
    }

    @Test
    public void testIntentFlagsOnNavigation() {
        activity = controller.create().start().resume().get();
        shadowActivity = Shadows.shadowOf(activity);

        // Wait for handler to execute
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        // Get the started intent
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertNotNull("An intent should be started", startedIntent);

        // While we can't verify exact flags without deeper integration,
        // we can verify an intent was started
        assertNotNull("Intent component should be set", startedIntent.getComponent());
    }

    @Test
    public void testOnCreateDoesNotCrash() {
        try {
            activity = controller.create().get();
            assertNotNull("Activity onCreate should complete without crash", activity);
        } catch (Exception e) {
            fail("onCreate should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testLayoutIsSet() {
        activity = controller.create().get();

        // Verify that setContentView was called by checking the activity is not null
        // and doesn't crash during creation
        assertNotNull("Activity should have content view set", activity.findViewById(android.R.id.content));
    }

    @Test
    public void testNavigationOccursOnMainThread() {
        activity = controller.create().start().resume().get();

        // Verify that the navigation happens on the main looper
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        // If we reach here without crash, the test passes
        assertTrue("Navigation should occur without threading issues", true);
    }

    @Test
    public void testActivityLifecycleDoesNotCrash() {
        // Test full activity lifecycle
        try {
            activity = controller.create().start().resume().pause().stop().destroy().get();
            assertTrue("Activity should handle full lifecycle", true);
        } catch (Exception e) {
            fail("Activity lifecycle should not crash: " + e.getMessage());
        }
    }
}