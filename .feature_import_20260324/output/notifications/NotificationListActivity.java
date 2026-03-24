package com.dailyserviceapp.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.data.models.Notification;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * NotificationListActivity — shows all in-app notifications for the current user,
 * loaded from Firestore in real-time. Unread items are highlighted; tapping one
 * marks it as read. The overflow menu lets users mark everything as read at once.
 *
 * SETUP REQUIRED:
 *  - Replace the layout references below if you rename activity_notification_list.xml.
 *  - Your activity_notification_list.xml must have:
 *      <RecyclerView android:id="@+id/notificationsRecycler" … />
 *      <TextView android:id="@+id/emptyStateText" … />  (shown when list is empty)
 *  - Add a menu resource res/menu/menu_notifications.xml with item id action_mark_all_read.
 *
 * Firestore path: notifications/{docId}  (fields: userId, title, message, type, read, timestamp)
 */
public class NotificationListActivity extends BaseActivity {

    private static final String TAG = "NotificationListActivity";

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private NotificationAdapter adapter;

    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;

    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        db = FirebaseFirestore.getInstance();

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Notifications");
            }
        }

        // Views
        recyclerView  = findViewById(R.id.notificationsRecycler);
        emptyStateText = findViewById(R.id.emptyStateText);

        // RecyclerView
        adapter = new NotificationAdapter(notification -> markAsRead(notification));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        listenForNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerReg != null) listenerReg.remove();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Options menu
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notifications, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_mark_all_read) {
            markAllAsRead();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Firestore real-time listener
    // ────────────────────────────────────────────────────────────────────────

    private void listenForNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        listenerReg = db.collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", user.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<Notification> notifications = new ArrayList<>();
                    for (var doc : snapshots.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            notifications.add(n);
                        }
                    }

                    adapter.setNotifications(notifications);

                    boolean isEmpty = notifications.isEmpty();
                    recyclerView.setVisibility(isEmpty ? View.GONE  : View.VISIBLE);
                    emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    if (isEmpty) emptyStateText.setText("No notifications yet");
                });
    }

    // ────────────────────────────────────────────────────────────────────────
    // Mark read / mark all read
    // ────────────────────────────────────────────────────────────────────────

    private void markAsRead(Notification notification) {
        if (notification.isRead() || notification.getId() == null) return;

        db.collection(Constants.COLLECTION_NOTIFICATIONS)
                .document(notification.getId())
                .update("read", true);
    }

    private void markAllAsRead() {
        List<Notification> unread = adapter.getUnreadNotifications();
        if (unread.isEmpty()) {
            Snackbar.make(recyclerView, "All notifications are already read", Snackbar.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();
        for (Notification n : unread) {
            if (n.getId() != null) {
                batch.update(
                        db.collection(Constants.COLLECTION_NOTIFICATIONS).document(n.getId()),
                        "read", true);
            }
        }
        batch.commit()
                .addOnSuccessListener(v ->
                        Snackbar.make(recyclerView, "All marked as read", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Snackbar.make(recyclerView, "Failed to update", Snackbar.LENGTH_SHORT).show());
    }

    // ════════════════════════════════════════════════════════════════════════
    // RecyclerView Adapter
    // ════════════════════════════════════════════════════════════════════════

    private static class NotificationAdapter
            extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

        interface OnNotificationClick {
            void onClick(Notification notification);
        }

        private final List<Notification> items = new ArrayList<>();
        private final OnNotificationClick clickListener;
        private static final SimpleDateFormat SDF =
                new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

        NotificationAdapter(OnNotificationClick clickListener) {
            this.clickListener = clickListener;
        }

        void setNotifications(List<Notification> notifications) {
            items.clear();
            items.addAll(notifications);
            notifyDataSetChanged();
        }

        List<Notification> getUnreadNotifications() {
            List<Notification> unread = new ArrayList<>();
            for (Notification n : items) {
                if (!n.isRead()) unread.add(n);
            }
            return unread;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate row_notification.xml — see layout file note in class javadoc.
            // If you don't have that file yet, create it (example below).
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_notification, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Notification n = items.get(position);

            holder.title.setText(n.getTitle() != null ? n.getTitle() : "");
            holder.message.setText(n.getMessage() != null ? n.getMessage() : "");

            if (n.getTimestamp() != null) {
                holder.timestamp.setText(SDF.format(n.getTimestamp().toDate()));
            } else {
                holder.timestamp.setText("");
            }

            // Unread = slightly highlighted background; read = normal
            holder.unreadDot.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
            holder.itemView.setAlpha(n.isRead() ? 0.75f : 1.0f);

            // Icon based on type
            holder.icon.setImageResource(iconForType(n.getType()));

            holder.itemView.setOnClickListener(v -> clickListener.onClick(n));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private int iconForType(String type) {
            if (type == null) return R.drawable.ic_notifications_24;
            switch (type) {
                case Constants.NOTIF_BILL_GENERATED:
                case Constants.NOTIF_PAYMENT_REMINDER:
                case Constants.NOTIF_PAYMENT_RECEIVED:
                    return R.drawable.ic_payment_24;
                case Constants.NOTIF_SERVICE_DELIVERY:
                    return R.drawable.ic_calendar_24;
                default:
                    return R.drawable.ic_notifications_24;
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView  title, message, timestamp;
            ImageView icon;
            View      unreadDot;

            ViewHolder(View itemView) {
                super(itemView);
                title     = itemView.findViewById(R.id.notifTitle);
                message   = itemView.findViewById(R.id.notifMessage);
                timestamp = itemView.findViewById(R.id.notifTimestamp);
                icon      = itemView.findViewById(R.id.notifIcon);
                unreadDot = itemView.findViewById(R.id.unreadDot);
            }
        }
    }
}
