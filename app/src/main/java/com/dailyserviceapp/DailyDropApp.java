package com.dailyserviceapp;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

/**
 * Main application class.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation,
 * including a base class for the application that serves as the 
 * application-level dependency container.
 */
@HiltAndroidApp
public class DailyDropApp extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Application-wide initialization (e.g., Timber, Analytics) goes here
    }
}
