package com.dailyserviceapp.notifications;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    public static final String CHANNEL_ADMIN = "admin_notifications";
    private static int notifIdCounter = 2000;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createNotificationChannels(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();
        String title = data.get("title");
        String body = data.get("body");
        String type = data.containsKey("type") ? data.get("type") : Constants.NOTIF_SERVICE_DELIVERY;
        String relatedId = data.get("relatedId");

        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (title == null) title = getString(R.string.app_name);
        if (body == null) body = "";

        NotificationHelper.showLocalNotification(this, title, body, type, relatedId);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        NotificationHelper.saveNotification(
            currentUser != null ? currentUser.getUid() : null,
            title,
            body,
            type,
            relatedId
        );
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        saveTokenToFirestore(token);
    }

    /**
     * Corrected: Uses set(..., SetOptions.merge()) to ensure the field is created
     * even if the document was just created or missing the field.
     */
    public static void saveTokenToFirestore(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "Cannot save token: No user logged in");
            return;
        }

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("fcmToken", token);
        tokenData.put("lastTokenUpdate", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .set(tokenData, SetOptions.merge()) // CRITICAL: This adds the field if missing
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving token", e));
    }
}
