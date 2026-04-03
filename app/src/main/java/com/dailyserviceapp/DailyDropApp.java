package com.dailyserviceapp;

import android.app.Application;
import com.dailyserviceapp.notifications.NotificationHelper;
import com.dailyserviceapp.notifications.NotificationSyncManager;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Main application class.
 */
@HiltAndroidApp
public class DailyDropApp extends Application {

    private NotificationSyncManager notificationSyncManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createNotificationChannels(this);
        notificationSyncManager = new NotificationSyncManager(this);
        notificationSyncManager.start();
    }
}
