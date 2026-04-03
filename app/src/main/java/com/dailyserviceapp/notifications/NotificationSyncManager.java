package com.dailyserviceapp.notifications;

import android.content.Context;
import android.util.Log;

import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class NotificationSyncManager {

    private static final String TAG = "NotificationSync";

    private final Context appContext;
    private final PreferenceManager preferenceManager;

    private ListenerRegistration notificationsListener;
    private FirebaseAuth.AuthStateListener authStateListener;
    private String currentUserId;
    private boolean firstSnapshot = true;

    public NotificationSyncManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.preferenceManager = new PreferenceManager(this.appContext);
    }

    public void start() {
        if (authStateListener != null) {
            return;
        }

        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null || user.getUid() == null || user.getUid().trim().isEmpty()) {
                detachListener();
                return;
            }
            attachListener(user.getUid().trim());
        };

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.addAuthStateListener(authStateListener);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getUid() != null && !user.getUid().trim().isEmpty()) {
            attachListener(user.getUid().trim());
        }
    }

    private void attachListener(String userId) {
        if (userId.equals(currentUserId) && notificationsListener != null) {
            return;
        }

        detachListener();
        currentUserId = userId;
        firstSnapshot = true;

        notificationsListener = FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.w(TAG, "Notification listener failed", error);
                    return;
                }
                if (snapshots == null) {
                    return;
                }

                long storedAlertTime = preferenceManager.getLong(alertKey(userId), 0L);
                long latestTimestamp = storedAlertTime;

                if (firstSnapshot) {
                    boolean seedOnly = storedAlertTime == 0L;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        long timestamp = extractTimestamp(doc);
                        latestTimestamp = Math.max(latestTimestamp, timestamp);

                        if (!seedOnly && timestamp > storedAlertTime && !isRead(doc)) {
                            NotificationHelper.showLocalNotification(
                                appContext,
                                doc.getString("title"),
                                doc.getString("message"),
                                doc.getString("type"),
                                doc.getString("relatedId")
                            );
                        }
                    }
                    preferenceManager.putLong(alertKey(userId), latestTimestamp);
                    firstSnapshot = false;
                    return;
                }

                for (DocumentChange change : snapshots.getDocumentChanges()) {
                    DocumentSnapshot doc = change.getDocument();
                    latestTimestamp = Math.max(latestTimestamp, extractTimestamp(doc));

                    if (change.getType() != DocumentChange.Type.ADDED) {
                        continue;
                    }
                    if (doc.getMetadata().hasPendingWrites()) {
                        continue;
                    }
                    if (isRead(doc)) {
                        continue;
                    }

                    NotificationHelper.showLocalNotification(
                        appContext,
                        doc.getString("title"),
                        doc.getString("message"),
                        doc.getString("type"),
                        doc.getString("relatedId")
                    );
                }

                preferenceManager.putLong(alertKey(userId), latestTimestamp);
            });
    }

    private void detachListener() {
        currentUserId = null;
        firstSnapshot = true;
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
    }

    private boolean isRead(DocumentSnapshot doc) {
        Boolean read = doc.getBoolean("read");
        return Boolean.TRUE.equals(read);
    }

    private long extractTimestamp(DocumentSnapshot doc) {
        com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
        return timestamp != null ? timestamp.toDate().getTime() : 0L;
    }

    private String alertKey(String userId) {
        return Constants.KEY_LAST_NOTIFICATION_ALERT_PREFIX + userId;
    }
}
