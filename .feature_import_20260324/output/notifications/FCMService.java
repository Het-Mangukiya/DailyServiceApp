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
import com.dailyserviceapp.data.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Cloud Messaging service — handles incoming push notifications
 * and FCM token lifecycle.
 *
 * What this does:
 *  1. Creates a persistent notification channel (required on Android 8+).
 *  2. On message received — shows a system notification AND saves it to
 *     the user's Firestore notifications collection so it appears in the
 *     in-app NotificationListActivity.
 *  3. On token refresh — saves the new FCM token to the user's Firestore
 *     document so the server can target this device.
 */
public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    // ── Notification channel IDs ─────────────────────────────────────────────
    public static final String CHANNEL_GENERAL   = "daily_service_general";
    public static final String CHANNEL_BILLING   = "daily_service_billing";
    public static final String CHANNEL_DELIVERY  = "daily_service_delivery";

    // ── Notification IDs (increment per type to allow stacking) ─────────────
    private static int notifIdCounter = 1000;

    // ── Token field name in Firestore user document ──────────────────────────
    private static final String FIELD_FCM_TOKEN = "fcmToken";
    private static final String FIELD_FCM_TOKEN_UPDATED = "fcmTokenUpdatedAt";

    // ────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Message received
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title   = null;
        String body    = null;
        String type    = Constants.NOTIF_SERVICE_DELIVERY; // default
        String related = null;

        // Prefer data payload (sent by our backend / Cloud Functions)
        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            title   = data.get("title");
            body    = data.get("body");
            type    = data.containsKey("type")    ? data.get("type")    : type;
            related = data.containsKey("relatedId") ? data.get("relatedId") : null;
        }

        // Fall back to notification payload (sent from Firebase Console)
        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body  = remoteMessage.getNotification().getBody();
        }

        if (title == null) title = getString(R.string.app_name);
        if (body  == null) body  = "";

        // 1. Show the system (heads-up) notification
        showSystemNotification(title, body, type);

        // 2. Persist to Firestore so it appears in NotificationListActivity
        saveNotificationToFirestore(title, body, type, related);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Token refresh
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed");
        saveTokenToFirestore(token);
    }

    // ────────────────────────────────────────────────────────────────────────
    // System notification
    // ────────────────────────────────────────────────────────────────────────

    private void showSystemNotification(String title, String body, String type) {
        String channelId = channelForType(type);

        // Tap → open DashboardActivity (which will route to NotificationListActivity)
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("openNotifications", true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, notifIdCounter, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications_24)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifIdCounter++, builder.build());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Firestore helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Save received notification to Firestore so the in-app list shows it.
     */
    private void saveNotificationToFirestore(String title, String body,
                                              String type, String relatedId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Notification notification = new Notification(user.getUid(), title, body, type);
        if (relatedId != null) notification.setRelatedId(relatedId);

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_NOTIFICATIONS)
                .add(notification)
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to save notification to Firestore", e));
    }

    /**
     * Save the FCM token onto the user's Firestore document.
     * If no user is logged in yet (e.g., first install before login), we skip —
     * LoginActivity should call saveTokenToFirestore() after sign-in instead.
     */
    public static void saveTokenToFirestore(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No user logged in — token will be saved after login");
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_FCM_TOKEN, token);
        update.put(FIELD_FCM_TOKEN_UPDATED, com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .update(update)
                .addOnSuccessListener(v  -> Log.d(TAG, "FCM token saved"))
                .addOnFailureListener(e  -> Log.w(TAG, "Failed to save FCM token", e));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Notification channels (must be created before Android 8+ can show notifs)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Call this once on app start (e.g., from Application.onCreate or here).
     * Safe to call multiple times — the OS is idempotent.
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT));

        NotificationChannel billing = new NotificationChannel(
                CHANNEL_BILLING,
                "Billing & Payments",
                NotificationManager.IMPORTANCE_HIGH);
        billing.setDescription("Bill generation and payment reminders");
        manager.createNotificationChannel(billing);

        NotificationChannel delivery = new NotificationChannel(
                CHANNEL_DELIVERY,
                "Service Deliveries",
                NotificationManager.IMPORTANCE_DEFAULT);
        delivery.setDescription("Daily delivery updates");
        manager.createNotificationChannel(delivery);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void createNotificationChannels() {
        createNotificationChannels(this);
    }

    private String channelForType(String type) {
        if (type == null) return CHANNEL_GENERAL;
        switch (type) {
            case Constants.NOTIF_BILL_GENERATED:
            case Constants.NOTIF_PAYMENT_REMINDER:
            case Constants.NOTIF_PAYMENT_RECEIVED:
                return CHANNEL_BILLING;
            case Constants.NOTIF_SERVICE_DELIVERY:
                return CHANNEL_DELIVERY;
            default:
                return CHANNEL_GENERAL;
        }
    }
}
