package com.dailyserviceapp.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.data.models.Notification;
import com.dailyserviceapp.provider.JoinRequestsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public final class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    private NotificationHelper() {}

    public static void createNotificationChannels(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
            FCMService.CHANNEL_ADMIN,
            "DailyDrop Notifications",
            NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Updates from DailyDrop");
        channel.enableVibration(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void showLocalNotification(Context context,
                                             String title,
                                             String body,
                                             String type,
                                             String relatedId) {
        if (context == null) {
            return;
        }

        createNotificationChannels(context);

        Intent intent;
        if (Constants.NOTIF_JOIN_REQUEST.equals(type)) {
            intent = new Intent(context, JoinRequestsActivity.class);
        } else {
            intent = new Intent(context, NotificationListActivity.class);
            if (relatedId != null && !relatedId.trim().isEmpty()) {
                intent.putExtra(Constants.EXTRA_ORDER_ID, relatedId);
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT
            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            (int) System.currentTimeMillis(),
            intent,
            flags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FCMService.CHANNEL_ADMIN)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle(title != null ? title : context.getString(R.string.app_name))
            .setContentText(body != null ? body : "")
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body != null ? body : ""))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent);

        NotificationManager manager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    public static void saveNotification(String userId,
                                        String title,
                                        String message,
                                        String type,
                                        String relatedId) {
        Map<String, Object> payload = buildNotificationPayload(userId, title, message, type, relatedId);
        if (payload == null) {
            return;
        }

        FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_NOTIFICATIONS)
            .add(payload)
            .addOnFailureListener(e -> Log.w(TAG, "Failed to save notification", e));
    }

    public static Map<String, Object> buildNotificationPayload(String userId,
                                                               String title,
                                                               String message,
                                                               String type,
                                                               String relatedId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        Notification notification = new Notification(
            userId,
            title != null ? title : "DailyDrop",
            message != null ? message : "",
            type != null ? type : Constants.NOTIF_SERVICE_DELIVERY
        );

        if (relatedId != null && !relatedId.trim().isEmpty()) {
            notification.setRelatedId(relatedId.trim());
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", notification.getUserId());
        payload.put("senderId", currentUser != null && currentUser.getUid() != null
            ? currentUser.getUid()
            : notification.getUserId());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getMessage());
        payload.put("type", notification.getType());
        payload.put("read", false);
        payload.put("relatedId", notification.getRelatedId());
        payload.put("timestamp", notification.getTimestamp());
        return payload;
    }
}
