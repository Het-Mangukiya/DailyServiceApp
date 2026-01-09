package com.dailyserviceapp.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {
    
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        // TODO: Handle FCM messages
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            // Show notification
        }
    }
    
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Send token to Firestore for push notifications
    }
}
