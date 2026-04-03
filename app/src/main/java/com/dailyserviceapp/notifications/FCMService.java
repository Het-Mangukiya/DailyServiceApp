package com.dailyserviceapp.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.dailyserviceapp.provider.JoinRequestsActivity;
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
        createNotificationChannels();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();
        String title = data.get("title");
        String body = data.get("body");
        String type = data.get("type");
        String relatedId = data.get("relatedId");

        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (title != null) {
            showNotification(title, body, type, relatedId);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        saveTokenToFirestore(token);
    }

    private void showNotification(String title, String body, String type, String relatedId) {
        Intent intent;
        if (Constants.NOTIF_JOIN_REQUEST.equals(type)) {
            intent = new Intent(this, JoinRequestsActivity.class);
        } else {
            intent = new Intent(this, DashboardActivity.class);
            if (relatedId != null) intent.putExtra(Constants.EXTRA_ORDER_ID, relatedId);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ADMIN)
                .setSmallIcon(R.drawable.ic_notifications_24)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifIdCounter++, builder.build());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel adminChannel = new NotificationChannel(
                    CHANNEL_ADMIN, "Admin Alerts", NotificationManager.IMPORTANCE_HIGH);
            adminChannel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(adminChannel);
        }
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
